package team157;

import java.util.Random;

import team157.Utility.*;
import battlecode.common.*;
import team157.Utility.*;

public class Drone extends MovableUnit {
    
    //General methods =========================================================
    private static MapLocation target = RobotPlayer.enemyHQLocation;
    private static MapLocation[] tempEnemyLoc;
    private static int numberOfEnemies = 0;
    private static RobotInfo[] enemiesInSight;
    
    
    public static void start() throws GameActionException {
        init();
        while(true) {
            loop();
            rc.yield(); //Yield the round
        }
    }
    
    private static void init() throws GameActionException {  
        robotState = RobotState.UNSWARM;
    }
    
    
    private static void loop() throws GameActionException {
        myLocation = rc.getLocation();
        
        // Code that runs in every robot (including buildings, excepting missiles)
        sharedLoopCode();
        
        setTarget();
        
        switch (robotState) {
            case UNSWARM:
                switchStateFromUnswarmState();
                break;
            case SWARM:
                switchStateFromSwarmState();
                break;
            default:
                throw new IllegalStateException();
        }
        
        //Display state
        rc.setIndicatorString(1, "In state: " + robotState);
        
        droneMoveAttack(target);
    }
    
    /**
     * Set target based on waypoints.
     * @throws GameActionException
     */
    private static void setTarget() throws GameActionException {
        Waypoints.refreshLocalCache();
        // target = Waypoints.waypoints[0];
        if (Waypoints.numberOfAttackWaypoints > 0) {
            target = Waypoints.waypoints[rand.nextInt(Waypoints.numberOfAttackWaypoints)];
        } else if (Waypoints.numberOfWaypoints > 1) {
            target = Waypoints.waypoints[rand.nextInt(Waypoints.numberOfWaypoints - 1)];
        } else if (Waypoints.numberOfWaypoints == 1) {
            target = Waypoints.waypoints[0];
        } 
    }
    
    
    private static void checkForEnemies() throws GameActionException
    {
        enemiesInSight = rc.senseNearbyRobots(sightRange, enemyTeam);
        numberOfEnemies = enemiesInSight.length;
        enemies = enemiesInSight;
            
        // Vigilance: stops everything and attacks when enemies are in attack range.
        while (enemies.length > 0) {
            if (rc.isWeaponReady()) {
                // basicAttack(enemies);
                priorityAttack(enemies, attackPriorities);
            }
            enemies = rc.senseNearbyRobots(attackRange, enemyTeam);
            RobotCount.report();
            rc.yield();
        }
    }

    private static void droneRetreat() throws GameActionException {
        tempEnemyLoc = new MapLocation[numberOfEnemies];
        MapLocation enemyLoc;
        int i = 0;
        while(i < numberOfEnemies) {
            enemyLoc = enemiesInSight[i].location;
            tempEnemyLoc[i] = enemyLoc;
            setInternalMap(enemyLoc, 4);
        }
        if (rc.isCoreReady()) {
            rc.move(chooseAvoidanceDir(myLocation));
        }
    }
    
    private static void droneMoveAttack(MapLocation target) throws GameActionException
    {
        checkForEnemies();

        switch(robotState) {
        case UNSWARM:
            if (numberOfEnemies > 1) {
                droneRetreat();
            } else {
                droneUnswarmPathing(target);
            } 
            break;
        case SWARM:
            droneSwarmPathing(target);
            break;
        default:
            throw new IllegalStateException();
        }

        distributeSupply(suppliabilityMultiplier_Preattack);
    }
    
    /**
     * Switches state to swarm state when >4 friendly units within sensing radius.
     */
    private static void switchStateFromUnswarmState() {
        if (rc.senseNearbyRobots(sightRange, RobotPlayer.myTeam).length > 4) {
            robotState = RobotState.SWARM;
            initAttackState(target);
        }
    }
    
    /**
     * Switches state to unswarm state when <=4 friendly units within sensing radius.
     */
    private static void switchStateFromSwarmState() {
        if (rc.senseNearbyRobots(sightRange, RobotPlayer.myTeam).length < 5) {
            robotState = RobotState.UNSWARM;
            initUnswarmState();
        }
    }
    
    /**
     * Set all locations within sight range of enemy tower and hq as void in internal map.
     */
    private static void initUnswarmState() {
        // set all locations within sight range of tower and hq as void in internal map
        for (MapLocation tower: rc.senseEnemyTowerLocations()) {
            for (MapLocation inSightOfTower: MapLocation.getAllMapLocationsWithinRadiusSq(tower, 35)) {
                RobotPlayer.setInternalMap(inSightOfTower, 10);
            }
        }
        for (MapLocation inSightOfHQ: MapLocation.getAllMapLocationsWithinRadiusSq(rc.senseEnemyHQLocation(),35)) {
            RobotPlayer.setInternalMap(inSightOfHQ, 10);
        }
    }
    
    /**
     * Set all locations within sight of input target as pathable.
     * @param target attack target.
     */
    private static void initAttackState(MapLocation target) {
        for (MapLocation inSightOfTarget: MapLocation.getAllMapLocationsWithinRadiusSq(target, 35)) {
            RobotPlayer.setInternalMap(inSightOfTarget, 0);
        }
    }
    
    /**
     * Drone pathing to input target, avoiding enemy tower and hq, staying at distance
     * >= 7 squares from target.
     * @param target target location.
     * @throws GameActionException
     */
    private static void droneUnswarmPathing(MapLocation target) throws GameActionException {
        // stay at distance >= 7 squares from target if not attacking yet.
        if(myLocation.distanceSquaredTo(target) < 49) {
            return;
        }
        bug(target);
    }
    
    /**
     * Drone swarm pathing to input target.
     * @param target target location
     * @throws GameActionException
     */
    private static void droneSwarmPathing(MapLocation target) throws GameActionException {
        bug(target);
    }
    
    /**
     * Returns true if drone can move in input direction.
     * @param dir target direction.
     * @return true if move is possible in input direction.
     */
    private static boolean droneMovePossible(Direction dir) {
        if (rc.canMove(dir) && (RobotPlayer.getInternalMap(myLocation.add(dir)) < 4)) {
                return true;
        }
        return false;
    }
    
    
    //Specific methods =========================================================
    

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
        0/*12:COMPUTER*/,   1/*13:SOLDIER*/,    1/*14:BASHER*/,     0/*15:MINER*/,
        1/*16:DRONE*/,      1/*17:TANK*/,       1/*18:COMMANDER*/,  1/*19:LAUNCHER*/,
        0/*20:MISSILE*/
    };
    
    
    
}