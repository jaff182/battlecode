package team157.Utility;

import team157.RobotPlayer;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class SoldierGroup {
    // Exposed variables for external use, after calling sync()
    public int groupCenterX;
    public int groupCenterY;
    
    // Channel numbers for internal use ============================
    
    /** The very first channel in start of structure in radio array */
    private final static int BASE_CHANNEL = 29999;
    private final static int NUMBER_OF_CHANNELS_USED = 10; 
    
    /** 2 channels to store current waypoint X-coordinate, Y-coordinate */
    private final static int X_COORDINATE_WAYPOINT_CHANNEL = BASE_CHANNEL;
    private final static int Y_COORDINATE_WAYPOINT_CHANNEL = BASE_CHANNEL+1;
    
    /** The type of waypoint (might see future use, reserve for now) */
    private final static int MOVE_TYPE_CHANNEL = BASE_CHANNEL+2;
    
    // TODO: These values should be up to 2 clock cycles behind.. write docs
    /** 2 channels to total up the center of group X-coordinate, Y-coordinate */
    private final static int X_COORDINATE_GROUP_CHANNEL_SUM = BASE_CHANNEL+3;
    private final static int Y_COORDINATE_GROUP_CHANNEL_SUM = BASE_CHANNEL+4;

    /** Channel to count number of individuals in group */
    private final static int GROUP_SIZE_CHANNEL = BASE_CHANNEL+5;
    
    /** Heartbeat to allow soldiers to check whether they're the first soldier in the round to process this data structure. If so, perform necessary maintenance. */
    private final static int TOUCHED_CHANNEL = BASE_CHANNEL+6;
    
    // Variables indicating status of group =====================================================
    // These variables may be up to 2 rounds out of date (may be updated every 2
    // rounds), but will always be populated.
    public static MapLocation waypointLocation;
    public static MapLocation groupCenter;
    public static int groupSize;
    
    /**
     * Called by all soldiers when initializing.
     * 
     * @param MapLocation
     * @throws GameActionException 
     */
    public static void sync(MapLocation myLocation) throws GameActionException {
        RobotController rc = RobotPlayer.rc;
        if (rc.readBroadcast(TOUCHED_CHANNEL) != Clock.getRoundNum()) {
            if (Clock.getRoundNum() % 2 == 0) {
                // Even rounds trigger recomputations.. Zero values in channels
                // used for distributed computation.
            }
        }

        // You're the rest of the group
        if (Clock.getRoundNum() % 2 == 0) { // Even round, update values, do not
                                            // read

        } else { // Odd round.. Compute waypointLocation, groupCenter, and groupSize
            
        }
            
    }
    
    /**
     * Set next location to move to.
     * 
     * @param x
     * @param y
     * @param moveType
     */
    public static void setNextLocation(int x, int y, MoveType moveType) {
    }
    
    public static enum MoveType {
        ATTACK, // Attack move to an area as a group.
        RALLY, // Build up forces in an area
        RETREAT // Move, but do not attack
        }

    
    /**
     * Check that a soldier is as close to the initial soldier as possible.
     * 
     * 
     */
    public static void isWaypointReached() {
        
    }
    
    /**
     * 
     */
    public static void putNextWaypoint() {
        
    }
    
}
