package team157;

import java.util.Random;

import battlecode.common.*;

public class Drone extends MovableUnit {
    
    //General methods =========================================================
    
    private static MapLocation myLocation;
    
    public static void start() throws GameActionException {
        init();
        while(true) {
            loop();
            rc.yield(); //Yield the round
        }
    }
    
    private static void init() throws GameActionException {
        rc.setIndicatorString(0,"hello i'm a drone.");
        
    }
    
    
    private static void loop() throws GameActionException {
        
        
    }
    
    private static void checkForEnemies() throws GameActionException
    {
        RobotInfo[] enemies = rc.senseNearbyRobots(sightRange, enemyTeam);

        // Vigilance: stops everything and attacks when enemies are in attack range.
        while (enemies.length > 0) {
            if (rc.isWeaponReady()) {
                // basicAttack(enemies);
                priorityAttack(enemies, attackPriorities);
            }
            enemies = rc.senseNearbyRobots(attackRange, enemyTeam);
            rc.yield();
        }
    }
    
    /**
     * Switches state to swarm state when >4 friendly units within sensing radius.
     */
    private static void switchStateFromUnswarmState() {
        if (rc.senseNearbyRobots(sightRange, RobotPlayer.myTeam).length > 4) {
            robotState = RobotState.SWARM;
        }
    }
    
    /**
     * Switches state to unswarm state when <=4 friendly units within sensing radius.
     */
    private static void switchStateFromSwarmState() {
        if (rc.senseNearbyRobots(sightRange, RobotPlayer.myTeam).length < 5) {
            robotState = RobotState.UNSWARM;
        }
    }
    
    /**
     * Set all locations within sight range of enemy tower and hq as void in internal map.
     */
    private static void initUnswarmState() {
        // set all locations within sight range of tower and hq as void in internal map
        for (MapLocation tower: rc.senseEnemyTowerLocations()) {
            for (MapLocation inSightOfTower: MapLocation.getAllMapLocationsWithinRadiusSq(tower, 35)) {
                RobotPlayer.setInternalMap(inSightOfTower, 4);
            }
        }
        for (MapLocation inSightOfHQ: MapLocation.getAllMapLocationsWithinRadiusSq(rc.senseEnemyHQLocation(),35)) {
            RobotPlayer.setInternalMap(inSightOfHQ, 4);
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
     * >= 7 squares from target if in unswarm state.
     * @param target target location.
     * @throws GameActionException
     */
    private static void droneExplore(MapLocation target) throws GameActionException {
        if(rc.isCoreReady()) {
            myLocation = rc.getLocation();
            
            // stay at distance >= 7 squares from target if not attacking yet.
            if(robotState == RobotState.UNSWARM && myLocation.distanceSquaredTo(target) < 49) {
                return;
            }
            
            int dirInt = myLocation.directionTo(target).ordinal();
            int offsetIndex = 0;
            while (offsetIndex < 5 && !droneMovePossible(directions[(dirInt+offsets[offsetIndex]+8)%8])) {
                offsetIndex++;
            }
            if (offsetIndex < 5) {
                Direction dirToMove = directions[(dirInt+offsets[offsetIndex]+8)%8];
                rc.move(dirToMove);
                previousDirection = dirToMove;
            }
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
    
    
}