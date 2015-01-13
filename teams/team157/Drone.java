package team157;

import team157.Utility.*;
import battlecode.common.*;
import team157.Utility.*;

public class Drone extends MovableUnit {

        
    //General methods =========================================================
    private static MapLocation target = RobotPlayer.enemyHQLocation;
    private static MapLocation[] tempEnemyLoc;
    private static int numberOfEnemies = 0;
    private static RobotInfo[] enemiesInSight;
    private static boolean switchTarget = false;
    private static MapLocation previousTarget = target;
    private static int indexInWaypoints = 0;
    private static int waypointTimeout = 100;
    private static final int roundNumAttack = 1750;
    private static final int numberInSwarm = 5;
    private static boolean keepAwayFromTarget = false;
    
    
    public static void start() throws GameActionException {
        init();
        while(true) {
            loop();
            rc.yield(); //Yield the round
        }
    }
    
    private static void init() throws GameActionException {  
        robotState = RobotState.UNSWARM;
        if (Clock.getRoundNum() < roundNumAttack) {
            initUnswarmState();
        }
        
        Waypoints.refreshLocalCache();
        target = Waypoints.waypoints[0];
    }
    
    
    private static void loop() throws GameActionException {
        myLocation = rc.getLocation();
        waypointTimeout--;
        rc.setIndicatorString(1, "Waypoint timeout " + waypointTimeout + " " + indexInWaypoints + " " + Waypoints.numberOfWaypoints
                + " x: " + target.x + "y: " + target.y);
        
        // Code that runs in every robot (including buildings, excepting missiles)
        sharedLoopCode();
        
        if (Clock.getRoundNum() == roundNumAttack) {
            resetInternalMap();
        }
        
        enemiesInSight = rc.senseNearbyRobots(sightRange, enemyTeam);
        numberOfEnemies = enemiesInSight.length;
        
        setTargetToWayPoints();
        
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
    private static void setTargetToWayPoints() throws GameActionException {
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
    
    
    private static void checkForEnemies() throws GameActionException
    {
        enemies = enemiesInSight;
        if (robotState == robotState.UNSWARM){
            if (enemies.length > 0) {
                if (rc.isWeaponReady()) {
                    // basicAttack(enemies);
                    priorityAttack(enemies, attackPriorities);
                }
                enemies = rc.senseNearbyRobots(attackRange, enemyTeam);
                RobotCount.report();
                rc.yield();
            }
        } else {
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
        
    }

    // TODO can't identify bug, do not use!!!
    private static void droneRetreat() throws GameActionException {
        rc.setIndicatorString(1, "retreat");
        tempEnemyLoc = new MapLocation[numberOfEnemies];
        MapLocation enemyLoc;
        int i = 0;
        while(i < numberOfEnemies) {
            enemyLoc = enemiesInSight[i].location;
            tempEnemyLoc[i] = enemyLoc;
            setInternalMap(enemyLoc, 1);
            i++;
        }
        Direction chosenDir = Direction.NONE;
        if (rc.isCoreReady()) {
            chosenDir = chooseAvoidanceDir(myLocation);
            if (chosenDir!= Direction.NONE && chosenDir!= Direction.OMNI){
                rc.move(chosenDir);
            }
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
        if (rc.senseNearbyRobots(sightRange, RobotPlayer.myTeam).length >= numberInSwarm) {
            robotState = RobotState.SWARM;
            initAttackState(target);
        }
    }
    
    /**
     * Switches state to unswarm state when <=4 friendly units within sensing radius
     * or when moving to a new target.
     */
    private static void switchStateFromSwarmState() {
        if (switchTarget || rc.senseNearbyRobots(sightRange, RobotPlayer.myTeam).length < numberInSwarm) {
            robotState = RobotState.UNSWARM;
            if (Clock.getRoundNum() < roundNumAttack) {
                initUnswarmState();
            }
        }
    }
    
    /**
     * Set all locations within sight range of enemy tower and hq as void in internal map.
     */
    private static void initUnswarmState() {
        // set all locations within sight range of tower and hq as void in internal map
        for (MapLocation tower: rc.senseEnemyTowerLocations()) {
            for (MapLocation inSightOfTower: MapLocation.getAllMapLocationsWithinRadiusSq(tower, 35)) {
                setInternalMapWithoutSymmetry(inSightOfTower, 9);
            }
        }
        for (MapLocation inSightOfHQ: MapLocation.getAllMapLocationsWithinRadiusSq(rc.senseEnemyHQLocation(),35)) {
            setInternalMapWithoutSymmetry(inSightOfHQ, 7);
        }
        //printInternalMap();
        //System.out.println(getInternalMap(rc.senseEnemyTowerLocations()[0].add(Direction.NORTH)));
    }
    
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
    /**
     * Set all locations within sight of input target as pathable.
     * @param target attack target.
     */
    private static void initAttackState(MapLocation target) {
        // check if target is tower or hq
        if (keepAwayFromTarget) {
            return;
        }
        for (MapLocation inSightOfTarget: MapLocation.getAllMapLocationsWithinRadiusSq(target, 35)) {
            RobotPlayer.setInternalMapWithoutSymmetry(inSightOfTarget, 0);
        }
    }
    
    /**
     * Drone pathing to input target, avoiding enemy tower and hq, staying at distance
     * >= 7 squares from target.
     * @param target target location.
     * @throws GameActionException
     */
    private static void droneUnswarmPathing(MapLocation target) throws GameActionException {
        if (keepAwayFromTarget) {
            // stay at distance >= 7 squares from target if not attacking yet.
            if(myLocation.distanceSquaredTo(target) < 35) {
                return;
            }
        } else {
            if(myLocation.distanceSquaredTo(target) < 15) {
                return;
            }
        }

        if (Clock.getRoundNum() < roundNumAttack) {
            bug(target);
        } else {
            exploreRandom(target);
        }
        
    }
    
    /**
     * Drone swarm pathing to input target.
     * @param target target location
     * @throws GameActionException
     */
    private static void droneSwarmPathing(MapLocation target) throws GameActionException {        
        if (Clock.getRoundNum() < roundNumAttack) {
            bug(target);
        } else {
            exploreRandom(target);
        }
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