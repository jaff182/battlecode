package launcherBot;

import launcherBot.Utility.*;
import battlecode.common.*;

public class Drone extends MovableUnit {

    //General methods =========================================================

    private static DroneState droneState = DroneState.UNSWARM;
    private static int retreatTimeout = 10; // number of rounds before changing from retreat to unswarm state.
    private static int baseRetreatTimeout = 10;
    //private static int numberInSwarm = 10;
    
    // parameters for supply drone
    private static double minSupplyLevelOfSupplyDrone = 4000; //min supply level before moving to supply target
    private static double lowSupplyLevel = 400; //min supply level before returning to hq to resupply
    public static int roundNumSupply = 0; // round number after which all newly spawned drones are supply drones
    private static int baseSupplyTimeout = 15;
    private static int supplyTimeout = baseSupplyTimeout; // number of rounds after staying near supply target before returning to hq
    private static int supplyDistributeRadius = 255;
    private static int[] ordinalOffsets = {0, 7, 1, 6, 2, 5, 3, 4};

    
    public static void start() throws GameActionException {
        init();
        while(true) {
            loop();
            rc.yield(); //Yield the round
        }
    }
    
    private static void init() throws GameActionException {  
        if (Clock.getRoundNum() > roundNumSupply) {
            droneState = DroneState.SUPPLY;
        }
        
        target = enemyHQLocation;
        // TODO waypoint system has a bug, drones try to move to offmap location at the start
        //Waypoints.refreshLocalCache();
        //target = Waypoints.waypoints[0];
    }
    
    
    private static void loop() throws GameActionException {
        updateMyLocation();

        //waypointTimeout--;
        // rc.setIndicatorString(1, "Waypoint timeout " + waypointTimeout + " " + indexInWaypoints + " " + Waypoints.numberOfWaypoints
        //        + " x: " + target.x + "y: " + target.y);
        
        // Code that runs in every robot (including buildings, excepting missiles)
        sharedLoopCode();
        
        enemiesInSight = rc.senseNearbyRobots(sightRange, enemyTeam);
        numberOfEnemiesInSight = enemiesInSight.length;
        enemies = rc.senseNearbyRobots(attackRange, enemyTeam);

        // TODO: fix waypoint bug
        //setTargetToWayPoints();
        
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
        if (enemies.length > 0) {
            if (rc.isWeaponReady()) {
               priorityAttack(enemies, attackPriorities);
            }
            droneRetreat();
        }
    }
    
    //TODO: switch to swarm only if drone units are nearby
    /**
     * Switches drone state based on number of enemies in sight
     */
    private static void droneSwitchState() {
        
        
        switch (droneState) {
        case UNSWARM:
            checkForDanger();
            /**
            if (rc.senseNearbyRobots(sightRange, Common.myTeam).length >= numberInSwarm) {
                // Switches to swarm state when >4 friendly units within sensing radius.
                droneState = DroneState.SWARM;
            } else 
            **/
            if (numberOfEnemiesInSight > 2) {
             // goes into retreat state if there are enemies in sight range
                droneState = DroneState.RETREAT;
                retreatTimeout = baseRetreatTimeout;
            }
            break;
        /**
        case SWARM:
            if (rc.senseNearbyRobots(sightRange, Common.myTeam).length < numberInSwarm) {
                // switch to unswarm state when <5 friendly units within sensing radius.
                droneState = DroneState.UNSWARM;
            }
            break;
        **/
        case FOLLOW:
            checkForDanger();
            if (numberOfEnemiesInSight == 0 || numberOfEnemiesInSight > 2) {
                // lost sight of follow target or too many enemies
                droneState = DroneState.UNSWARM;
            }
            break;
        case RETREAT:
            checkForDanger();
            /**
            if (rc.senseNearbyRobots(sightRange, Common.myTeam).length >= numberInSwarm) {
                // switch to swarm state when enough friendly units in range
                droneState = DroneState.SWARM;
            } else 
            **/
            if (retreatTimeout < 0) {
                // switch to unswarm state when in stationary retreat for enough turns without encountering any enemy
                droneState = DroneState.UNSWARM;
            }
            break;
        case SUPPLY:
            if (supplyTargetID == HQID) {
                if (rc.getSupplyLevel() > minSupplyLevelOfSupplyDrone) {
//                System.out.println("Choosing supply target..");
                chooseSupplyTarget2();
                supplyTimeout = baseSupplyTimeout;
                }
            } else if (rc.getSupplyLevel() < lowSupplyLevel || supplyTimeout < 0) {
                supplyTargetID = HQID;
            } 
            rc.setIndicatorString(0, "" + supplyTargetID);
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
        switch(droneState) {
        case UNSWARM:
            // defensive state for lone drones, stays away from target and waits for reinforcements.
            /*
            if (keepAwayFromTarget) {
                // target is tower or hq
                if(myLocation.distanceSquaredTo(target) < 35) {
                    return;
                }
            } else {
                if(myLocation.distanceSquaredTo(target) < 25) {
                    return;
                }
            }*/
            checkForEnemies();
            bug(target);
            distributeSupply(suppliabilityMultiplier_Preattack);
            break;
        /**
        case SWARM:
            // aggressive state, bugs toward target
            bug(target);
            break;
        **/
        case FOLLOW:
            checkForEnemies();
            followTarget(enemiesInSight, followPriorities);
            distributeSupply(suppliabilityMultiplier_Preattack);
            break;
        case RETREAT:
            if (numberOfEnemiesInSight == 0) {
                retreatTimeout--;
            } else {
                droneRetreat();
                retreatTimeout = baseRetreatTimeout;
            }
            distributeSupply(suppliabilityMultiplier_Preattack);
            break; 
        case SUPPLY:
            if (supplyTargetID == HQID) {
                // staying near HQ to collect supply
                checkForEnemies();
                supplyTarget = HQLocation;
                moveAndAvoidEnemies(myLocation.directionTo(HQLocation));
                // try to avoid enemies while moving to supplytarget
            } else {
                try {
                    RobotInfo robot = rc.senseRobot(supplyTargetID);
                    supplyTarget = robot.location;
                } catch (GameActionException e) {
                    chooseSupplyTarget2();
                    RobotInfo robot = rc.senseRobot(supplyTargetID);
                    supplyTarget = robot.location;
                }
                rc.setIndicatorString(2, "Supply target: " + supplyTargetID + " at " + supplyTarget);
                moveAndAvoidEnemies(myLocation.directionTo(supplyTarget));
                if (supplyTargetID != HQID && (myLocation.distanceSquaredTo(supplyTarget) < supplyDistributeRadius)) {
                    distributeSupply(suppliabilityMultiplier_Preattack);
                    supplyTimeout--;
                }
            }
            break;
        default:
            throw new IllegalStateException();
        }

        
    }
    
    // Supply state methods ===============================================================
    
    private static int supplyRadius = 64; // radius within which to sense for friendly units for distributing supply
    private static int supplyTargetID; // target to distribute supply at
    private static int HQID;
    private static MapLocation supplyTarget;
    /**
     * Sense locations that are within supplyRadius of each tower (both friendly and enemy),
     * and sums up number of friendly units with supply lower than a threshold. Sets supplyTarget
     * as location with the highest sum.
     */
    private static void chooseSupplyTarget() {
        int numberOfSupplyLessUnits = 0;
        MapLocation supplyTarget;
        for (MapLocation tower: rc.senseTowerLocations()) {
            int supplyDef = 0;
            for (RobotInfo unit: rc.senseNearbyRobots(tower, supplyRadius, myTeam)) {
                int unitType = unit.type.ordinal();
                if (unit.supplyLevel < lowSupply[unitType]) {
                    supplyDef+=supplyPriority[unitType];
                }
            }
            if (supplyDef > numberOfSupplyLessUnits) {
                supplyTarget = tower;
                numberOfSupplyLessUnits = supplyDef;
            }
        }
        for (MapLocation tower: rc.senseEnemyTowerLocations()) {
            int supplyDef = 0;
            for (RobotInfo unit: rc.senseNearbyRobots(tower, supplyRadius, myTeam)) {
                int unitType = unit.type.ordinal();
                if (unit.supplyLevel < lowSupply[unitType]) {
                    supplyDef+=3*supplyPriority[unitType];
                }
            }
            if (supplyDef > numberOfSupplyLessUnits) {
                supplyTarget = tower;
                numberOfSupplyLessUnits = supplyDef;
            }
        }     
        if (numberOfSupplyLessUnits == 0) {
            supplyTarget = HQLocation;
        }
    }
    
    private static void chooseSupplyTarget2() {
        RobotInfo[] friendlyRobots = rc.senseNearbyRobots(9999, myTeam);
        
        int id = Math.max(friendlyRobots.length / 70, 1); // Only examine a
                                                          // maximum of 70
                                                          // robots, evenly
                                                          // sampled
        int i = Clock.getRoundNum() % id; // Randomized start index

        int minSupply = Integer.MAX_VALUE; // number of rounds minSupplyRobot
                                           // can run for without supply
        RobotInfo minSupplyRobot = null;
        while (i < friendlyRobots.length) {
            RobotInfo robot = friendlyRobots[i];
            if (robot.type.needsSupply()) {
                if (robot.supplyLevel / robot.type.supplyUpkeep < minSupply) {
                    minSupplyRobot = robot;
                    minSupply = (int) (robot.supplyLevel / robot.type.supplyUpkeep);
                }
            }
            i += id;
        }
        
        if (minSupplyRobot != null) {
            supplyTargetID = minSupplyRobot.ID;
//            System.out.println("Set supply to " + supplyTargetID + minSupplyRobot.type + " " + minSupplyRobot.location);
        }
    }
    
    /**
     * Move in direction that avoids enemies while favoring input direction.
     * @param dir favored direction
     * @throws GameActionException 
     */
    private static void moveAndAvoidEnemies(Direction dir) throws GameActionException {
        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(myType.sensorRadiusSquared, enemyTeam);
        
        double minDamage = Double.MAX_VALUE;
        Direction bestDirection = null;
        if (nearbyEnemies.length != 0) {
            double currentDamage = 0;
            for (RobotInfo enemy: nearbyEnemies) {
                if (enemy.location.distanceSquaredTo(myLocation) <= enemy.type.attackRadiusSquared)
                    currentDamage += enemy.type.attackPower;
            }
            
            for (int ordinalOffset: ordinalOffsets) {
                Direction newDirection = Common.directions[(dir.ordinal()+ordinalOffset)%8];
                MapLocation newLocation = myLocation.add(newDirection);
                
                double damageForDirection = 0;

                if (!rc.isPathable(myType, newLocation)) {
                    damageForDirection += Double.MAX_VALUE;
                }
                if (rc.senseTerrainTile(newLocation) == TerrainTile.VOID) {
                    damageForDirection += currentDamage;
                }
                for (RobotInfo enemy: nearbyEnemies) {
                    if (enemy.location.distanceSquaredTo(newLocation) <= enemy.type.attackRadiusSquared) {
                        damageForDirection += enemy.type.attackPower;
                    } 
                }
                
                if (damageForDirection == 0) {
                    if (rc.isCoreReady() && movePossible(newDirection)) {
                        rc.move(newDirection);
                    }
                    return;
                } else if (damageForDirection < minDamage){
                    bestDirection = newDirection;
                    minDamage = damageForDirection;
                }
                        
            }
            if (minDamage != Double.MAX_VALUE)
                if (rc.isCoreReady() && movePossible(bestDirection)) {
                    rc.move(bestDirection);
                }
            return;
        } else {
            bug(supplyTarget);
        }
        
    }
    
    
    
    // Retreat or follow methods ===================================================================
    
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
                if (enemiesInSight[0].health <= lowHP[enemyType]) {
                    droneState = DroneState.FOLLOW;
                }else {
                    droneState = DroneState.RETREAT;
                }
            }
        } else {
            for (RobotInfo info: enemiesInSight) {
                int enemyType = info.type.ordinal();
                if (dangerRating[enemyType] == 2) {
                    if (info.health > lowHP[enemyType]) {
                        droneState = DroneState.RETREAT;
                        retreatTimeout = 5;
                        return;
                    }
                }
            }
        }
        
    }
    
    /**
     * Retreat in preference of direction with least enemies
     * Update enemiesInSight before using!
     * @return true if unit was moved by this function, false otherwise
     * @throws GameActionException
     */
    private static boolean droneRetreat() throws GameActionException {
        if (rc.isCoreReady() && enemiesInSight != null && enemiesInSight.length != 0) {
            int[] enemiesInDir = new int[8];
            for (RobotInfo info: enemiesInSight) {
                enemiesInDir[myLocation.directionTo(info.location).ordinal()]+= dangerRating[info.type.ordinal()];
            }
            int minDirScore = 100;
            int dirScore = 0;
            int minIndex = 0;
            for (int i = 0; i < 8; i++) {
                dirScore = enemiesInDir[i] + enemiesInDir[(i+7)%8] + enemiesInDir[(i+1)%8] + enemiesInDir[(i+6)%8] + enemiesInDir[(i+2)%8];
                if (dirScore <= minDirScore && movePossible(directions[i])) {
                        minDirScore = dirScore;
                        minIndex = i;         
                }
            }
            if (movePossible(directions[minIndex])) {
                rc.move(directions[minIndex]);
                return true;
            } else {
                if (enemies.length > 0) {
                    if (rc.isWeaponReady()) {
                        // basicAttack(enemies);
                        priorityAttack(enemies, attackPriorities);
                        return false;
                    }
                }
            }
        }
        return false;
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
        0/*12:COMPUTER*/,   2/*13:SOLDIER*/,   2/*14:BASHER*/,    1/*15:MINER*/,
        2/*16:DRONE*/,     2/*17:TANK*/,      2/*18:COMMANDER*/, 2/*19:LAUNCHER*/,
        2/*20:MISSILE*/
    };
    
    /**
     * Values of hp of respective units such that if the unit has lower hp, then
     * one should attack it.
     */
    private static int[] lowHP = {
        100/*0:HQ*/,         100/*1:TOWER*/,      8/*2:SUPPLYDPT*/,   8/*3:TECHINST*/,
        100/*4:BARRACKS*/,    100/*5:HELIPAD*/,     100/*6:TRNGFIELD*/,   100/*7:TANKFCTRY*/,
        100/*8:MINERFCTRY*/,  8/*9:HNDWSHSTN*/,   100/*10:AEROLAB*/,   30/*11:BEAVER*/,
        8/*12:COMPUTER*/,   16/*13:SOLDIER*/,   8/*14:BASHER*/,    50/*15:MINER*/,
        8/*16:DRONE*/,     8/*17:TANK*/,      8/*18:COMMANDER*/, 8/*19:LAUNCHER*/,
        2/*20:MISSILE*/
    };
    
    
    
    /**
     * Values of supply of respective units such that if the unit has lower supply, then
     * one should distribute supply to it, usually a multiple (10, 20, or 30) of supply upkeep.
     */
    private static int[] lowSupply = {
        -1/*0:HQ*/,         -1/*1:TOWER*/,      -1/*2:SUPPLYDPT*/,   -1/*3:TECHINST*/,
        -1/*4:BARRACKS*/,    -1/*5:HELIPAD*/,     -1/*6:TRNGFIELD*/,   -1/*7:TANKFCTRY*/,
        -1/*8:MINERFCTRY*/,  -1/*9:HNDWSHSTN*/,   -1/*10:AEROLAB*/,   100/*11:BEAVER*/,
        -1/*12:COMPUTER*/,   100/*13:SOLDIER*/,   120/*14:BASHER*/,    160/*15:MINER*/,
        300/*16:DRONE*/,     450/*17:TANK*/,      150/*18:COMMANDER*/, 750/*19:LAUNCHER*/,
        -1/*20:MISSILE*/
    };
    
    /**
     * Priority for supply drone to decide where to go: if unit has a higher priority,
     * supply drone has a higher preference to go there.
     */
    private static int[] supplyPriority = {
        -1/*0:HQ*/,         -1/*1:TOWER*/,      -1/*2:SUPPLYDPT*/,   -1/*3:TECHINST*/,
        -1/*4:BARRACKS*/,    -1/*5:HELIPAD*/,     -1/*6:TRNGFIELD*/,   -1/*7:TANKFCTRY*/,
        -1/*8:MINERFCTRY*/,  -1/*9:HNDWSHSTN*/,   -1/*10:AEROLAB*/,   2/*11:BEAVER*/,
        -1/*12:COMPUTER*/,   4/*13:SOLDIER*/,   5/*14:BASHER*/,    3/*15:MINER*/,
        2/*16:DRONE*/,     8/*17:TANK*/,      8/*18:COMMANDER*/, 10/*19:LAUNCHER*/,
        -1/*20:MISSILE*/
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
        1/*16:DRONE*/,      3/*17:TANK*/,       2/*18:COMMANDER*/,  5/*19:LAUNCHER*/,
        0/*20:MISSILE*/
    };
    
    /**
     * The importance rating that enemy units of each RobotType should be followed 
     * (so higher means follow first).
     */
    private static int[] followPriorities = {
        0/*0:HQ*/,         0/*1:TOWER*/,      0/*2:SUPPLYDPT*/,   0/*3:TECHINST*/,
        8/*4:BARRACKS*/,    8/*5:HELIPAD*/,     8/*6:TRNGFIELD*/,   8/*7:TANKFCTRY*/,
        8/*8:MINERFCTRY*/,  0/*9:HNDWSHSTN*/,   8/*10:AEROLAB*/,   6/*11:BEAVER*/,
        0/*12:COMPUTER*/,   3/*13:SOLDIER*/,   0/*14:BASHER*/,    7/*15:MINER*/,
        3/*16:DRONE*/,     0/*17:TANK*/,      0/*18:COMMANDER*/, 0/*19:LAUNCHER*/,
        8/*20:MISSILE*/
    };


    public static enum DroneState {
        //SWARM, // aggressive mode for drones in a group
        UNSWARM, // defensive mode for lone drones, stays away from target waits for reinforcements
        FOLLOW, // following enemy
        RETREAT, // retreats when enemy is in sight range and then stays still.
        SUPPLY // move back to hq to collect supply and distribute it to other units
    }
}