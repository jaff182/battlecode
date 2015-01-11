package team157;

import team157.RobotPlayer.ChannelName;
import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.RobotType;

public class Request {
    /**
     * Class for making requests.
     * 
     * Requesters:<br>
     * They create a request, broadcast it, and (optionally) keep tabs on its
     * completion.<br>
     * 
     * 1) use the get*Request series of functions to create a request<br>
     * 2) use the broadcastTo* series of functions to broadcast the created
     * request<br>
     * 3) From the next round onwards, use the checkJobStatus function to get
     * the status of a request
     * 
     * Workers:<br>
     * They check for requests, score their own suitability for the task, and
     * then try to claim the job. If they succeed, they find out the next round.
     * They then continually send updates on the job.<br>
     * 
     * 1) Use the checkForRequest function to check for any unfilled requests<br>
     * 2) Unpack the request data returned to get parameters and request type. <br>
     * Functions to unpack request are unwritten.<br>
     * Robots that are suitable for the task must score its suitability for the
     * task from 0-63, with 63 the most suitable. (For example, if it is a
     * request to move, then how far and fast a robot is from that point)<br>
     * 3) Use getRequestInfo and then getRequestInfoScore to see if any other
     * robots score higher. requestInfo scores the current top scorer. <br>
     * 4) Use the attemptToClaimJob function if this robot is the best scorer.<br>
     * 5) Check the next round to see whether this robot has won the job. 6) If
     * so, start doing the job, and use updateJobStatus to advise requester on
     * job status<br>
     * 
     * Variable namings available to callees:<br>
     * request<br>
     * The value to be broadcasted on the request channel<br>
     * Contains jobID, round number, task type, and parameters<br>
     * request is a long, and requires two contiguous channels to transmitted.<br>
     * put the high bits on the first, low on the second.<br>
     * 
     * bit allocation, from left to right:<br>
     * 3 bits -> task priority<br>
     * 29 bits -> task info, task specific. See job documentation later for info.<br>
     * 
     * 14 bits -> jobID<br>
     * 11 bits -> round number<br>
     * 7 bits -> task type<br>
     * 
     * 
     * requestInfo<br>
     * Reporting back on request made<br>
     * Contains task score, countingID<br>
     * 3 bits -> job status<br>
     * 11 bits -> round number<br>
     * 12 bits -> countingID (uniquely identifies robot on task)<br>
     * 6 bits -> task score suitability, reverts to task info later<br>
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
        // 7 bits for this
        
        // xx                             xxxxx
        // <build flag (2 bits)>          <general instruction number>
        
        // leftmost 3 bits reserved
        private final static int BUILD_BUILDING_MASK = 0b010_0000; 
        // Indicates this is a build command for buildings
        // The sequence of 01 for the leftmost two bits characterize this 
        //The rightmost 5 bits determine what to build, according to the robotType ordinal. Add to use
        
        private final static int BUILD_MOVING_ROBOT_MASK = 0b100_0000; 
        // Indicates this is a build command for moving robots
        // The sequence of 10 for the leftmost two bits characterize this 
        //The rightmost 5 bits determine what to build, according to the robotType ordinal. Add to use

        
        // You have 32 instructions using the rightmost 5 bits (with 00 for leftmost 2)
        // Reserve 0
        private final static int MOVE = 1;
        
        private final static int SUPPLY = 2;
        
    }
    
    /**
     * Base address into messaging array of a randomized data structure storing request metadata
     */
    private static final int BASE_REQUEST_METADATA_CHANNEL = RobotPlayer.getChannel(RobotPlayer.ChannelName.REQUESTS_METADATA_BASE);
    
    /**
     * Size of randomized data structure in channels.
     * 
     * Ideally, we would like load factor to be low, to make channel allocations faster.
     */
    private static final int REQUEST_METADATA_SIZE = 7000; // max value of 16384
    
    // Variables for workers =====================================
    enum WorkerState {
        IDLE, REQUESTING_JOB, ON_JOB
    }
    
    /** 
     * Reflects the state of all robots.
     * 
     * The Request library will update this state, do not write to it directly.
     */
    public static WorkerState workerState = WorkerState.IDLE;
    
    // Variables for robots in process of claim ======================
    // Robots should only request one job at a time
    public static int claimJobID;
    public static int claimCreatedRoundNum;
    public static int claimJobType;
        
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
    public static int getJobID(long request) {
        return (int) (request >>> (32-14)) & 0b11_1111_1111_1111;
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
    public static long getConstructBuildingRequest(int buildingType, int x, int y, int fudgeAreaCircleRadius) {
        assert x <= 61 && x >= -61 && y <= 61 && y >= -61;
        return ((long)x << (64 - 7 - 3)) | ((long)y << (64 - 14 - 3))
                | ((long)fudgeAreaCircleRadius << (64 - 21 - 3))
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
    public static long getMoveRequest(int x, int y) {
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
    public static long getSupplyRequest(int x, int y, int amount) {
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
     * @throws GameActionException 
     */
    public static void broadcastToUnitType(long request, int unitType) throws GameActionException {
        RobotPlayer.rc.broadcast(
                RobotPlayer.getChannel(ChannelName.REQUEST_MAILBOX_BASE)
                        + unitType, (int) (request >>> 32));
        RobotPlayer.rc.broadcast(
                RobotPlayer.getChannel(ChannelName.REQUEST_MAILBOX_BASE)
                        + unitType + 20, (int) request);

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
     * Gets a request for a job (if available), for units at location and type.
     * 
     * Updates claimCreatedRoundNum, claimJobID, claimJobType. Only works if worker is idle.
     * 
     * Zero if no requested jobs are found.
     * 
     * @param x
     * @param y
     * @param unitType
     * @return jobID
     * @throws GameActionException 
     */
    public static long checkForRequest(int x, int y, int unitType)
            throws GameActionException {
        if (true || workerState == WorkerState.IDLE) {
            // Only check unit mailbox for now
            int lowBits = RobotPlayer.rc.readBroadcast(RobotPlayer
                    .getChannel(ChannelName.REQUEST_MAILBOX_BASE)
                    + unitType
                    + 20);
            claimCreatedRoundNum = (lowBits >>> 7) & 0b111_1111_1111;
            System.out.println(claimCreatedRoundNum);
            if (Clock.getRoundNum() - claimCreatedRoundNum <= 1) { // Request is
                                                                   // valid
                long fullRequest = (long) (RobotPlayer.rc.readBroadcast(RobotPlayer
                        .getChannel(ChannelName.REQUEST_MAILBOX_BASE)
                        + unitType)) << 32 | lowBits;
                claimJobID = (int) (fullRequest >>> (32-14)) & 0b11_1111_1111_1111;
                System.out.println(claimJobID);
                claimJobType = (int) fullRequest & 0b111_1111;
                System.out.println(claimJobType);
                return fullRequest;
            }
            return 0;
            // Interesting case occurs when round number is 0 or 1. Then, we
            // will always transmit requests.
            // This would require corruption to occur in 2 rounds though.
        }
        return 0;
    }

    public static int getXCoordinate(long request) {
        return (int) ((request >>> (64-3-7)) & 0b111_1111);
    }
    
    public static int getYCoordinate(long request) {
        return (int) ((request >>> (64-3-14)) & 0b111_1111);
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
     * Modifies claimJobID and workerState to the jobID and REQUESTING_JOB respectively.
     * 
     * @param currentScore must be below 63
     * @return
     * @throws GameActionException 
     */
    public static void attemptToClaimJob(int currentScore) throws GameActionException {
        assert currentScore < 63 && currentScore >= 0;
        final int taskInfo = (JobStatus.REQUESTING << (32 - 3))
                | (Clock.getRoundNum() << (32 - 3 - 11))
                | (RobotPlayer.countingID << (32 - 3 - 11 - 12)) | (currentScore);
        System.out.println("Taskinfo" + taskInfo);
        RobotPlayer.rc.broadcast(BASE_REQUEST_METADATA_CHANNEL + claimJobID,
                taskInfo);
        Request.workerState = WorkerState.REQUESTING_JOB;
    }
    
    /**
     * Checks whether you've won the job. Use in next turn after claiming job.
     * 
     * @param jobID
     * @return true if you've been assigned the job. False otherwise
     * @throws GameActionException
     */
    public static boolean isJobClaimSuccessful() throws GameActionException {
        int requestInfo = getRequestInfo(claimJobID);
        final int requestStatus = requestInfo >>> (32-3);
        final int winnerCountingID = (requestInfo >>> 6) & 0xFFF;
        if (winnerCountingID == RobotPlayer.countingID
                && requestStatus == JobStatus.REQUESTING) {
            final int newRequestInfo = (JobStatus.IN_PROGRESS << (32 - 3))
                    | (Clock.getRoundNum() << (32 - 11))
                    | (RobotPlayer.countingID << (32-3-11-12));
            RobotPlayer.rc.broadcast(BASE_REQUEST_METADATA_CHANNEL + claimJobID, newRequestInfo);
            Request.workerState = WorkerState.ON_JOB;
            return true;
        }
        return false;
    }
    
    /**
     * Requesters: Get the status of the task issued.
     * 
     * Use only after task has been assigned (on the third turn after
     * requesting), and up to five rounds after task fail/complete.
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
     * @param jobStatus
     *            the status we want to broadcast (JobStatus.COMPLETE, DELAYED,
     *            IN_PROGRESS, FAILED are the only valid values)
     * @return false if job has been cancelled (or has ended/corrupt), true
     *         otherwise
     * @throws GameActionException
     */
    public static boolean updateJobStatus(int jobStatus) throws GameActionException {
        final int requestInfo = RobotPlayer.rc.readBroadcast(BASE_REQUEST_METADATA_CHANNEL
                + claimJobID);
        final int lastJobStatus = requestInfo >>> (32-3);
        if (lastJobStatus != JobStatus.DELAYED && lastJobStatus != JobStatus.IN_PROGRESS) {
            Request.workerState = WorkerState.IDLE;
            return false;
        }
        final int newRequestInfo = (jobStatus << (32-3)) | (Clock.getRoundNum() << (32-11)) & (RobotPlayer.countingID << (32-3-11-12));
        RobotPlayer.rc.broadcast(BASE_REQUEST_METADATA_CHANNEL + claimJobID, newRequestInfo);
        return true;
    }
}