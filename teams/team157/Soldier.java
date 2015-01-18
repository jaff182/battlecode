package team157;

import java.util.Random;

import team157.Utility.SoldierGroup;
import battlecode.common.*;

public class Soldier extends MovableUnit {
    
    public static SoldierState state = SoldierState.ATTACK_MOVE;
    private static MapLocation moveTargetLocation = RobotPlayer.HQLocation;
    
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
            if (myLocation.distanceSquaredTo(SoldierGroup.groupCenter) > distanceSquaredFromCenterOfGroupBeforeLost) {
                // Override state if too far from group, to get it to
                // regroup.
                oldStateBeforeJoinGroupTriggered = state;
                state = SoldierState.JOIN_GROUP;
            } else if (!SoldierGroup.waypointLocation
                    .equals(moveTargetLocation)) {
                // Override state if waypoint has changed, update
                // moveTargetLocation
                state = SoldierState.ATTACK_MOVE;
            } else if (checkIfMoveTargetReached())
                state = SoldierState.WAIT;
            break;
        case RETREAT:
            if (myLocation.distanceSquaredTo(SoldierGroup.groupCenter) > distanceSquaredFromCenterOfGroupBeforeLost) {
                // Override state if too far from group, to get it to
                // regroup.
                oldStateBeforeJoinGroupTriggered = state;
                state = SoldierState.JOIN_GROUP;
            } else if (!SoldierGroup.waypointLocation
                    .equals(moveTargetLocation)) {
                // Override state if waypoint has changed, update
                // moveTargetLocation
                state = SoldierState.ATTACK_MOVE;
                moveTargetLocation = SoldierGroup.waypointLocation;
            } else if (checkIfMoveTargetReached())
                state = SoldierState.WAIT;
            break;
        case WAIT: // No escape from WAIT, unless waypoint is moved (see
                   // above)
            break;
        case JOIN_GROUP:
            if (myLocation.distanceSquaredTo(SoldierGroup.groupCenter) <= distanceSquaredFromCenterOfGroupBeforeLost) {
                state = oldStateBeforeJoinGroupTriggered; // Restore old
                                                          // state
            } else if (checkIfMoveTargetReached()) {
                state = oldStateBeforeJoinGroupTriggered; 
            }
            break;
        }
        rc.setIndicatorString(2, "State " + state + ", moveTarget: " + moveTargetLocation + ", groupCenter: " + SoldierGroup.groupCenter);
        // Action
        // TODO: refactor
        switch (state) {
        case ATTACK_MOVE:
            if (enemies.length != 0 && rc.isWeaponReady())
                priorityAttack(enemies, attackPriorities); // attack (if any)
            else if (enemies.length == 0)
                bug(moveTargetLocation); // move (if can move, since no
                                         // cooldown)
            break;
        case RETREAT:
            bug(moveTargetLocation); //move (if can move, since no cooldown)    
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
     * Checks if the current moveTarget is reached.
     * 
     * A moveTarget is reached when both 1) The center of group is near the
     * waypoint <br>
     * 2) Bugging from the soldier to the soldier's individual
     * moveTarget (which might be the waypoint, or group center), will not move
     * the soldier closer to it<br>
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
    private static boolean checkIfMoveTargetReached() throws GameActionException {
        switch (state) {
        case ATTACK_MOVE:
        case RETREAT:
        case JOIN_GROUP:
            final boolean centerOfGroupNearTarget = SoldierGroup.groupCenter
                    .distanceSquaredTo(SoldierGroup.waypointLocation) < 10;
            Direction bugDirection = MovableUnit
                    .bugDirection(moveTargetLocation);
            final boolean noPathToWaypoint = bugDirection == Direction.NONE
                    || myLocation.distanceSquaredTo(moveTargetLocation)
                            - myLocation.add(bugDirection).distanceSquaredTo(
                                    moveTargetLocation) > 1; // Check that
                                                             // bugging will be
                                                             // in a direction
                                                             // somewhat towards
                                                             // waypoint
            return noPathToWaypoint && centerOfGroupNearTarget;
        case WAIT:
            return false;
        }
        return false;
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
    
}