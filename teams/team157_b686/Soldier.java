package team157_b686;

import team157_b686.Utility.SoldierGroup;
import battlecode.common.*;

public class Soldier extends MovableUnit {
    
    public static SoldierState state = SoldierState.ATTACK_MOVE;
    private static MapLocation moveTargetLocation = Common.HQLocation;
    
    private static SoldierState oldStateBeforeJoinGroupTriggered;
    
    // Constants to tune behaviour ============================================
    /**
     * Square distance from center of group before robot will start bugging
     * towards the center of group instead of the waypoint
     */
    public final static int distanceSquaredFromCenterOfGroupBeforeLost = 10;
    
    //General methods =========================================================
    
    public static void start() throws GameActionException {
        init();
        while(true) {
            loop();
            rc.yield(); //Yield the round
        }
    }
    
    private static void init() throws GameActionException {
        initInternalMap(); //set locations within attack radius of enemy tower or hq as unpathable
        
    }
    
    private static void loop() throws GameActionException {
        // Update any pertinent data structures
        myLocation = rc.getLocation();

        enemies = rc.senseNearbyRobots(attackRange, enemyTeam);
        
        SoldierGroup.sync(myLocation);
        
        // Code that runs in every robot (including buildings, excepting missiles)
        sharedLoopCode();
        
        // Transition
        // TODO: refactor

        // Else, perform standard state transitions
        switch (state) {
        case ATTACK_MOVE:
            if (SoldierGroup.hasSoldierGroupReachedWaypoint() && locationToWaypointHasHighDensity())
                state = SoldierState.WAIT;
            break;
        case WAIT:
            if (!SoldierGroup.hasSoldierGroupReachedWaypoint() || !locationToWaypointHasHighDensity())
                state = SoldierState.ATTACK_MOVE;
            break;
        default:
            throw new IllegalStateException("Illegal state " + state + " reached");
        }
//        rc.setIndicatorString(1, "Waypoint is reached: " + SoldierGroup.hasSoldierGroupReachedWaypoint() + ", Waypoint is filled: " +  locationToWaypointHasHighDensity());
        rc.setIndicatorString(2, "State " + state + ", moveTarget: " + moveTargetLocation + ", groupCenter: " + SoldierGroup.groupCenter);
        // Action
        // TODO: refactor
        switch (state) {
        case ATTACK_MOVE:
            if (enemies.length != 0 && rc.isWeaponReady())
                priorityAttack(enemies, attackPriorities); // attack (if any)
            else if (enemies.length == 0)
                bug(SoldierGroup.waypointLocation); // move (if can move, since no
                                         // cooldown)
            break;
        case RETREAT:
            bug(SoldierGroup.waypointLocation); //move (if can move, since no cooldown)    
            break;
        case WAIT:
            priorityAttack(enemies, attackPriorities); //attack (if any)
            break;
        case JOIN_GROUP:
            if (enemies.length != 0 && rc.isWeaponReady())
                priorityAttack(enemies, attackPriorities); // attack (if any)
            bug(SoldierGroup.groupCenter); // move (if can move, since no
                                         // cooldown)
            break;
        }
    }
    
    /**
     * Checks whether the "area" around the waypoint this robot is trying to
     * advance to is already 80% packed with friendly robots.
     * 
     * This "area" is formed by the imaginary square a robot is at the edge of,
     * centered at the waypoint.
     * 
     * @return
     */
    public static boolean locationToWaypointHasHighDensity() {
        final int distanceSquaredToWaypoint = myLocation.distanceSquaredTo(SoldierGroup.waypointLocation);
        return Common.rc.senseNearbyRobots(SoldierGroup.waypointLocation,
                distanceSquaredToWaypoint, Common.myTeam).length > 0.8*distanceSquaredToWaypoint;
    }
    
    /**
     * Checks if group is dispersed.
     * 
     * @return
     */
    public static boolean isGroupDispersed() {
        return Common.rc.senseNearbyRobots(SoldierGroup.groupCenter,
                SoldierGroup.groupSize, Common.myTeam).length < 0.8*SoldierGroup.groupSize;
    }
    
    /**
     * BUGGY, DO NOT USE..
     * 
     * Checks if the current location is reached.
     * 
     * A moveTarget is reached when both 1) The center of group is near the
     * location <br>
     * 2) Bugging from the soldier to the soldier's individual moveTarget (which
     * might be the waypoint, or group center), will not move the soldier closer
     * to it<br>
     * 
     * 
     * In ATTACK_MOVE and WAIT, this requires that no enemies are attackable by
     * any unit.
     * 
     * This call will always return false if in WAIT state.
     * 
     * @return true if they have been, and false otherwise
     * @throws GameActionException
     */
    private static boolean checkIfLocationReached(MapLocation location)
            throws GameActionException {
        final boolean centerOfGroupNearTarget = SoldierGroup.groupCenter
                .distanceSquaredTo(location) < 10;
        Direction bugDirection = MovableUnit.bugDirection(moveTargetLocation);
        final boolean noPathToWaypoint = bugDirection == Direction.NONE
                || myLocation.distanceSquaredTo(moveTargetLocation)
                        - myLocation.add(bugDirection).distanceSquaredTo(
                                moveTargetLocation) > 1; // Check that
                                                         // bugging will be
                                                         // in a direction
                                                         // somewhat towards
                                                         // waypoint
        return noPathToWaypoint && centerOfGroupNearTarget;
    }

    //Specific methods =========================================================
    /**
     * Checks for enemies in attack range and attacks them if in ATTACK_MOVE or
     * WAIT state.
     *      * 
     * @throws GameActionException
     */
    private static void checkForEnemiesInAttackRange() throws GameActionException {
        
        if (state == SoldierState.ATTACK_MOVE || state == SoldierState.WAIT){
            enemies = rc.senseNearbyRobots(attackRange, enemyTeam);
            // continually attacks when enemies are in attack range.
            while (enemies.length > 0) {
                if (rc.isWeaponReady()) {
                    // basicAttack(enemies);
                    priorityAttack(enemies, attackPriorities);
                }
                enemies = rc.senseNearbyRobots(attackRange, enemyTeam);
            }
        }
    }

        /**
         * The importance rating that enemy units of each RobotType should be attacked 
         * (so higher means attack first). Needs to be adjusted dynamically based on 
         * defence strategy.
         */
        private static int[] attackPriorities = {
            20/*0:HQ*/,         21/*1:TOWER*/,      6/*2:SUPPLYDPT*/,   3/*3:TECHINST*/,
            7/*4:BARRACKS*/,    8/*5:HELIPAD*/,     5/*6:TRNGFIELD*/,   9/*7:TANKFCTRY*/,
            4/*8:MINERFCTRY*/,  1/*9:HNDWSHSTN*/,   10/*10:AEROLAB*/,   13/*11:BEAVER*/,
            2/*12:COMPUTER*/,   16/*13:SOLDIER*/,   15/*14:BASHER*/,    12/*15:MINER*/,
            14/*16:DRONE*/,     17/*17:TANK*/,      18/*18:COMMANDER*/, 11/*19:LAUNCHER*/,
            19/*20:MISSILE*/
        };

    public static enum SoldierState {
        WAIT, // Stay stationary
        ATTACK_MOVE, // Move towards a location, attacking units it sees
        RETREAT, // Move towards a location, without attacking
        JOIN_GROUP, // Too far from center of group (from distanceSquaredFromCenterOfGroupBeforeLost)
                    // Will keep trying to join the group we've lost, no matter the cost.
    }
}