package team157;

import team157.AttackingUnit.MovableUnitState;
import team157.Utility.Map;
import battlecode.common.*;

public class AttackingGroupUnit extends MovableUnit {
    
    //General methods =========================================================


    private static MapLocation gatherLocation;
    private static MapLocation surroundLocation;
    private static MapLocation defendLocation = HQLocation;
    private static int numberInSwarm = 30;
    private static AttackingGroupState state;
    private static AttackingGroupState previousState; // cannot be attack or retreat state
    private static int gatherRange = 24;
    private static int numberOfEnemyTowers;
    private static MapLocation enemyTarget; //used only in defend state
    private static int defendRadius = distanceBetweenHQs/4;
    
    private static int sensingRange = 35;
    private static final int baseSurroundTimeout = 20;
    private static int surroundTimeout = baseSurroundTimeout;
   
    private static double macroScoringAdvantage = 0;
    private static RobotInfo attackTarget;
    private static MapLocation advanceLocation;
    private static MapLocation retreatLocation;
    
    // Constants to tweak ========================================
    static int macroScoringFriendlyLocationThreshold;
     static double advantageBeforeRetreating;


    enum AttackingGroupState {
        ATTACK, // attack nearby units
        ADVANCE, // advance to enemy location
        SURROUND, // only for surrounding towers/hqs and nothing else
        GATHER, // wait until there are enough reinforcements
        RETREAT // wait or retreat 
    } ;
    
    

    public static void start(int macroScoringFriendlyLocationThreshold, double advantageBeforeRetreating) throws GameActionException {
        AttackingGroupUnit.macroScoringFriendlyLocationThreshold = macroScoringFriendlyLocationThreshold;
        AttackingGroupUnit.advantageBeforeRetreating = advantageBeforeRetreating;
        init();
        while(true) {
            loop();
            rc.yield(); //Yield the round
        }
    }
    
    private static void init() throws GameActionException {
        target = enemyHQLocation;
        numberOfEnemyTowers = enemyTowers.length;
        retreatLocation = HQLocation;
        
        if (numberOfEnemyTowers == 0 && distanceBetweenHQs < 1000) {
            state = AttackingGroupState.SURROUND;
            surroundLocation = enemyHQLocation;
            previousState = state;
            return;
        } else {
            if (rc.senseTowerLocations().length > 0) {
                gatherLocation = getClosestFriendlyTower(enemyHQLocation);
            } else {
                int midX = (2*HQLocation.x + enemyHQLocation.x)/3;
                int midY = (2*HQLocation.y + enemyHQLocation.y)/3;
                gatherLocation = new MapLocation(midX,midY); 
            }
            
            state = AttackingGroupState.GATHER; 
            previousState = state;
        }
    }
    
    
    private static void loop() throws GameActionException {
        // Code that runs in every robot (including buildings, excepting missiles)
        sharedLoopCode();
        
        myLocation = rc.getLocation();
        enemiesInSight = rc.senseNearbyRobots(sightRange, enemyTeam);
        numberOfEnemiesInSight = enemiesInSight.length;
        enemies = rc.senseNearbyRobots(attackRange, enemyTeam);
        
        //TODO: change this??
        if (Clock.getRoundNum() >= roundNumAttack) {
            setTargetToClosestTowerOrHQ();
            advanceLocation = target;
        }
        
        macroScoringAdvantage = macroScoringOfAdvantageInArea(
                rc.senseNearbyRobots(25), macroScoringFriendlyLocationThreshold);


       
        switchState();
        
        attackingGroupMove();
        
        if (Clock.getRoundNum()%10 == 3) {
            setInternalMapAroundTowers();
        } else if (Clock.getRoundNum()%10 == 7) {
            setNextSurroundTarget();
        }
        
        //Display state
        rc.setIndicatorString(1, "In state: " + state + " previous state " + previousState);
        
        numberOfEnemyTowers = enemyTowers.length;

    }
    

    
    
    // Simple attack strategy:
    // first gather near hq until enough units are amassed, then
    // they advance towards the enemy towers and surround them.
    // if enemies are encountered, go into an attack state and
    // attack them. when they reach the towers/hq, go into a surround state
    // where they surround the tower and kill it.
    private static void switchState() throws GameActionException {
        switch(state) {
        case ATTACK:
            if (macroScoringAdvantage < advantageBeforeRetreating) {
                state = AttackingGroupState.RETREAT;
            } else {
                RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(30, Common.enemyTeam);
                if (nearbyEnemies.length != 0) {
                    setAttackTargetToNearestEnemy(nearbyEnemies);
                } else {
                    state = previousState;
                    if (state == AttackingGroupState.SURROUND){
                        surroundTimeout = baseSurroundTimeout;
                    }
                }   
            }
            break;
        case ADVANCE:
            if (macroScoringAdvantage < advantageBeforeRetreating) {
                state = AttackingGroupState.RETREAT;
                previousState = AttackingGroupState.ADVANCE;
            } else {
                RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(30, Common.enemyTeam);
                if (nearbyEnemies.length != 0) {
                    setAttackTargetToNearestEnemy(nearbyEnemies);
                    state = AttackingGroupState.ATTACK;
                    previousState = AttackingGroupState.ADVANCE;
                } else if (myLocation.distanceSquaredTo(surroundLocation) < 50) {
                    state = AttackingGroupState.SURROUND;
                    previousState = AttackingGroupState.ADVANCE;
                    surroundTimeout = baseSurroundTimeout;
                } else if (getNearbyFriendlyAttackers() < numberInSwarm/3) {
                    state = AttackingGroupState.GATHER;
                    previousState = AttackingGroupState.ADVANCE;
                }
            } 
            break;
        case GATHER:
            if (macroScoringAdvantage < advantageBeforeRetreating) {
                state = AttackingGroupState.RETREAT;
                previousState = AttackingGroupState.GATHER;
            } else {
                RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(30, Common.enemyTeam);
                if (nearbyEnemies.length > 0) {
                    setAttackTargetToNearestEnemy(nearbyEnemies);
                    state = AttackingGroupState.ATTACK;
                    previousState = AttackingGroupState.GATHER;
                } else {
                    RobotInfo[] enemiesAroundDefendLocation = rc.senseNearbyRobots(gatherLocation, defendRadius, enemyTeam);
                    if (enemiesAroundDefendLocation.length!=0) {
                        enemyTarget = getNearestEnemy(enemiesAroundDefendLocation).location;
                    } else {
                        enemyTarget = null;
                        if (getNearbyFriendlyAttackers() >= numberInSwarm) {
                            state = AttackingGroupState.ADVANCE;
                            previousState = AttackingGroupState.GATHER;
                            setNextSurroundTarget();
                        }
                    }
                }
            }
            break;
        case SURROUND:
            if (macroScoringAdvantage < advantageBeforeRetreating) {
                state = AttackingGroupState.RETREAT;
                previousState = AttackingGroupState.SURROUND;
            } else if (numberOfEnemiesInSight > 0) {
                setAttackTargetToNearestEnemy(enemiesInSight);
                state = AttackingGroupState.ATTACK;
                previousState = AttackingGroupState.SURROUND;
            } else if (surroundTimeout < 0) {
                state = AttackingGroupState.GATHER;
                previousState = AttackingGroupState.SURROUND;
            } else {
             // check if tower is still alive!
                enemyTowers = rc.senseEnemyTowerLocations();
                boolean targetIsAlive = false;
                if (enemyTowers.length < numberOfEnemyTowers) {
                    for (MapLocation tower: enemyTowers) {
                        if (tower == surroundLocation) {
                            targetIsAlive = true;
                            break;
                        }
                    }
                    if (!targetIsAlive) {
                        //Tower is destroyed!
                        // set area around tower as pathable
                        int towerIndex = getEnemyTowerIndex(surroundLocation);
                        if(towerIndex != -1) {
                            Map.turnOffEnemyTowerRange(towerIndex);
                        }
                        setNextSurroundTarget();
                        state = AttackingGroupState.GATHER;
                        previousState = AttackingGroupState.SURROUND;
                        gatherLocation = surroundLocation;
                    }
                    break;
                }
                if (myLocation.distanceSquaredTo(surroundLocation) <= 24) {
                    if (rc.senseRobotAtLocation(surroundLocation) == null ||
                            rc.senseRobotAtLocation(surroundLocation).team == myTeam) {
                                setNextSurroundTarget();
                                /*
                                state = LauncherState.GATHER;
                                previousState = LauncherState.SURROUND;
                                gatherLocation = surroundLocation;
                                */
                            }
                }
            }
            break;
        case RETREAT:
            if (macroScoringAdvantage >= advantageBeforeRetreating) {
                RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(30, Common.enemyTeam);
                if (nearbyEnemies.length != 0) {
                    setAttackTargetToNearestEnemy(nearbyEnemies);
                    state = AttackingGroupState.ATTACK;
                } else {
                    state = previousState;
                    if (state == AttackingGroupState.SURROUND){
                        surroundTimeout = baseSurroundTimeout;
                    }
                }
            }
            break;
        }
        
    }
    
    
    
    private static void attackingGroupMove() throws GameActionException {
        switch(state) {
        case ATTACK:
            rc.setIndicatorString(2, "State: " + state + " with target "
                    + attackTarget.location);
            attackTarget(macroScoringAdvantage);
            break;
        case ADVANCE:
            bug(surroundLocation);
            break;
        case GATHER:
            if (enemyTarget != null) {
                bug(enemyTarget);
            } else {
                int distanceToGatherLoc = myLocation.distanceSquaredTo(gatherLocation);
                if (distanceToGatherLoc > 2) {
                    bug(gatherLocation);
                }
            }
            break;
        case SURROUND:
            int distanceToSurroundLoc = myLocation.distanceSquaredTo(surroundLocation);
            if (distanceToSurroundLoc < 36 && distanceToSurroundLoc > 24) {
                if (rc.senseNearbyRobots(surroundLocation, 49, myTeam).length < numberInSwarm/4) {
                    surroundTimeout--;
                } else if (rc.senseNearbyRobots(surroundLocation, 36, myTeam).length > 7
                        || rc.senseNearbyRobots(surroundLocation, 24, myTeam).length > 2){
                    if (surroundLocation == enemyHQLocation) {
                        Map.letMeInEnemyHQAttackRegion();
                    } else {
                        int index = Common.getEnemyTowerIndex(surroundLocation);
                        if (index!= -1) {
                            Map.letMeInEnemyTowerRange(index);
                        }
                    } 
                }
            } else {
                bug(surroundLocation);
            }
            break;
        case RETREAT:
            if (myType.cooldownDelay == 0 && rc.isWeaponReady()) {
                MovableUnit.basicAttack(rc.senseNearbyRobots(
                        myType.attackRadiusSquared, Common.enemyTeam));
            }

            if (!MovableUnit.retreat()) {
                bug(retreatLocation);
            }
            break;
        default:
            break;
        }
        
        distributeSupply(suppliabilityMultiplier_Preattack);
    }
    
    
    /**
     * Returns number of friendly launchers in gather range
     * @return
     */
    private static int getNearbyFriendlyAttackers() {
        int numberOfFriendlyAttackers = 0;
        for (RobotInfo info: rc.senseNearbyRobots(gatherRange, Common.myTeam)) {
            if (info.type == RobotType.SOLDIER || info.type == RobotType.TANK){
                numberOfFriendlyAttackers++;
            }
        }
        return numberOfFriendlyAttackers;
    }


    
    // Attacking ==================================================================


    private static void setAttackTargetToNearestEnemy(RobotInfo[] nearbyEnemies)
    {

        RobotInfo nearestEnemy = null;
        int nearestEnemyDistance = Integer.MAX_VALUE;
        for (int i=0; i<nearbyEnemies.length; i++) {
            final int distance = nearbyEnemies[i].location.distanceSquaredTo(myLocation);
            if (nearestEnemyDistance > nearbyEnemies[i].location.distanceSquaredTo(myLocation)) {
                nearestEnemy = nearbyEnemies[i];
                nearestEnemyDistance = distance;
            }
        }
        attackTarget = nearestEnemy;
    }
    
    
    /**
     * Returns nearest enemy or null if there are no nearby enemies
     * @return
     */
    public static RobotInfo getNearestEnemy(RobotInfo[] enemyInfo) {
        RobotInfo nearestEnemy = null;
        int nearestEnemyDistance = Integer.MAX_VALUE;
        for (int i=0; i<enemyInfo.length; i++) {
            int distance = enemyInfo[i].location.distanceSquaredTo(myLocation);
            if (nearestEnemyDistance > distance) {
                nearestEnemy = enemyInfo[i];
                nearestEnemyDistance = distance;
            }
        }
        return nearestEnemy;
    }
    
    private static void attackTarget(double macroScoringAdvantage) throws GameActionException {
        final int distanceToEnemySquared = myLocation.distanceSquaredTo(attackTarget.location);
        final double distanceToEnemy = Math.sqrt(distanceToEnemySquared);
        final double enemyAttackRadius = Math.sqrt(attackTarget.type.attackRadiusSquared);

        final double myCooldownRate;
        if (rc.getSupplyLevel() >= myType.supplyUpkeep) {
            myCooldownRate = 1.0;
        }
        else {
            myCooldownRate = 0.5;
        }

        final double enemyCooldownRate;

        if (attackTarget.supplyLevel >= attackTarget.type.supplyUpkeep) {
            enemyCooldownRate = 1.0;
        }
        else {
            enemyCooldownRate = 0.5;
        }

        if (rc.canAttackLocation(attackTarget.location)) {
            if (rc.isWeaponReady()) {
                rc.attackLocation(attackTarget.location);
            }
            rc.setIndicatorString(1, "Attacking in range");
        } else if (macroScoringAdvantage > 1.5 || !attackTarget.type.canAttack()) {
            rc.setIndicatorString(1, "Advancing to attack");
            bug(attackTarget.location);
        } else if (canShootFasterThanEnemy(distanceToEnemy, enemyAttackRadius, myCooldownRate, enemyCooldownRate)) {
            bug(attackTarget.location);
            rc.setIndicatorString(1, "advancing to attack");
        } else {
            rc.setIndicatorString(1, "No attack indicated, waiting here.");
        }
    }
    
    /**
     * Time until robot can move, close in on enemy, and then shoot it, lower than how long it takes for enemy to
     * shoot you, assuming it's stationary
     * @param distanceToEnemy
     * @param enemyAttackRadius
     * @param myCooldownRate
     * @param enemyCooldownRate
     * @return
     */
    private static boolean canShootFasterThanEnemy(double distanceToEnemy, double enemyAttackRadius, double myCooldownRate,
                                                   double enemyCooldownRate)
    {
        return (myType.movementDelay * (distanceToEnemy - enemyAttackRadius) + myType.loadingDelay) / myCooldownRate
                <= attackTarget.weaponDelay / enemyCooldownRate;
    }
    
    /**
     * Scoring function for advantage in an area.
     * 
     * Computes (where all variables are aggregates over robots of one side)<br>
     * 
     * (your hp)/(average damage dealt over time by enemy) <br>
     * --------------------------------------------------- <br>
     * (enemy hp)/(average damage dealt over time by you)
     * 
     * i.e., in an idealized and very rough scenario, how many times your robots
     * can fight the same battle before giving out.
     * 
     * For launchers, special purpose code is used to check if they can launch.<br>
     * Warning: This function might not be reliable in the presence of missiles.
     * Write special purpose code to execute retreats.
     * 
     * @param robots
     *            all movable robots in an area (excluding HQ and towers)
     * @return a positive score if area is safe, negative if dangerous
     */
    public static double macroScoringOfAdvantageInArea(RobotInfo[] robots, int macroScoringFriendlyDistanceThreshold) {
        double yourHP = rc.getHealth();
        double yourDamageDealtPerUnitTime = myType.attackPower/myType.attackDelay;

        double enemyHP = 0;
        double enemyDamageDealtPerUnitTime = 0;
        
        for (int i = 0; i < robots.length; ++i) {
            final RobotInfo robot = robots[i];
            if (!robot.type.isBuilding && robot.type != RobotType.LAUNCHER && robot.type != RobotType.MISSILE) {
                if (robot.team == Common.myTeam && myLocation.distanceSquaredTo(robot.location) < macroScoringFriendlyLocationThreshold) {
                    yourHP += robot.health;
                    yourDamageDealtPerUnitTime += robot.type.attackPower
                            / robot.type.attackDelay;
                } else if (robot.team != Common.myTeam) {
                    enemyHP += robot.health;
                    enemyDamageDealtPerUnitTime += robot.type.attackPower
                            / robot.type.attackDelay;
                }
            } else if (robot.type == RobotType.LAUNCHER) {
                // Special code for LAUNCHER, since it's attack power is technically 0
                // Roughly perform attack computation over next 3 missile launches (based on missile quantities) and average that
                // TODO: rationalizing choice of 3 (possibly engagement length?)
                double robotDamageDealtPerUnitTime = Math.min(robot.missileCount, 3)*RobotType.MISSILE.attackPower;
                if (robot.missileCount<3) {
                    // (Missile damage)/(Time taken to produce next missile)
                    robotDamageDealtPerUnitTime += RobotType.MISSILE.attackPower/robot.weaponDelay;
                }
                if (robot.team == Common.myTeam && myLocation.distanceSquaredTo(robot.location) < macroScoringFriendlyLocationThreshold) {
                    yourHP += robot.health;
                    yourDamageDealtPerUnitTime += robotDamageDealtPerUnitTime;
                } else if (robot.team != Common.myTeam) {
                    enemyHP += robot.health;
                    enemyDamageDealtPerUnitTime += robotDamageDealtPerUnitTime;
                }
            } else if (robot.type == RobotType.MISSILE){
                // Again, special purpose code for missiles, which only add damage
                if (robot.team == Common.myTeam && myLocation.distanceSquaredTo(robot.location) < macroScoringFriendlyLocationThreshold)
                    yourDamageDealtPerUnitTime += RobotType.MISSILE.attackPower;
                else if (robot.team != Common.myTeam)
                    enemyDamageDealtPerUnitTime += RobotType.MISSILE.attackPower;
            }
        }

        if (enemyHP != 0 && enemyDamageDealtPerUnitTime !=0)
            return ((yourHP/enemyDamageDealtPerUnitTime) / (enemyHP/yourDamageDealtPerUnitTime));
        else 
            return Double.POSITIVE_INFINITY;
    }
    
    //Pathing  ===================================================================
    
    
    /**
     * Returns closest friendly tower to input location.
     * @param loc
     * @return
     */
    private static MapLocation getClosestFriendlyTower(MapLocation loc) {
        myTowers = rc.senseTowerLocations();
        if (myTowers.length > 0) {
            int closestTowerDistance = Integer.MAX_VALUE;
            MapLocation closestTower = myTowers[0];
            for (MapLocation towerLoc: myTowers) {
                int towerDist = towerLoc.distanceSquaredTo(loc);
                if (towerDist < closestTowerDistance) {
                    closestTower = towerLoc;
                    closestTowerDistance = towerDist;
                }
            }
            return closestTower;
        }
        return null;
    }
    
    /**
     * Returns furthest enemy tower from enemy hq
     * check that enemytowers is nonempty before using.
     * @return
     */
    private static MapLocation getFurthestTowerFromEnemyHQ() {
        int furthestTowerDistance = 0;
        MapLocation furthestTower = enemyTowers[0];
        for (MapLocation towerLoc: enemyTowers) {
            int towerDist = towerLoc.distanceSquaredTo(enemyHQLocation);
            if (towerDist > furthestTowerDistance) {
                furthestTower = towerLoc;
                furthestTowerDistance = towerDist;
            }
        }
        return furthestTower;
    }
    
    
    /**
     * Sets next target for launchers to surround
     */
    private static void setNextSurroundTarget() {
        MapLocation[] towerLoc = rc.senseEnemyTowerLocations();
        if ((towerLoc.length > 1 && Clock.getRoundNum() > 300) || towerLoc.length > 0) {
            int distanceToClosestTower = Integer.MAX_VALUE;
            for (MapLocation loc: towerLoc) {
                int towerDist = myLocation.distanceSquaredTo(loc);
                if (towerDist < distanceToClosestTower) {
                    surroundLocation = loc;
                    distanceToClosestTower = towerDist;
                }
            }
        } else {
            surroundLocation = rc.senseEnemyHQLocation();
        }
       
    }
    
    
    
    
    // Parameters =================================================================


    
    private static double[] suppliabilityMultiplier_Preattack = {
        0/*0:HQ*/,          0/*1:TOWER*/,       0/*2:SUPPLYDPT*/,   0/*3:TECHINST*/,
        0/*4:BARRACKS*/,    0/*5:HELIPAD*/,     0/*6:TRNGFIELD*/,   0/*7:TANKFCTRY*/,
        0/*8:MINERFCTRY*/,  0/*9:HNDWSHSTN*/,   0/*10:AEROLAB*/,    1/*11:BEAVER*/,
        0/*12:COMPUTER*/,   1/*13:SOLDIER*/,    1/*14:BASHER*/,     1/*15:MINER*/,
        1/*16:DRONE*/,      3/*17:TANK*/,       2/*18:COMMANDER*/,  5/*19:LAUNCHER*/,
        0/*20:MISSILE*/
    };
    


}