package team157;

import java.util.Random;

import team157.Utility.Map;
import team157.Utility.RobotCount;
import team157.Utility.TankDefenseCount;
import team157.Utility.Waypoints;
import battlecode.common.*;

public class Tank extends MovableUnit {
    
    //General methods =========================================================
    
    private static MapLocation defendLocation = HQLocation; // location of friendly unit to defend
    private static TankState tankState;
    private static int defendChannel;
    private static int defendRadius = 15;
    private static MapLocation defendPositioning;
    private static int bugTimeout = 15;
    private static int targetAttackRadius = towerAttackRadius;
    private static int numberInSwarm = 5;
    private static int swarmRange = 35;
    private static int distanceBetweenHQs = HQLocation.distanceSquaredTo(enemyHQLocation);
    
    
    public static void start() throws GameActionException {
        init();
        while(true) {
            loop();
            rc.yield(); //Yield the round
        }
    }
    
    private static void init() throws GameActionException {
        if (Clock.getRoundNum() < roundNumAttack) {
            initInternalMap();//set locations within attack radius of enemy tower or hq as unpathable
        }
 
        tankState = TankState.UNSWARM;
    }
    
    private static void loop() throws GameActionException {
        myLocation = rc.getLocation();
        
        // Code that runs in every robot (including buildings, excepting missiles)
        sharedLoopCode();
        
        if (Clock.getRoundNum() > roundNumAttack) {
            tankState = TankState.KAMIKAZE;
        }
        
        enemiesInSight = rc.senseNearbyRobots(sightRange, enemyTeam);
        numberOfEnemiesInSight = enemiesInSight.length;
        enemies = rc.senseNearbyRobots(attackRange, enemyTeam);
        

        // first check if need to switch states
        if (Clock.getRoundNum()%10 == 2) {
            if (tankState == TankState.DEFEND){
                if(!TankDefenseCount.report(defendChannel)) {
                    if (!setDefendTarget()) {
                        setTargetToTowerOrHQ();
                    }
                }
            } else {
                setTargetToTowerOrHQ();
            }
        }
        if (Clock.getRoundNum()%10 == 4) {
            if (tankState == TankState.UNSWARM){
                if (rc.getHealth() > 100) {
                    if (setDefendTarget()) {
                        tankState = TankState.DEFEND;
                        bugTimeout = 15;
                    }
                }
            }
        }
        
        // switch state based on number of enemies in sight
        tankSwitchState(); 
        tankMove();
        
      //Display state
        rc.setIndicatorString(1, "In state: " + tankState + " " + defendLocation.toString() + " " + defendChannel);
        
        RobotCount.report();
        
    }
    
    // State methods ==========================================================
    

    /**
     * Switches tank state based on number of enemies in sight
     */
    private static void tankSwitchState() {
        switch (tankState) {
        case DEFEND:
            break;
        case UNSWARM:
            if (numberOfEnemiesInSight == 1 && !isBuilding(enemiesInSight[0].type.ordinal())) {
                // lone enemy in sight which is not a building
                tankState = TankState.FOLLOW;
            } else {
                // switches to swarm state when >4 friendly tanks in sight range.
                int numberOfFriendlyTanks = 0;
                for (RobotInfo info: rc.senseNearbyRobots(swarmRange, RobotPlayer.myTeam)) {
                    if (info.type == RobotType.TANK){
                        numberOfFriendlyTanks++;
                    }
                }
                if (numberOfFriendlyTanks >= numberInSwarm) {
                    tankState = TankState.SWARM;
                } 
            }
            break;
        case SWARM:
            if (rc.senseNearbyRobots(swarmRange, RobotPlayer.myTeam).length < numberInSwarm) {
             // switch to unswarm state when <3 friendly units within sensing radius.
                tankState = TankState.UNSWARM;
            }
            break;
        case KAMIKAZE:
            break;
        case FOLLOW:
            if (numberOfEnemiesInSight == 0 || numberOfEnemiesInSight > 2) {
                // lost sight of follow target or too many enemies
                tankState = TankState.UNSWARM;
            }
            break;
        default:
            throw new IllegalStateException();
        }
    }
    
    /**
     * Attacks or move to target based on tank state.
     * @throws GameActionException
     */
    private static void tankMove() throws GameActionException{
        // first check for enemies and attacks if there are
        checkForEnemies();

        switch(tankState) {
        case DEFEND:
            int distanceToUnit = myLocation.distanceSquaredTo(defendLocation);
            if (distanceToUnit > defendRadius) {
                //far from unit to defend
                checkForEnemies(); //attack enemies around oneself
                if (rc.isCoreReady()) {
                    bug(defendLocation);
                }
            } else {
                if (!defenseAttack(defendLocation,defendRadius)) {
                    if (myLocation.distanceSquaredTo(defendPositioning) < 5) {
                        return;
                    } else if (Math.abs(distanceToUnit-defendRadius) < 10 && bugTimeout < 0) {
                        return;
                    } else {
                        bug(defendPositioning);
                        bugTimeout--;
                    }
                }
            }
            break;
        case UNSWARM:
            checkForEnemies();
            int distanceToHQ = myLocation.distanceSquaredTo(enemyHQLocation);
            if (distanceToHQ > distanceBetweenHQs/2) {
                if (distanceToHQ <= 0.6*distanceBetweenHQs) {
                    return;
                } else {
                    bug(enemyHQLocation);
                }
            } else {
                bug(HQLocation);
            }
            /**
            // defensive state for lone tanks, stays away from target and waits for reinforcements.
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
            **/
            break;
        case SWARM:
            checkForEnemies();
            // aggressive state, bugs toward target
            bug(target);
            break;
        case KAMIKAZE:
            checkForEnemies();
            bug(target);
            break;
        case FOLLOW:
            checkForEnemies();
            followTarget(enemiesInSight, followPriorities);
            break;
        default:
            throw new IllegalStateException();
        }

        distributeSupply(suppliabilityMultiplier_Preattack);
    }
    
    
    
    
    
    
    //Defense methods =========================================================
    
    /**
     * Sets target to defend according to channels on tank defense count.
     * @return true if defense target is set, false if no need to defend any target.
     * @throws GameActionException
     */
    private static boolean setDefendTarget() throws GameActionException {
        int HQbroadcast= rc.readBroadcast(TankDefenseCount.HQ_CHANNEL);
        if (HQbroadcast > 0 ) {
            rc.broadcast(TankDefenseCount.HQ_CHANNEL, HQbroadcast -1);
            defendLocation = HQLocation;
            defendChannel = TankDefenseCount.HQ_CHANNEL;
            defendPositioning = defendLocation.add(defendLocation.directionTo(enemyHQLocation), defendRadius);
            return true;
        } else {
            int maxTanksNeeded = 0;
            for (int i=0; i<16; i+=3) {
                int tanksNeeded = rc.readBroadcast(TankDefenseCount.TOWER_BASE_CHANNEL + i);
                if (tanksNeeded > maxTanksNeeded) {
                    defendChannel = TankDefenseCount.TOWER_BASE_CHANNEL + i;
                    maxTanksNeeded = tanksNeeded;
                }
            }
            if (maxTanksNeeded > 0) {
                defendLocation = new MapLocation(rc.readBroadcast(defendChannel+1), 
                        rc.readBroadcast(defendChannel+2));
                rc.broadcast(defendChannel, maxTanksNeeded-1);
                defendPositioning = defendLocation.add(defendLocation.directionTo(enemyHQLocation), defendRadius);
                return true;
            }
        }
        return false;
    }
    

    /**
     * TODO: needs fixing, do not use!
     * Chooses best location to defend friendly unit, based on location of other friendly units near it.
     * Should only be called when near friendly unit.
     * @param defendLocation location of friendly unit to defend
     * @param radius radius within which to defend friendly unit
     * @return
     */
    private static MapLocation chooseDefenseLocation(MapLocation defendLocation, int radius) {
        RobotInfo[] friends = rc.senseNearbyRobots(defendLocation, radius, myTeam);
        int[] friendsInDir = new int[8];
        for (RobotInfo info: friends) {
            Direction dir = defendLocation.directionTo(info.location);
            if (dir != Direction.NONE && dir != Direction.OMNI){
                friendsInDir[defendLocation.directionTo(info.location).ordinal()]++;
            }
        }
        int minDirScore = 30;
        int dirScore = 0;
        int minIndex = 0;
        for (int i = 0; i < 8; i++) {
            dirScore = friendsInDir[i] + friendsInDir[(i+7)%8] + friendsInDir[(i+1)%8] + friendsInDir[(i+6)%8] + friendsInDir[(i+2)%8];
            //TODO: account for voids
            if (dirScore <= minDirScore) {
                    minDirScore = dirScore;
                    minIndex = i;         
            }
        }
        return defendLocation.add(directions[minIndex], radius);
    }
    
    
    /**
     * Attack method for defense forces. Attacks enemies within radius of defended friendly unit,
     * or moves closer if enemies are not in range. If there are no enemies within radius of
     * defended unit, checks for enemies in own attack range and attacks them.
     * @param defendLocation Location of friendly unit to defend.
     * @param radius distance squared radius around friendly unit to patrol for enemies.
     * @return true if there are enemies within radius of defended friendly unit, false otherwise.
     * @throws GameActionException
     */
    private static boolean defenseAttack(MapLocation defendLocation, int radius) throws GameActionException {
        RobotInfo[] enemiesToDefendFrom = rc.senseNearbyRobots(defendLocation, radius, enemyTeam);
        if (enemiesToDefendFrom.length > 0) {
            // there are enemies near defended unit
            if (rc.isWeaponReady()) {
                // first check if there are enemies within own attack range near defended unit and attack if there is
                RobotInfo attackTarget = RobotPlayer.chooseDefensePriorityAttackTarget(defendLocation, 
                        enemiesToDefendFrom, defenseAttackPriorities);
                if (attackTarget!=null) {
                    rc.attackLocation(attackTarget.location);
                } else {
                    // enemies are not in range, move towards defended unit to get in range of enemies
                    if (rc.isCoreReady()) {
                        bug(defendLocation);
                    }
                }
            }
            return true;
        } else {
            checkForEnemies();
            return false;
        }
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
        if (myLocation.distanceSquaredTo(target) < 48 && tankState == TankState.SWARM){
            setAreaAroundTargetAsPathable();
        }
    }
    
    public static void setAreaAroundTargetAsPathable() {
        // set area around target as pathable
        int targetID = Map.getInternalMap(target);
        for (MapLocation inSightOfTarget: MapLocation.getAllMapLocationsWithinRadiusSq(target, targetAttackRadius)) {          
            if (Map.getInternalMap(inSightOfTarget) == targetID) {
                Map.setInternalMapWithoutSymmetry(inSightOfTarget, 0);        
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
     * The importance rating that enemy units of each RobotType should be attacked 
     * (so higher means attack first). Needs to be adjusted dynamically based on 
     * defence strategy.
     */
    private static int[] defenseAttackPriorities = {
        20/*0:HQ*/,         21/*1:TOWER*/,      10/*2:SUPPLYDPT*/,   10/*3:TECHINST*/,
        12/*4:BARRACKS*/,    12/*5:HELIPAD*/,     12/*6:TRNGFIELD*/,   13/*7:TANKFCTRY*/,
        11/*8:MINERFCTRY*/,  1/*9:HNDWSHSTN*/,   13/*10:AEROLAB*/,   14/*11:BEAVER*/,
        10/*12:COMPUTER*/,   16/*13:SOLDIER*/,   15/*14:BASHER*/,    13/*15:MINER*/,
        17/*16:DRONE*/,     20/*17:TANK*/,      18/*18:COMMANDER*/, 19/*19:LAUNCHER*/,
        21/*20:MISSILE*/
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