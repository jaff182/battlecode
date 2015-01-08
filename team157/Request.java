package team157;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.RobotType;

public class Request {
    /**
     * Class for making requests. 
     * 
     * 1) use the get*Request series of functions to create a request
     * 2) use the broadcastTo* series of functions to broadcast a request
     * 
     * Variable namings available to callees:
     * request
     * The value to be broadcasted on the request channel
     * Contains jobID, round number, task type, and parameters
     * request is a long, and requires two contiguous channels to transmitted.
     * put the high bits on the first, low on the second.
     * 
     * bit allocation, from left to right:
     * 14 bits -> jobID
     * 11 bits -> round number
     * 7 bits -> task type
     * 
     * parameters:
     * 3 bits -> task priority
     * task info, task specific. See job documentation later for info.
     * 
     * 
     * requestInfo
     * Reporting back on request made
     * Contains task score, countingID
     * 3 bits -> job status
     * 11 bits -> round number
     * 12 bits -> countingID (uniquely identifies robot on task)
     * 6 bits -> task score suitability, reverts to task info later
     */
    
    public class JobStatus {
        /**
         * REQUESTING - task has not been adopted<br>
         * COMPLETE - task has been completed<br>
         * IN_PROGRESS - task is being done<br>
         * DELAYED - task has been abandoned by unit for a while<br>
         * FAILED - task has been marked as impossible by the unit, or unit
         * fails to report for three turns<br>
         * CANCELLED - task has been cancelled
         */
        private final static int REQUESTING = 0; // Must be 0 to match zeroed metadata
        private final static int COMPLETE = 1;
        private final static int IN_PROGRESS = 2;
        private final static int DELAYED = 3;
        private final static int FAILED = 4;
        private final static int CANCELLED = 5;
    }
    
    public class JobType { 
        // 7 bits for this, use a byte to avoid blowing limit
        private final static int BUILD_BUILDING_MASK = 0b00100000; 
        // Indicates this is a build command for buildings
        // No other commands must set the 2nd-leftmost bit to 1
        //The rightmost 5 bits determine what to build, according to the robotType ordinal. Add to use
        
        private final static int BUILD_MOVING_ROBOT_MASK = 0b01000000; 
        // Indicates this is a build command for buildings
        // No other commands must set the leftmost bit to 1
        //The rightmost 5 bits determine what to build, according to the robotType ordinal. Add to use
        
        private final static int MOVE = 1;
        
        private final static int SUPPLY = 2;

    }
    
    /**
     * Base address into messaging array of a randomized data structure storing request metadata
     */
    private static final int BASE_REQUEST_METADATA_CHANNEL = 30000;
    
    /**
     * Size of randomized data structure in channels.
     * 
     * Ideally, we would like load factor to be low, to make channel allocations faster.
     */
    private static final int REQUEST_METADATA_SIZE = 4000; // max value of 16384
    
    /**
     * Gets a job id. This job id is also your relative index into the
     * "request metadata" structure in the messaging array.
     * 
     * The returned jobID will index to a zeroed metadata location.
     * 
     * So, if REQUEST_METADATA_SIZE is 4000, you can get a value from 0-4000.
     * Your channel index is then BASE_REQUEST_METADATA_CHANNEL + jobID
     * 
     * @return -1 if error, positive integer otherwise
     */
    private static int getJobID() {
        try {
            for (int tries = 15; tries != 0; --tries) {

                // Randomly generate a jobID
                int proposedJobID = RobotPlayer.rand
                        .nextInt(REQUEST_METADATA_SIZE);

                // Check if jobID is in use, by retrieving metadata from
                // REQUEST_METADATA and testing
                int proposedJobMetadata = RobotPlayer.rc
                        .readBroadcast(BASE_REQUEST_METADATA_CHANNEL
                                + proposedJobID);

                if (proposedJobMetadata == 0)
                    return proposedJobID;
                else if (Clock.getRoundNum()
                        - getRequestInfoLastRoundNumber(proposedJobMetadata) >= 5) {
                    RobotPlayer.rc.broadcast(proposedJobID, 0);
                    return proposedJobID;
                }
            }
        } catch (GameActionException e) {
            e.printStackTrace();
            return -1;
        }
        return -1;
    }
    
    /**
     * Retrieves a jobID from an existing request.
     * 
     * @return
     */
    public static int getJobID(int request) {
        return (request & 0xFC00) >>> 18;
    }
    
    
    /**
     * Build a request for a robot of specific type, with fastest delivery time to (x,y)
     * 
     * Do not use for constructing buildings.
     * 
     * @param robotType ordinal as used in the official RobotType
     * @param x coordinate relative to the center of map
     * @param y coordinate relative to the center of map
     * @return request to be broadcast
     */
    public static long getBuildRobotRequest(int robotType, int x, int y) {
        assert x <= 61 && x >= -61 && y <= 61 && y >= -61;
        return (x << (64 - 7 - 3)) | (y << (64 - 14 - 3)) | (getJobID() << (32 - 14))
                | (Clock.getRoundNum() << (32 - 11 - 14))
                | JobType.BUILD_MOVING_ROBOT_MASK & robotType;
    }
    
    /**
     * Build a request for a building to be built in the specified area centered
     * at (x,y)
     * 
     * @param buildingType
     *            the building we want
     * @param x
     * @param y
     * @param fudgeAreaCircleRadius
     *            the size (in radius) that we are willing to have the building
     *            built inexactly at
     * @return request to be broadcast
     */
    public static int getConstructBuildingRequest(int buildingType, int x, int y, int fudgeAreaCircleRadius) {
        assert x <= 61 && x >= -61 && y <= 61 && y >= -61;
        return (x << (64 - 7 - 3)) | (y << (64 - 14 - 3))
                | (fudgeAreaCircleRadius << (64 - 21 - 3))
                | (getJobID() << (32 - 14))
                | (Clock.getRoundNum() << (32 - 11 - 14))
                | JobType.BUILD_MOVING_ROBOT_MASK | buildingType;
    }

    /**
     * Asks a robot on the specified channel to move to this point
     * 
     * @param x
     * @param y
     * @return
     */
    public long getMoveRequest(int x, int y) {
        assert x <= 61 && x >= -61 && y <= 61 && y >= -61;
        return (x << (64 - 7 - 3)) | (y << (64 - 14 - 3)) | (getJobID() << (32 - 14))
                | (Clock.getRoundNum() << (32 - 11 - 14))
                | JobType.MOVE;
    }
    
    /**
     * Asks for supplies to be sent to this spot.
     * 
     * Does not guarantee supplies will be delivered, just that a robot with
     * this amount of supply will move to given spot.
     * 
     * @param x
     * @param y
     * @param amount up to 262144
     * @return
     */
    public long getSupplyRequest(int x, int y, int amount) {
        assert amount < 262144;
        return (x << (64 - 7 - 3)) | (y << (64 - 14 - 3)) | (amount << 32)
                | (getJobID() << (32 - 14))
                | (Clock.getRoundNum() << (32 - 11 - 14)) | JobType.SUPPLY;
    }
    
    /**
     * Change the priority of a request
     * 
     * @param request the request we wish to (de)prioritize
     * @param priority default priority is 2, only values 0 to 4 are supported
     * @return the new request
     */
    public static int modifyRequestPriority(int request, int priority) {
        return 0;
    }
    
    /**
     * Broadcast to all units of a given type
     * 
     * @param unitType
     */
    public static void broadcastToUnitType(int request, int unitType) {
        
    }
    
    /**
     * Broadcast to all units in a square centered about x, y
     * 
     * Will broadcast to units outside slightly this area too
     * 
     * Under the hood, uses a grid approximation of board, and then broadcasts
     * to squares touching the area defined in function parameters.
     * 
     * @param request
     * @param x
     * @param y
     * @param edgeLength width of square in which we will broadcast
     */
    public static void broadcastToLocation(int request, int x, int y, int edgeLength) {
        
    }

    /**
     * Gets a request for a job (if available), for units at location and type
     * 
     * Zero if no requested jobs are found.
     * 
     * @param x
     * @param y
     * @param unitType
     * @return jobID
     */
    public static int getRequest(int x, int y, int unitType) {
        return 0;
    }
    
    /**
     * Gets the scoring metadata for a request.
     * 
     * Unpack with getRequestInfo* set of functions.
     *
     * @return requestInfo an integer containing all the scoring information, from the metadata 
     * @throws GameActionException 
     */
    public static int getRequestInfo(int jobID) throws GameActionException {
        return RobotPlayer.rc.readBroadcast(BASE_REQUEST_METADATA_CHANNEL + jobID);
    }
    
    /**
     * Gets the current score for a request.
     * 
     * 0 if no attempt has been made, 64 if request is filled/unavailable
     * 
     * Note that 63 is the maximum score.
     * 
     * @param requestInfo
     * @return
     */
    public static int getRequestInfoScore(int requestInfo) {
        final int requestStatus = requestInfo >>> (32-3);
        if (requestStatus != JobStatus.REQUESTING)
            return 64;
        return requestInfo & 0x3F;
    }
    
    /**
     * Gets the current priority for a request.
     * 
     * @param requestInfo
     * @return
     */
    public static int getRequestInfoPriority(int requestInfo) {
        return 0;
    }
    
    /**
     * Gets the round number this job was last updated.
     * 
     * @param requestInfo
     * @return
     */
    public static int getRequestInfoLastRoundNumber(int requestInfo) {
        return (requestInfo >>> (32-3-11)) & 0x7FF;
    }
    
    /**
     * Attempts to claim the task. If successful, the task will be allocated to
     * robot next round.
     * 
     * Only run attemptToClaimJob if robot has higher score, and it believes it
     * can complete task
     * 
     * @param request the request we were given earlier (not requestInfo!)
     * @param countingID
     * @param currentScore must be below 63
     * @return
     * @throws GameActionException 
     */
    public static void attemptToClaimJob(int request, int countingID, int currentScore) throws GameActionException {
        final int jobID = getJobID(request);
        assert currentScore < 63 && currentScore >= 0;
        final int taskInfo = (JobStatus.REQUESTING << (32 - 3))
                | (Clock.getRoundNum() << (32 - 3 - 11))
                | (countingID << (32 - 3 - 11 - 12)) | (currentScore);
        System.out.println("Taskinfo" + taskInfo);
        RobotPlayer.rc.broadcast(BASE_REQUEST_METADATA_CHANNEL + jobID,
                taskInfo);
    }
    
    /**
     * Get the status of the task.
     * 
     * Use only after task has been assigned, and up to five rounds after task
     * fail/complete.
     * 
     * @param jobID
     * @return a value from Request.JobStatus.* indicating task status
     * @throws GameActionException 
     */
    public static int checkJobStatus(int jobID) throws GameActionException {
        return RobotPlayer.rc.readBroadcast(BASE_REQUEST_METADATA_CHANNEL
                + jobID) >>> (32 - 3);
    }
    
    /**
     * For workers after assignment: Update job status
     * 
     * Call every round to report back, and check if task has been cancelled.
     * 
     * @param jobID
     * @param inboxID
     * @param jobStatus
     *            the status we want to broadcast (JobStatus.COMPLETE, DELAYED,
     *            IN_PROGRESS, FAILED are the only valid values)
     * @return false if job has been cancelled (or has ended/corrupt), true
     *         otherwise
     * @throws GameActionException
     */
    public static boolean updateJobStatus(int jobID, int inboxID, int jobStatus) throws GameActionException {
        final int requestInfo = RobotPlayer.rc.readBroadcast(BASE_REQUEST_METADATA_CHANNEL
                + jobID);
        final int lastJobStatus = requestInfo >>> (32-3);
        if (jobStatus != JobStatus.DELAYED && lastJobStatus != JobStatus.IN_PROGRESS)
            return false;
        final int newRequestInfo = (jobStatus << (32-3)) | (Clock.getRoundNum() << (32-11)) & (inboxID << (32-3-11-12));
        RobotPlayer.rc.broadcast(BASE_REQUEST_METADATA_CHANNEL + jobID, newRequestInfo);
        return true;
    }
}