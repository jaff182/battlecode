package team157;

import java.util.Random;

import team157.Utility.SoldierGroup;
import battlecode.common.*;

public class Soldier extends MovableUnit {
    
    public static SoldierState state = SoldierState.ATTACK_MOVE;
    private static MapLocation moveTargetLocation = RobotPlayer.HQLocation;
    
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

        SoldierGroup.sync(myLocation);
        
        // Code that runs in every robot (including buildings, excepting missiles)
        sharedLoopCode();
        
        // Transition
        // TODO: refactor
        if (myLocation.distanceSquaredTo(SoldierGroup.groupCenter) > distanceSquaredFromCenterOfGroupBeforeLost) {
            // Override state if too far from group, to get it to regroup.
            state = SoldierState.JOIN_GROUP;
            moveTargetLocation = SoldierGroup.groupCenter;
        } else if (!SoldierGroup.waypointLocation.equals(moveTargetLocation)) { 
            // Override state if waypoint has changed, update moveTargetLocation
            state = SoldierState.ATTACK_MOVE;
            moveTargetLocation = SoldierGroup.waypointLocation;
        } else { 
            // Else, perform standard state transitions
            switch (state) {
            case ATTACK_MOVE:
                if (checkIfWaypointReached())
                    state = SoldierState.WAIT;
                break;
            case RETREAT:
                if (checkIfWaypointReached())
                    state = SoldierState.WAIT;
                break;
            case WAIT: // No escape from WAIT, unless it gets left behind or waypoint is moved.
                break;
            case JOIN_GROUP: // Keep trying to join the group we've lost, no matter the cost.
                break;
            }
            
        }
        
        // Action
        // TODO: refactor
        switch (state) {
        case ATTACK_MOVE:
            priorityAttack(enemies, attackPriorities); //attack (if any)
            bug(moveTargetLocation); //move (if can move, since no cooldown)
            break;
        case RETREAT:
            bug(moveTargetLocation); //move (if can move, since no cooldown)
            break;
        case WAIT:
            priorityAttack(enemies, attackPriorities); //attack (if any)
            break;
        case JOIN_GROUP:
            break;
        }
    }
    
    /**
     * Checks if the conditions for the current waypoint (in currentWaypoint)
     * has been fulfilled.
     * 
     * This requires that the center of the group be within 4 square of the target waypoint.
     * 
     * In ATTACK_MOVE and WAIT, this requires that no enemies are attackable by any unit.
     * 
     * @return true if they have been, and false otherwise
     */
    private static boolean checkIfWaypointReached() {
        // TODO Auto-generated method stub
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