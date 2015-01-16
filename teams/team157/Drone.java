package team157;

import team157.Utility.*;
import battlecode.common.*;
import team157.Utility.*;

public class Drone extends MovableUnit {

    //General methods =========================================================

    private static MapLocation target = RobotPlayer.enemyHQLocation;
    private static int numberOfEnemies = 0;
    private static RobotInfo[] enemiesInSight;
    private static boolean switchTarget = false;
    private static MapLocation previousTarget = target;
    private static int indexInWaypoints = 0;
    private static int waypointTimeout = 100; // timeout before waypoint changes
    private static final int roundNumAttack = 1750; // round number when end game attack starts
    private static final int numberInSwarm = 5; // minimum size of group for drones to start swarming
    private static boolean keepAwayFromTarget = false; // true if target is tower or hq, false otherwise
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
        numberOfEnemies = enemiesInSight.length;
        
        setTargetToWayPoints();
        
        switch (droneState) {
            case UNSWARM:
                switchStateFromUnswarmState();
                break;
            case SWARM:
                switchStateFromSwarmState();
                break;
            case KAMIKAZE:
                break;
            case FOLLOW:
                break;
            case RETREAT:
                switchStateFromRetreatState();
                break;
            case SCOUT:
                break;
            default:
                throw new IllegalStateException();
        }
        
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
                (myLocation.distanceSquaredTo(target) < 24 && numberOfEnemies == 0)) {
            previousTarget = target;
            indexInWaypoints = Math.min(indexInWaypoints + 1, Waypoints.numberOfWaypoints - 1);
            target = Waypoints.waypoints[indexInWaypoints];
            if (targetIsTowerOrHQ(target)) {
                keepAwayFromTarget = true;
            } else{
                keepAwayFromTarget = false;
            }
            waypointTimeout = 100;
        }
        switchTarget = !target.equals(previousTarget);
    }
    
    /**
     * Checks for enemies in attack range and attacks them. Attacks only once
     * if not in ATTACK state and continuously attacks if in ATTACK state.
     * @throws GameActionException
     */
    private static void checkForEnemies() throws GameActionException {
        
        if (droneState == DroneState.KAMIKAZE){
            enemies = rc.senseNearbyRobots(attackRange, enemyTeam);
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
            enemies = rc.senseNearbyRobots(attackRange, enemyTeam);
            if (enemies.length > 0) {
                if (rc.isWeaponReady()) {
                    // basicAttack(enemies);
                    priorityAttack(enemies, attackPriorities);
                }
            }
        }
    }

    
    private static void droneMove(MapLocation target) throws GameActionException{
        // first check for enemies and attacks if there are
        checkForEnemies();

        switch(droneState) {
        case UNSWARM:
            // defensive state for lone drones
            if (numberOfEnemies > 0) {
                // goes into retreat state if there are enemies in sight range
                retreat();
                droneState = DroneState.RETREAT;
                retreatTimeout = 5;
            } else {
                // stays away from target and waits for reinforcements.
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
            }
            break;
        case SWARM:
            // aggressive state, bugs toward target
            bug(target);
            break;
        case KAMIKAZE:
            bug(target);
            break;
        case FOLLOW:
            // TODO follow method not implemented yet!
            break;
        case RETREAT:
            if (numberOfEnemies > 0) {
                retreat();
                retreatTimeout = 5;
            } else {
                retreatTimeout--;
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
    
    
    // switch states =============================================================
    
    /**
     * Switches state to swarm state when >4 friendly units within sensing radius.
     */
    private static void switchStateFromUnswarmState() {
        if (rc.senseNearbyRobots(sightRange, RobotPlayer.myTeam).length >= numberInSwarm) {
            droneState = DroneState.SWARM;
        }
    }
    
    /**
     * Switches state to unswarm state when <=4 friendly units within sensing radius
     * or when moving to a new target.
     */
    private static void switchStateFromSwarmState() {
        if (switchTarget || rc.senseNearbyRobots(sightRange, RobotPlayer.myTeam).length < numberInSwarm) {
            droneState = DroneState.UNSWARM;
        }
    }
    
    private static void switchStateFromRetreatState() {
        if (rc.senseNearbyRobots(sightRange, RobotPlayer.myTeam).length >= numberInSwarm) {
            droneState = DroneState.SWARM;
        } else if (retreatTimeout < 0) {
            droneState = DroneState.UNSWARM;
        }
        
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
        20/*0:HQ*/,         21/*1:TOWER*/,   19/*20:MISSILE*/,    
        14/*16:DRONE*/,     11/*19:LAUNCHER*/, 17/*17:TANK*/,
        18/*18:COMMANDER*/, 10/*10:AEROLAB*/, 8/*5:HELIPAD*/, 
        9/*7:TANKFCTRY*/, 7/*4:BARRACKS*/, 4/*8:MINERFCTRY*/,
        5/*6:TRNGFIELD*/, 15/*14:BASHER*/, 16/*13:SOLDIER*/,     
        13/*11:BEAVER*/,  12/*15:MINER*/, 6/*2:SUPPLYDPT*/,   
        3/*3:TECHINST*/, 1/*9:HNDWSHSTN*/, 2/*12:COMPUTER*/,   
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
    
    
    
}