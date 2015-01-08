package team157;

public class Request {
    /**
     * Class for making requests. 
     * 
     * 1) use the get*Request series of functions to create a request
     * 2) use the broadcastTo* series of functions to broadcast a request
     * 
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
        private final int REQUESTING = 1, COMPLETE = 2, IN_PROGRESS = 3, DELAYED = 4, FAILED = 5,
                CANCELLED = 6;
    }
    
    /**
     * Base address into messaging array of a randomized data structure storing request metadata
     */
    private final int BASE_REQUEST_METADATA_CHANNEL = 30000;
    
    /**
     * Size of randomized data structure in channels.
     * 
     * Ideally, we would like load factor to be low, to make channel allocations faster.
     */
    private final int REQUEST_METADATA_SIZE = 4000;
    
    /**
     * Gets a job id. This job id is also your relative index into the
     * "request metadata" structure in the messaging array.
     * 
     * So, if REQUEST_METADATA_SIZE is 4000, you can get a value from 0-4000.
     * Your channel index is then BASE_REQUEST_METADATA_CHANNEL + jobID
     * 
     * @return
     */
    private static int getJobID() {
        return 0;
    }
    
    /**
     * Retrieves a jobID from an existing request.
     * 
     * @return
     */
    private static int getJobID(int request) {
        return 0;
    }
    
    
    /**
     * Build a request for a robot of specific type, with fastest delivery time to (x,y)
     * 
     * Do not use for constructing buildings.
     * 
     * @param robotType
     * @param x
     * @param y
     * @return request to be broadcast
     */
    public static int getBuildRobotRequest(int robotType, int x, int y) {
        return 0;
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
        return 0;
    }

    /**
     * Asks a robot on the specified channel to move to this point
     * 
     * @param x
     * @param y
     * @return
     */
    public int getExploreRequest(int x, int y) {
        return 0;
    }
    
    /**
     * Change the priority of a request
     * 
     * @param request the request we wish to (de)prioritize
     * @param priority default priority is 2, only values 0 to 4 are supported
     * 0 is lowest priority, 4 highest 
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
     * Gets the metadata for a request.
     * 
     * Unpack with getRequest* set of functions.
     *
     * @return an integer containing all the information you need for a request
     */
    public static int getRequest(int jobID) {
        return 0;
    }
    
    /**
     * Gets the current score for a request.
     * 
     * @param requestInfo
     * @return
     */
    public static int getRequestScore(int requestInfo) {
        return 0;
    }
    
    /**
     * Gets the current priority for a request.
     * 
     * @param requestInfo
     * @return
     */
    public static int getRequestPriority(int requestInfo) {
        return 0;
    }
    
    /**
     * Attempts to claim the task. If successful, the task will be allocated to
     * robot next round.
     * 
     * Only run attemptToClaimJob if robot has higher score, and it believes it
     * can complete task
     * 
     * @param jobID
     * @param taskInfo
     * @param currentScore
     * @return
     */
    public static boolean attemptToClaimJob(int jobID, int taskInfo, int currentScore) {
        return false;
    }
    
    /**
     * Get the status of the task.
     * 
     * Use only after task has been assigned, and up to five rounds after task
     * fail/complete.
     * 
     * @param jobID
     * @return
     */
    public static int checkJobStatus(int jobID) {
        return 0;
    }
    
    /**
     * For workers: Update job status
     * 
     * Call every round to report back, and check if task has been cancelled.
     * 
     * @param jobID
     * @return true if job has been cancelled, false otherwise
     */
    public static boolean updateJobStatus(int jobID) {
        return false;
    }
}
