package team156.Utility;

import team156.RobotPlayer;
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
    // rounds), but will always be populated appropriately after sync is called.
    public static MapLocation waypointLocation = RobotPlayer.myLocation;
    public static MapLocation groupCenter = RobotPlayer.myLocation;
    public static int groupSize;
    
    // Constants defining behaviour of group =======================================================
    /** Allowable distance squared between group center and waypoint before waypoint is designated as reached. */
    public final static int WAYPOINT_REACHED_DISTANCE_SQUARE_TOLERANCE = 15;
    /** Allowable radius about which waypoint has to be clear of enemies */
    public final static int WAYPOINT_REACHED_ENEMIES_DISTANCE_CHECK = 25;

    /**
     * Called by all soldiers when initializing.
     * 
     * Updates waypointLocation, groupCenter, groupSize on odd rounds.
     * 
     * Writes to these channels instead on even rounds, leaving the stale values
     * of waypointLocation, groupCenter, groupSize.
     * 
     * @param MapLocation your current location
     * @throws GameActionException
     */
    public static void sync(MapLocation myLocation) throws GameActionException {
        RobotController rc = RobotPlayer.rc;
        if (rc.readBroadcast(TOUCHED_CHANNEL) != Clock.getRoundNum()) {
            // You're a leader (the first soldier to execute this round).
            rc.broadcast(TOUCHED_CHANNEL, Clock.getRoundNum()); // Show that a leader has preprocessed this structure (to all other units)
            if (Clock.getRoundNum() % 2 == 0) {
                // Even rounds trigger recomputations.. Zero values in channels
                // used for distributed computation.
                rc.broadcast(X_COORDINATE_GROUP_CHANNEL_SUM, 0);
                rc.broadcast(Y_COORDINATE_GROUP_CHANNEL_SUM, 0);
                rc.broadcast(GROUP_SIZE_CHANNEL, 0);
            }
        }  

        // Code for the rest of the group (and also the leader, since it's also
        // part of the group)
        if (Clock.getRoundNum() % 2 == 0) { // Even round, update values, do not
                                            // read
            rc.broadcast(X_COORDINATE_GROUP_CHANNEL_SUM,
                    rc.readBroadcast(X_COORDINATE_GROUP_CHANNEL_SUM)
                            + myLocation.x);
            rc.broadcast(Y_COORDINATE_GROUP_CHANNEL_SUM,
                    rc.readBroadcast(Y_COORDINATE_GROUP_CHANNEL_SUM)
                            + myLocation.y);
            rc.broadcast(GROUP_SIZE_CHANNEL,
                    rc.readBroadcast(GROUP_SIZE_CHANNEL) + 1);
        } else { // Odd round.. Compute and update waypointLocation,
                 // groupCenter, and groupSize
            final int groupSize = rc.readBroadcast(GROUP_SIZE_CHANNEL);
            if (groupSize > 0) { // Only work if we have more than 1 soldier.
                SoldierGroup.groupSize = groupSize;
                SoldierGroup.groupCenter = new MapLocation(
                        rc.readBroadcast(X_COORDINATE_GROUP_CHANNEL_SUM)
                                / groupSize,
                        rc.readBroadcast(Y_COORDINATE_GROUP_CHANNEL_SUM)
                                / groupSize);
                SoldierGroup.waypointLocation = new MapLocation(
                        rc.readBroadcast(X_COORDINATE_WAYPOINT_CHANNEL),
                        rc.readBroadcast(Y_COORDINATE_WAYPOINT_CHANNEL));
            }
        }

    }

    /**
     * Set next waypoint.
     * 
     * Only HQ should call this, or soldiers could be in an inconsistent state
     * for 2 rounds.
     * 
     * @param x
     * @param y
     * @param moveType ignored for now
     * @throws GameActionException
     */
    public static void setNextWaypoint(int x, int y, MoveType moveType) throws GameActionException {
        RobotPlayer.rc.broadcast(X_COORDINATE_WAYPOINT_CHANNEL, x);
        RobotPlayer.rc.broadcast(Y_COORDINATE_WAYPOINT_CHANNEL, y);
    }    
    
    /**
     * Checks whether the group of soldiers have reached the waypoint. This
     * occurs when the center of the group has distance squared less than
     * WAYPOINT_DISTANCE_SQUARE_TOLERANCE, and no visible enemies are present
     * within 25 radius squared of waypoint.
     * 
     * @return true when waypoint is reached, false otherwise
     */
    public static boolean hasSoldierGroupReachedWaypoint() {
        return groupCenter.distanceSquaredTo(SoldierGroup.waypointLocation) < SoldierGroup.WAYPOINT_REACHED_DISTANCE_SQUARE_TOLERANCE
                && RobotPlayer.rc.senseNearbyRobots(
                        SoldierGroup.waypointLocation,
                        WAYPOINT_REACHED_ENEMIES_DISTANCE_CHECK,
                        RobotPlayer.enemyTeam).length == 0;
    }

    public static enum MoveType {
        ATTACK, // Attack move to an area as a group.
        RALLY, // Build up forces in an area
        RETREAT // Move, but do not attack
    }
    
    
}
