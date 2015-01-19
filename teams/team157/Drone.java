package team157;

import team157.Utility.*;
import battlecode.common.*;
import team157.Utility.*;

public class Drone extends MovableUnit {

    //General methods =========================================================

    private static DroneState droneState = DroneState.UNSWARM;
    private static int retreatTimeout = 5; // number of rounds before changing from retreat to unswarm state.
    
    public static void start() throws GameActionException {
        init();
        while(true) {
            loop();
            rc.yield(); //Yield the round
        }
    }
    
    private static void init() throws GameActionException {  
        if (Clock.getRoundNum() < roundNumAttack) {
            // set all locations within sight range of tower and hq as void in internal map
            initInternalMap();

        } else {
            droneState = DroneState.KAMIKAZE;
        }
        
        Waypoints.refreshLocalCache();
        target = Waypoints.waypoints[0];
    }
    
    
    private static void loop() throws GameActionException {
        updateMyLocation();

        waypointTimeout--;
        // rc.setIndicatorString(1, "Waypoint timeout " + waypointTimeout + " " + indexInWaypoints + " " + Waypoints.numberOfWaypoints
        //        + " x: " + target.x + "y: " + target.y);
        
        // Code that runs in every robot (including buildings, excepting missiles)
        sharedLoopCode();
        
        if (Clock.getRoundNum() > roundNumAttack) {
            droneState = DroneState.KAMIKAZE;
        }
        
        enemiesInSight = rc.senseNearbyRobots(sightRange, enemyTeam);
        numberOfEnemiesInSight = enemiesInSight.length;
        enemies = rc.senseNearbyRobots(attackRange, enemyTeam);

        setTargetToWayPoints();
        
        // switch state based on number of enemies in sight
        droneSwitchState();
        
        //Display state
        rc.setIndicatorString(1, "In state: " + droneState);
        
        droneMove(target);
        RobotCount.report();
    }

    
    /**
     * Checks for enemies in attack range and attacks them. Attacks only once
     * if not in ATTACK state and continuously attacks if in ATTACK state.
     * @throws GameActionException
     */
    private static void checkForEnemies() throws GameActionException {
        if (droneState == DroneState.KAMIKAZE){
            // continually attacks when enemies are in attack range.
            while (enemies.length > 0) {
                if (rc.isWeaponReady()) {
                    // basicAttack(enemies);
                    priorityAttack(enemies, attackPriorities);
                }
                enemies = rc.senseNearbyRobots(attackRange, enemyTeam);
                rc.yield();
            }
        } else {
            if (enemies.length > 0) {
                if (rc.isWeaponReady()) {
                    // basicAttack(enemies);
                    priorityAttack(enemies, attackPriorities);
                }
            }
        }
    }
    
    //TODO: switch to swarm only if drone units are nearby
    /**
     * Switches drone state based on number of enemies in sight
     */
    private static void droneSwitchState() {
        checkForDanger();
        
        switch (droneState) {
        case UNSWARM:
            if (rc.senseNearbyRobots(sightRange, RobotPlayer.myTeam).length >= numberInSwarm) {
                // Switches to swarm state when >4 friendly units within sensing radius.
                droneState = DroneState.SWARM;
            } else if (numberOfEnemiesInSight > 2 && Clock.getRoundNum() > 500) {
             // goes into retreat state if there are enemies in sight range
                droneState = DroneState.RETREAT;
            }
            break;
        case SWARM:
            if (rc.senseNearbyRobots(sightRange, RobotPlayer.myTeam).length < numberInSwarm) {
                // switch to unswarm state when <5 friendly units within sensing radius.
                droneState = DroneState.UNSWARM;
            }
            break;
        case KAMIKAZE:
            break;
        case FOLLOW:
            if (numberOfEnemiesInSight == 0 || numberOfEnemiesInSight > 2) {
                // lost sight of follow target or too many enemies
                droneState = DroneState.UNSWARM;
            }
            break;
        case RETREAT:
            if (rc.senseNearbyRobots(sightRange, RobotPlayer.myTeam).length >= numberInSwarm) {
                // switch to swarm state when enough friendly units in range
                droneState = DroneState.SWARM;
            } else if (retreatTimeout < 0) {
                // switch to unswarm state when in stationary retreat for enough turns without encountering any enemy
                droneState = DroneState.UNSWARM;
            }
            break;
        case SCOUT:
            //TODO
            break;
        default:
            throw new IllegalStateException();
        }
    }

    /**
     * Attacks or move to target based on drone state.
     * @param target target to move to.
     * @throws GameActionException
     */
    private static void droneMove(MapLocation target) throws GameActionException{
        // first check for enemies and attacks if there are
        checkForEnemies();

        switch(droneState) {
        case UNSWARM:
            // defensive state for lone drones, stays away from target and waits for reinforcements.
            if (keepAwayFromTarget) {
                // target is tower or hq
                if(myLocation.distanceSquaredTo(target) < 35) {
                    return;
                }
            } else {
                if(myLocation.distanceSquaredTo(target) < 25) {
                    return;
                }
            }
            bug(target);
            break;
        case SWARM:
            // aggressive state, bugs toward target
            bug(target);
            break;
        case KAMIKAZE:
            bug(target);
            break;
        case FOLLOW:
            followTarget(enemiesInSight, followPriorities);
            break;
        case RETREAT:
            if (numberOfEnemiesInSight == 0) {
                retreatTimeout--;
            } else {
                retreat();
                retreatTimeout = 5;
            }
            break;
        case SCOUT:
            // TODO scout not implemented yet!
            break;
        default:
            throw new IllegalStateException();
        }

        distributeSupply(suppliabilityMultiplier_Preattack);
    }
    
    /**
     * TODO untested
     * If there is only 1 or 2 enemies in sight, decide whether to follow it or not
     * based on danger rating and health of enemy.
     */
    private static void switchToFollowState(RobotInfo[] enemiesInSight, int numberOfEnemiesInSight) {
        if (numberOfEnemiesInSight == 1) {
            int enemyType = enemiesInSight[0].type.ordinal();
            if (dangerRating[enemyType] == 1) {
                droneState = DroneState.FOLLOW;
            } else if (dangerRating[enemyType] == 2) {
                if (enemiesInSight[0].health < lowHP[enemyType]) {
                    droneState = DroneState.FOLLOW;
                }
            }
        } else if (numberOfEnemiesInSight == 2) {
            if (dangerRating[enemiesInSight[0].type.ordinal()] == 0) {
                switchToFollowState(new RobotInfo[]{enemiesInSight[1]}, 1);
            } else if (dangerRating[enemiesInSight[1].type.ordinal()] == 0) {
                switchToFollowState(new RobotInfo[]{enemiesInSight[0]}, 1);
            }
        }
    }
    
    /**
     * If there is only 1 enemy in sight, decide whether to follow it or not
     * based on danger rating and health of enemy. If there are more than 1
     * enemy in sight, decides if retreat is necessary based on danger rating.
     * @return true if switching to follow state, false otherwise.
     */
    private static void checkForDanger() {
        if (numberOfEnemiesInSight == 0) {
            return;
        } else if (numberOfEnemiesInSight == 1) {
            int enemyType = enemiesInSight[0].type.ordinal();
            int enemyDangerRating = dangerRating[enemyType];
            if (enemyDangerRating == 1) {
                droneState = DroneState.FOLLOW;
            } else if (enemyDangerRating == 2) {
                if (enemiesInSight[0].health < lowHP[enemyType]) {
                    droneState = DroneState.FOLLOW;
                } else if (Clock.getRoundNum() > 500) {
                    droneState = DroneState.RETREAT;
                    retreatTimeout = 5;
                }
            }
        } else {
            for (RobotInfo info: enemiesInSight) {
                int enemyType = info.type.ordinal();
                if (dangerRating[enemyType] == 2) {
                    if (info.health > lowHP[enemyType] && Clock.getRoundNum()>500) {
                        droneState = DroneState.RETREAT;
                        retreatTimeout = 5;
                        return;
                    }
                }
            }
        }
        
    }
    
    /**
     * Returns true if one should retreat from enemies in sight and 
     * false otherwise, based on danger rating and health of enemy.
     * @return
     */
    private static boolean shouldRetreat() {
        if (numberOfEnemiesInSight == 0) {
            return false;
        } else if (numberOfEnemiesInSight == 1) {
            int enemyType = enemiesInSight[0].type.ordinal();
            int enemyDangerRating = dangerRating[enemyType];
            if (enemyDangerRating == 0 || enemyDangerRating == 1) {
                return false;
            } else if (enemyDangerRating == 2) {
                if (enemiesInSight[0].health < lowHP[enemyType]) {
                    return false;
                }
            }
        }
        return true;
    }
    

    //Parameters ==============================================================
    
    /**
     * Danger rating is 3 if one should retreat from it.
     * Danger rating is 2 if one can attack it if it has low hp.
     * Danger rating is 1 if one should follow and attack it.
     * Danger rating is 0 if one should ignore it.
     */
    private static int[] dangerRating = {
        3/*0:HQ*/,         3/*1:TOWER*/,      0/*2:SUPPLYDPT*/,   0/*3:TECHINST*/,
        1/*4:BARRACKS*/,    1/*5:HELIPAD*/,     1/*6:TRNGFIELD*/,   1/*7:TANKFCTRY*/,
        1/*8:MINERFCTRY*/,  0/*9:HNDWSHSTN*/,   1/*10:AEROLAB*/,   1/*11:BEAVER*/,
        0/*12:COMPUTER*/,   1/*13:SOLDIER*/,   2/*14:BASHER*/,    1/*15:MINER*/,
        2/*16:DRONE*/,     2/*17:TANK*/,      2/*18:COMMANDER*/, 2/*19:LAUNCHER*/,
        2/*20:MISSILE*/
    };
    
    /**
     * Values of hp if respective units such that if the unit has lower hp, then
     * one should attack it.
     */
    private static int[] lowHP = {
        100/*0:HQ*/,         100/*1:TOWER*/,      8/*2:SUPPLYDPT*/,   8/*3:TECHINST*/,
        100/*4:BARRACKS*/,    100/*5:HELIPAD*/,     50/*6:TRNGFIELD*/,   100/*7:TANKFCTRY*/,
        100/*8:MINERFCTRY*/,  8/*9:HNDWSHSTN*/,   100/*10:AEROLAB*/,   40/*11:BEAVER*/,
        8/*12:COMPUTER*/,   20/*13:SOLDIER*/,   20/*14:BASHER*/,    50/*15:MINER*/,
        30/*16:DRONE*/,     40/*17:TANK*/,      40/*18:COMMANDER*/, 40/*19:LAUNCHER*/,
        2/*20:MISSILE*/
    };
    
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
    
    /**
     * Multipliers for the effective supply capacity for friendly unit robotTypes, by 
     * which the dispenseSupply() and distributeSupply() methods allocate supply (so 
     * higher means give more supply to units of that type).
     */
    private static double[] suppliabilityMultiplier_Conservative = {
        1/*0:HQ*/,          1/*1:TOWER*/,       1/*2:SUPPLYDPT*/,   1/*3:TECHINST*/,
        1/*4:BARRACKS*/,    1/*5:HELIPAD*/,     1/*6:TRNGFIELD*/,   1/*7:TANKFCTRY*/,
        1/*8:MINERFCTRY*/,  1/*9:HNDWSHSTN*/,   1/*10:AEROLAB*/,    0/*11:BEAVER*/,
        0/*12:COMPUTER*/,   0/*13:SOLDIER*/,    0/*14:BASHER*/,     0.5/*15:MINER*/,
        0/*16:DRONE*/,      0/*17:TANK*/,       0/*18:COMMANDER*/,  0/*19:LAUNCHER*/,
        0/*20:MISSILE*/
    };
    
    private static double[] suppliabilityMultiplier_Preattack = {
        0/*0:HQ*/,          0/*1:TOWER*/,       0/*2:SUPPLYDPT*/,   0/*3:TECHINST*/,
        0/*4:BARRACKS*/,    0/*5:HELIPAD*/,     0/*6:TRNGFIELD*/,   0/*7:TANKFCTRY*/,
        0/*8:MINERFCTRY*/,  0/*9:HNDWSHSTN*/,   0/*10:AEROLAB*/,    1/*11:BEAVER*/,
        0/*12:COMPUTER*/,   1/*13:SOLDIER*/,    1/*14:BASHER*/,     1/*15:MINER*/,
        1/*16:DRONE*/,      1/*17:TANK*/,       1/*18:COMMANDER*/,  1/*19:LAUNCHER*/,
        0/*20:MISSILE*/
    };
    
    /**
     * The importance rating that enemy units of each RobotType should be followed 
     * (so higher means follow first).
     */
    private static int[] followPriorities = {
        0/*0:HQ*/,         0/*1:TOWER*/,      0/*2:SUPPLYDPT*/,   0/*3:TECHINST*/,
        0/*4:BARRACKS*/,    0/*5:HELIPAD*/,     0/*6:TRNGFIELD*/,   0/*7:TANKFCTRY*/,
        0/*8:MINERFCTRY*/,  0/*9:HNDWSHSTN*/,   0/*10:AEROLAB*/,   1/*11:BEAVER*/,
        0/*12:COMPUTER*/,   3/*13:SOLDIER*/,   4/*14:BASHER*/,    2/*15:MINER*/,
        9/*16:DRONE*/,     6/*17:TANK*/,      5/*18:COMMANDER*/, 7/*19:LAUNCHER*/,
        8/*20:MISSILE*/
    };
    
    
    
    
}