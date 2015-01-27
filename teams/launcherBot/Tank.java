package launcherBot;

import launcherBot.AttackingUnit.MovableUnitState;
import launcherBot.Drone.DroneState;
import launcherBot.Utility.Map;
import launcherBot.Utility.RobotCount;
import launcherBot.Utility.TankDefenseCount;
import launcherBot.Utility.Waypoints;
import battlecode.common.*;

public class Tank extends MovableUnit {
    
    //General methods =========================================================
    
    private static TankState tankState;
    private static int bugTimeout = 15;
    private static int targetAttackRadius = towerAttackRadius;
    private static int numberInSwarm = 5;
    private static int swarmRange = 64;
    private static int distanceBetweenHQs = HQLocation.distanceSquaredTo(enemyHQLocation);
    private static MapLocation retreatLocation = Common.HQLocation;
    private static double macroScoringAdvantage = 0;
    private static int nearbyFriendlyTanks = 0;
    private static int retreatTimeout = 5;
    private static MapLocation gatherLoc = HQLocation.add(HQLocation.directionTo(enemyHQLocation), distanceBetweenHQs/2);
    
    private static RobotInfo attackTarget;
    
    public static void start() throws GameActionException {
        init();
        while(true) {
            loop();
            rc.yield(); //Yield the round
        }
    }
    
    private static void init() throws GameActionException {
        tankState = TankState.GATHER;
        
    }
    
    private static void loop() throws GameActionException {
        myLocation = rc.getLocation();
        
        // Code that runs in every robot (including buildings, excepting missiles)
        sharedLoopCode();
        
        if (Clock.getRoundNum() > roundNumAttack) {
            tankState = TankState.KAMIKAZE;
            if (Clock.getRoundNum() == 1950) {
                Map.resetInternalMap();
            }
        } 
        
        
        enemiesInSight = rc.senseNearbyRobots(sightRange, enemyTeam);
        numberOfEnemiesInSight = enemiesInSight.length;
        enemies = rc.senseNearbyRobots(attackRange, enemyTeam);
        nearbyFriendlyTanks = getNearbyFriendlyTanks();
        
        
        // switch state based on number of enemies in sight
        tankSwitchState(); 
        tankMove();
        
      //Display state
        rc.setIndicatorString(1, "In state: " + tankState);
        
        RobotCount.report();
        
    }
    
    // State methods ==========================================================
    

    /**
     * Switches tank state based on number of enemies in sight
     * @throws GameActionException 
     */
    private static void tankSwitchState() throws GameActionException {
        // the second parameter should be either 10 or 25
        macroScoringAdvantage = AttackingUnit.macroScoringOfAdvantageInArea(rc.senseNearbyRobots(25), 25);
        // State transitions
        if (macroScoringAdvantage<2) {
            tankState = TankState.RETREAT;
            retreatTimeout = 5;
        } else {
            if (numberOfEnemiesInSight != 0) {
                attackTarget = getNearestEnemy();
                tankState = TankState.ATTACK;
            } else {
                tankState = TankState.ADVANCE;
            }

        }
        
        switch (tankState) {
        case GATHER:
            if (nearbyFriendlyTanks >= numberInSwarm) {
                tankState = TankState.ADVANCE;
                setTargetToClosestTowerOrHQ();
            }
            break;
        case ADVANCE:
            if (nearbyFriendlyTanks < 0.7*numberInSwarm) {
                tankState = TankState.GATHER;
            } else if (myLocation.distanceSquaredTo(target) < 64) {
                tankState = TankState.SURROUND;
            }
            break;
        case SURROUND:
            if (rc.senseNearbyRobots(target, 64, myTeam).length > 2) {
                tankState = TankState.SWARM_ATTACK;
            }
            break;
        case KAMIKAZE:
            break;
        case SWARM_ATTACK:
            setTargetToClosestTowerOrHQ();
            if (myLocation.distanceSquaredTo(target) > 100) {
                tankState = TankState.ADVANCE;
            }
            break;
        default:
            break;
        }
    }
    
    /**
     * Attacks or move to target based on tank state.
     * @throws GameActionException
     */
    private static void tankMove() throws GameActionException{
        switch (tankState) {
        case GATHER:
            checkForEnemies();
            bug(gatherLoc);
            /**
            int distanceToHQ = myLocation.distanceSquaredTo(HQLocation);
            if (distanceToHQ < 49 + distanceBetweenHQs/4) {
                if (distanceToHQ > 0.8*distanceBetweenHQs) {
                    return;
                } else {
                    bug(enemyHQLocation);
                }
            } else {
                bug(HQLocation);
            }
            **/
        case SURROUND:
            checkForEnemies();
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
        case KAMIKAZE:
            checkForEnemies();
            bug(target);
            break;
        case SWARM_ATTACK:
            checkForEnemies();
            bug(target);
        case ATTACK:
            attack();
            break;
        case RETREAT:
            if (myType.cooldownDelay == 0 && rc.isWeaponReady())
                MovableUnit.basicAttack(rc.senseNearbyRobots(
                        myType.attackRadiusSquared, Common.enemyTeam));
            if (MovableUnit.retreat())
                bug(retreatLocation);
            break;
        case ADVANCE:
            bug(target);
        default:
            break;
        
        }

        distributeSupply(suppliabilityMultiplier_Preattack);
    }
    
    
    /**
     * Returns number of friendly tanks in swarm range
     * @return
     */
    private static int getNearbyFriendlyTanks() {
        int numberOfFriendlyTanks = 0;
        for (RobotInfo info: rc.senseNearbyRobots(swarmRange, Common.myTeam)) {
            if (info.type == RobotType.TANK){
                numberOfFriendlyTanks++;
            }
        }
        return numberOfFriendlyTanks;
    }
    
    /**
     * Returns nearest enemy or null if there are no nearby enemies
     * @return
     */
    public static RobotInfo getNearestEnemy() {
        if (numberOfEnemiesInSight != 0) {
            RobotInfo nearestEnemy = null;
            int nearestEnemyDistance = Integer.MAX_VALUE;
            for (int i=0; i<enemiesInSight.length; i++) {
                final int distance = enemiesInSight[i].location.distanceSquaredTo(myLocation);
                if (nearestEnemyDistance > enemiesInSight[i].location.distanceSquaredTo(myLocation)) {
                    nearestEnemy = enemiesInSight[i];
                    nearestEnemyDistance = distance;
                }
            }
            return nearestEnemy;
        }
        return null; 
    }
    
    /**
     * Attacks attack target
     * @throws GameActionException 
     */
    public static void attack() throws GameActionException {
        final int distanceToEnemySquared = myLocation.distanceSquaredTo(attackTarget.location);
        final double distanceToEnemy = Math.sqrt(distanceToEnemySquared);
        final double enemyAttackRadius = Math.sqrt(attackTarget.type.attackRadiusSquared);
        
        final double myCooldownRate;
        if (rc.getSupplyLevel() >= myType.supplyUpkeep)
            myCooldownRate = 1.0;
        else
            myCooldownRate = 0.5;
        
        final double enemyCooldownRate;
        if (attackTarget.supplyLevel >= attackTarget.type.supplyUpkeep)
            enemyCooldownRate = 1.0;
        else
            enemyCooldownRate = 0.5;
        
        if (rc.canAttackLocation(attackTarget.location)) {
            if (rc.isWeaponReady())
                    rc.attackLocation(attackTarget.location);
            rc.setIndicatorString(1, "Attacking in range");
        } else if (macroScoringAdvantage > 1.5 || !attackTarget.type.canAttack()) {
            bug(attackTarget.location);
        } else if ((myType.movementDelay
                * (distanceToEnemy - enemyAttackRadius) + myType.loadingDelay)
                / myCooldownRate <= attackTarget.weaponDelay
                / enemyCooldownRate) {
            // Time until robot can move, close in on enemy, and then shoot
            // it, lower than how long it takes for enemy to shoot you
            // assuming it's stationary
            bug(attackTarget.location);
            rc.setIndicatorString(1,
                    "advancing to attack");
        } else
            rc.setIndicatorString(1, "No attack indicated, waiting here.");
    }
    
    // Attack methods =========================================================
    
    /**
     * Checks for enemies in attack range and attacks them. 
     * @throws GameActionException
     */
    private static void checkForEnemies() throws GameActionException {
        // continually attacks when enemies are in attack range.
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
     * Sets target to closest enemy tower, or the enemy hq if there are no towers remaining. 
     * Sets area around the target as pathable in the internal map.
     * @throws GameActionException
     */
    public static void setTargetToTowerOrHQ() throws GameActionException {
        MapLocation[] towerLoc = rc.senseEnemyTowerLocations();
        int distanceToClosestTower = Integer.MAX_VALUE;
        targetAttackRadius = towerAttackRadius;
        if (towerLoc.length != 0) {
            for (MapLocation loc: towerLoc) {
                int towerDist = myLocation.distanceSquaredTo(loc);
                if (towerDist <= distanceToClosestTower) {
                    target = loc;
                    distanceToClosestTower = towerDist;
                }
            }
        } else {
            target = rc.senseEnemyHQLocation();
            targetAttackRadius = HQAttackRadius;
        }
        keepAwayFromTarget= true;
        if (tankState == TankState.KAMIKAZE || (myLocation.distanceSquaredTo(target) < 48 && tankState == TankState.SWARM_ATTACK)){
            setAreaAroundTargetAsPathable();
        }
    }
    
    public static void setAreaAroundTargetAsPathable() throws GameActionException {
        int value = Map.getRadioMap(target.x,target.y);
        for(int i=0; i<6; i++) {
            if(Map.decodeInEnemyTowerRange(value,i) 
                && !Map.isEnemyTowerRangeTurnedOff(i)) {
                    Map.turnOffEnemyTowerRange(i);
                    break;
            }
        }
        keepAwayFromTarget = false;
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


    public static enum TankState {
        GATHER, //gather near HQ
        SURROUND, // aggressive mode for tanks in a group
        ADVANCE, 
        RETREAT,
        ATTACK, // micro attack
        SWARM_ATTACK, // attacking in a swarm
        KAMIKAZE; // all out attack

    }
}