package team157;

import team157.Utility.*;
import battlecode.common.*;
import team157.Utility.*;

public class Drone extends MovableUnit {

    //General methods =========================================================

    private static MapLocation target = RobotPlayer.enemyHQLocation;
    private static int numberOfEnemiesInSight = 0;
    private static RobotInfo[] enemiesInSight;
    private static int indexInWaypoints = 0;
    private static int waypointTimeout = 100; // timeout before waypoint changes
    private static final int roundNumAttack = 1750; // round number when end game attack starts
    private static final int numberInSwarm = 5; // minimum size of group for drones to start swarming
    private static boolean keepAwayFromTarget = false; // true if target is tower or hq, false otherwise
    private static DroneState droneState = DroneState.UNSWARM;
    private static int retreatTimeout = 5; // number of rounds before changing from retreat to unswarm state.
    private static RobotInfo attackTarget; // current attack target
    
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
        myLocation = rc.getLocation();
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
     * Set target based on waypoints.
     * @throws GameActionException
     */
    private static void setTargetToWayPoints() throws GameActionException {
        if (Clock.getRoundNum() > roundNumAttack) {
            // end game attack on towers and then hq
            MapLocation[] towerLoc = rc.senseEnemyTowerLocations();
            int enemyAttackRadius = towerAttackRadius;
            if (rc.senseEnemyTowerLocations().length != 0) {
                target = towerLoc[0];
            } else {
                target = rc.senseEnemyHQLocation();
                enemyAttackRadius = HQAttackRadius;
            }
            // set area around target as pathable
            int targetID = Map.getInternalMap(target);
            for (MapLocation inSightOfTarget: MapLocation.getAllMapLocationsWithinRadiusSq(target, enemyAttackRadius)) {          
                if (!rc.senseTerrainTile(inSightOfTarget).equals(TerrainTile.OFF_MAP)) {
                    if (Map.getInternalMap(inSightOfTarget) <= targetID) {
                        Map.setInternalMapWithoutSymmetry(inSightOfTarget, 0);
                    }        
                }
            }
        }
        
		// switch target to next waypoint if timeout is reached or close to target but
		// no enemies in sight
        if (waypointTimeout <= 0 ||
                (myLocation.distanceSquaredTo(target) < 24 && numberOfEnemiesInSight == 0)) {
            indexInWaypoints = Math.min(indexInWaypoints + 1, Waypoints.numberOfWaypoints - 1);
            target = Waypoints.waypoints[indexInWaypoints];
            if (targetIsTowerOrHQ(target)) {
                keepAwayFromTarget = true;
            } else{
                keepAwayFromTarget = false;
            }
            waypointTimeout = 100;
        }
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
        switch (droneState) {
        case UNSWARM:
            if (numberOfEnemiesInSight == 1 && !isBuilding(enemiesInSight[0].type.ordinal())) {
                // lone enemy in sight which is not a building
                droneState = DroneState.FOLLOW;
            } else if (rc.senseNearbyRobots(sightRange, RobotPlayer.myTeam).length >= numberInSwarm) {
                // Switches to swarm state when >4 friendly units within sensing radius.
                droneState = DroneState.SWARM;
            } else if (numberOfEnemiesInSight > 1) {
             // goes into retreat state if there are enemies in sight range
                droneState = DroneState.RETREAT;
            }
            break;
        case SWARM:
            if (numberOfEnemiesInSight == 1 && !isBuilding(enemiesInSight[0].type.ordinal())) {
                // lone enemy in sight which is not a building
                droneState = DroneState.FOLLOW;
            } else if (rc.senseNearbyRobots(sightRange, RobotPlayer.myTeam).length < numberInSwarm) {
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
                if(myLocation.distanceSquaredTo(target) < 16) {
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
            if (enemies.length == 0) {
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
    
    //Misc. =========================================================
    
    
    
    /**
     * Returns true if current target is an enemy tower or HQ, and false otherwise.
     * @param target current target
     * @return true if current target is enemy tower or HQ, and false otherwise.
     */
    private static boolean targetIsTowerOrHQ(MapLocation target) {
        for (MapLocation tower: rc.senseEnemyTowerLocations()) {
            if (target.equals(tower)) {
                return true;
            }
        }
        if (target.equals(rc.senseEnemyHQLocation())) {
            return true;
        }
        return false;
    }
    

    //Parameters ==============================================================
    
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