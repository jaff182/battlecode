package team157;

import team157.AttackingGroupUnit.AttackingGroupState;
import team157.Utility.Map;
import team157.Utility.Supply;
import battlecode.common.*;

public class Launcher extends MovableUnit {
    
    //General methods =========================================================

    public static int missileCount = 0;

    public static final int defaultMissileCount = 5;
    
    private static MapLocation gatherLocation;
    private static MapLocation surroundLocation;
    private static MapLocation defendLocation = HQLocation;
    private static int numberInSwarm = 3;
    private static LauncherState state;
    private static LauncherState previousState;
    private static int gatherRange = 24;
    private static final int baseAttackTimeout = 5;
    private static final int baseRetreatTimeout = 5;
    private static int attackTimeout = baseAttackTimeout;
    private static int retreatTimeout = baseRetreatTimeout;
    private static int[] launchOffsets = new int[]{0,1,-1};
    private static int numberOfEnemyTowers;
    private static MapLocation enemyTarget; //used only in defend state
    private static int defendRadius = distanceBetweenHQs/4;
    
    private static final int sensingRange = 35;

    
    

    enum LauncherState {
        ATTACK, // attack nearby units
        ADVANCE, // advance to enemy location
        SURROUND, // only for surrounding towers/hqs and nothing else
        GATHER, // wait until there are enough reinforcements
        RETREAT, // wait or retreat if no missiles
        DEFEND //defend hq or towers
    } ;
    
    

    public static void start() throws GameActionException {
        init();
        while(true) {
            loop();
            rc.yield(); //Yield the round
        }
    }
    
    private static void init() throws GameActionException {
        target = enemyHQLocation;
        missileCount = 0;
        numberOfEnemyTowers = enemyTowers.length;
        
        if (numberOfEnemyTowers == 0 && distanceBetweenHQs < 1000) {
            state = LauncherState.SURROUND;
            surroundLocation = enemyHQLocation;
            previousState = state;
            return;
        } else {
            int fate = rand.nextInt(3);
            if (fate == 0) {
                int midX = (3*HQLocation.x + enemyHQLocation.x)/4;
                int midY = (3*HQLocation.y + enemyHQLocation.y)/4;
                gatherLocation = new MapLocation(midX,midY); 
            } else if (fate == 1) {
                int midX = HQLocation.x;
                int midY = (3*HQLocation.y + enemyHQLocation.y)/4;
                gatherLocation = new MapLocation(midX,midY);
            } else if (fate == 2) {
                int midX = (3*HQLocation.x + enemyHQLocation.x)/4;
                int midY = HQLocation.y;
                gatherLocation = new MapLocation(midX,midY);
            }
            state = LauncherState.GATHER; 
            previousState = state;
        }
    }
    
    
    private static void loop() throws GameActionException {
        // Code that runs in every robot (including buildings, excepting missiles)
        sharedLoopCode();
        
        myLocation = rc.getLocation();
        enemiesInSight = rc.senseNearbyRobots(sensingRange, enemyTeam);
        numberOfEnemiesInSight = enemiesInSight.length;
        enemies = rc.senseNearbyRobots(attackRange, enemyTeam);
        missileCount = rc.getMissileCount();
       
        switchState();
        
        launcherMove();
        
        if (Clock.getRoundNum()%10 == 3) {
            setInternalMapAroundTowers();
        } else if (Clock.getRoundNum()%10 == 7) {
            setNextSurroundTarget();
        }
        
        //Display state
        rc.setIndicatorString(1, "In state: " + state + " previous state " + previousState);
        
        numberOfEnemyTowers = enemyTowers.length;

    }
    

    
    
    // Simple launcher strategy:
    // launcher first gathers at hq until enough launchers are amassed, then
    // they advance towards the enemy towers and surround them.
    // if enemies are encountered, launchers go into an attack state and
    // attack them. when they reach the towers/hq, they go into a surround state
    // where they surround the tower and kills it.
    private static void switchState() throws GameActionException {
        switch(state) {
        case ATTACK:
            if (missileCount == 0) {
                state = LauncherState.RETREAT;
                retreatTimeout = baseRetreatTimeout;
            } else if (attackTimeout <= 0) {
            // switch back to previous non-attack state 
            // if enough rounds have passed without seeing any enemy
                state = previousState;
                attackTimeout = baseAttackTimeout;
            }
            break;
        case ADVANCE:
            if (missileCount == 0) {
                state = LauncherState.RETREAT;
                previousState = LauncherState.ADVANCE;
                retreatTimeout = baseRetreatTimeout;
            } else if (numberOfEnemiesInSight > 0) {
                state = LauncherState.ATTACK;
                previousState = LauncherState.ADVANCE;
                attackTimeout = baseAttackTimeout;
            } else if (myLocation.distanceSquaredTo(surroundLocation) < 50) {
                state = LauncherState.SURROUND;
                previousState = LauncherState.ADVANCE;
            }
            break;
        case GATHER:
            if (numberOfEnemiesInSight > 0) {
                if (missileCount == 0 ) {
                    state = LauncherState.RETREAT;
                    previousState = LauncherState.GATHER;
                    retreatTimeout = baseRetreatTimeout;
                } else {
                    state = LauncherState.ATTACK;
                    previousState = LauncherState.GATHER;
                    attackTimeout = baseAttackTimeout;
                }
            } else {
                RobotInfo[] enemiesAroundDefendLocation = rc.senseNearbyRobots(gatherLocation, defendRadius, enemyTeam);
                if (enemiesAroundDefendLocation.length!=0) {
                    enemyTarget = getNearestEnemy(enemiesAroundDefendLocation).location;
                } else {
                    enemyTarget = null;
                    if (getNearbyFriendlyLaunchers() >= numberInSwarm
                            && missileCount > 2) {
                        state = LauncherState.ADVANCE;
                        previousState = LauncherState.GATHER;
                        setNextSurroundTarget();
                    }
                }
            }
            break;
        case DEFEND:
            if (numberOfEnemiesInSight > 0) {
                if (missileCount == 0 ) {
                    state = LauncherState.RETREAT;
                    previousState = LauncherState.DEFEND;
                    retreatTimeout = baseRetreatTimeout;
                } else {
                    state = LauncherState.ATTACK;
                    previousState = LauncherState.DEFEND;
                    attackTimeout = baseAttackTimeout;
                }
            } else {
                RobotInfo[] enemiesAroundDefendLocation = rc.senseNearbyRobots(defendLocation, defendRadius, enemyTeam);
                if (enemiesAroundDefendLocation.length!=0) {
                    enemyTarget = getNearestEnemy(enemiesAroundDefendLocation).location;
                } else {
                    enemyTarget = null;
                }
            }
            break;
        case SURROUND:
            if (numberOfEnemiesInSight > 1) { // lone tower
                if (missileCount == 0 ) {
                    state = LauncherState.RETREAT;
                    previousState = LauncherState.SURROUND;
                    retreatTimeout = baseRetreatTimeout;
                } else {
                    state = LauncherState.ATTACK;
                    previousState = LauncherState.SURROUND;
                    attackTimeout = baseAttackTimeout;
                }
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
                        state = LauncherState.GATHER;
                        previousState = LauncherState.SURROUND;
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
            if (missileCount > 2) {
                state = previousState;
                if (state == LauncherState.ATTACK){
                    attackTimeout = baseAttackTimeout;
                }
            } else if (retreatTimeout <= 0) {
            // switch back to previous state 
            // if enough rounds have passed without seeing any enemy
                state = previousState;
                if (state == LauncherState.ATTACK){
                    attackTimeout = baseAttackTimeout;
                }
            }
            break;
        }
        
    }
    
    
    
    private static void launcherMove() throws GameActionException {
        switch(state) {
        case ATTACK:
            // basic attacking micro by spawning missile in direction with most dangerous enemies and least friendly units
            if (numberOfEnemiesInSight != 0) {
                launcherAttack();
                if (rc.isCoreReady()) {
                    retreat();
                }
                attackTimeout = baseAttackTimeout;
            } else {
                // wait if no enemies in sight
                attackTimeout--;
            }
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
        case DEFEND:
            if (enemyTarget!=null) {
                bug(enemyTarget);
            } else {
                int distanceToDefendLoc = myLocation.distanceSquaredTo(defendLocation);
                if (distanceToDefendLoc > 36) {
                    bug(defendLocation);
                }
            }
            break;
        case SURROUND:
            int distanceToSurroundLoc = myLocation.distanceSquaredTo(surroundLocation);
            if (distanceToSurroundLoc < 36 && distanceToSurroundLoc > 24) {
                if (missileCount > 2 || getNearbyFriendlyLaunchersAroundSurroundLocation() > 1) {
                    if (Clock.getRoundNum()%3 == 1) {
                        //synchronize attacks
                        launchInThreeDir(myLocation.directionTo(surroundLocation));
                    }
                    
                } else
                    bug(surroundLocation);
                
            } else {
                bug(surroundLocation);
            }
            break;
        case RETREAT:
            if (numberOfEnemiesInSight!= 0) {
                launcherAttack();
                if (rc.isCoreReady()) {
                    retreat();
                }
            } else {
                if (retreatTimeout > baseRetreatTimeout - 2) {
                    if (rc.isCoreReady()) {
                        retreat();
                    }
                }
                retreatTimeout--;
            }
        default:
            break;
        }
        
        Supply.callForSupply();
        Supply.distribute(suppliabilityMultiplier_Preattack);
    }
    


    
    // Attacking ==================================================================

    
    /**
     * Launch missiles in direction with most enemies and least friends.
     * Use only if number of enemies in sight is nonzero.
     * @throws GameActionException
     */
    public static void launcherAttack() throws GameActionException {
        RobotInfo[] unitsInSight = rc.senseNearbyRobots(24);
        int[] dangerInDir = new int[8];
        for (RobotInfo info: unitsInSight) {
            if (info.team == myTeam) {
                dangerInDir[myLocation.directionTo(info.location).ordinal()]-= friendlyDangerRating[info.type.ordinal()];
            } else {
                dangerInDir[myLocation.directionTo(info.location).ordinal()]+= enemyDangerRating[info.type.ordinal()];
            }
            
        }
        
        int[] dirScores = new int[8];
        for (int i = 0; i < 8; i++) {
            int dirScore = 2*dangerInDir[i] + dangerInDir[(i+7)%8] + dangerInDir[(i+1)%8];
            if (dirScore != 0 && rc.canLaunch(directions[i])) {
                dirScores[i] = dirScore;
            } 
        }
        
        for (int i=0; i < 3; i++) {
            int maxDir = 0;
            int maxScore = 0;
            for (int dirToLaunch=0; dirToLaunch<8;dirToLaunch++) {
                if (dirScores[dirToLaunch] > maxScore) {
                    maxScore = dirScores[dirToLaunch];
                    maxDir = dirToLaunch;
                }
            }
            
            if (maxScore > 0) {
                tryLaunch(directions[maxDir]);
                dirScores[maxDir] = 0;
            } else
                break;
        }
        
        
    }
    
    
    /**
     * Launches three missiles around input direction
     * Used only for attacking surroundLocation!
     * @param dir
     * @throws GameActionException
     */
    public static void launchInThreeDir(Direction dir) throws GameActionException {
        if (rc.canLaunch(dir)) {
            if (myLocation.add(dir).distanceSquaredTo(surroundLocation) <= 24) {
                rc.launchMissile(dir);
            }  
        }
        Direction leftDir = dir.rotateLeft();
        if (rc.canLaunch(leftDir)) {
            if (myLocation.add(leftDir).distanceSquaredTo(surroundLocation) <= 24) {
                rc.launchMissile(leftDir);
            }
        }
        Direction rightDir = dir.rotateRight();
        if (rc.canLaunch(rightDir)) {
            if (myLocation.add(rightDir).distanceSquaredTo(surroundLocation) <= 24) {
                rc.launchMissile(rightDir);
            }
        }
    }
    /**
     * Launch missile in direction dir0 if allowed, transfers supply
     * @param dir0 Direction to spawn at
     * @throws GameActionException
     */
    public static void tryLaunch(Direction dir0) throws GameActionException {
        int dirint0 = dir0.ordinal();
            
        for(int offset : launchOffsets) {
            int dirint = (dirint0+offset+8)%8;
            if(rc.canLaunch(directions[dirint])) {
                rc.launchMissile(directions[dirint]);
                break;
            }
        }
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
     * Returns number of friendly launchers in gather range
     * @return
     */
    private static int getNearbyFriendlyLaunchers() {
        int numberOfFriendlyLaunchers = 0;
        for (RobotInfo info: rc.senseNearbyRobots(gatherRange, Common.myTeam)) {
            if (info.type == RobotType.LAUNCHER){
                numberOfFriendlyLaunchers++;
            }
        }
        return numberOfFriendlyLaunchers;
    }
    
    /**
     * Returns number of friendly launchers in around surround location
     * @return
     */
    private static int getNearbyFriendlyLaunchersAroundSurroundLocation() {
        int numberOfFriendlyLaunchers = 0;
        for (RobotInfo info: rc.senseNearbyRobots(surroundLocation, 36, Common.myTeam)) {
            if (info.type == RobotType.LAUNCHER){
                numberOfFriendlyLaunchers++;
            }
        }
        return numberOfFriendlyLaunchers;
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

    /**
     * Danger rating is higher if launcher prioritize attacking it first
     */
    private static int[] enemyDangerRating = {
        4/*0:HQ*/,         5/*1:TOWER*/,      1/*2:SUPPLYDPT*/,   1/*3:TECHINST*/,
        3/*4:BARRACKS*/,    2/*5:HELIPAD*/,     2/*6:TRNGFIELD*/,   3/*7:TANKFCTRY*/,
        2/*8:MINERFCTRY*/,  1/*9:HNDWSHSTN*/,   3/*10:AEROLAB*/,   2/*11:BEAVER*/,
        1/*12:COMPUTER*/,   2/*13:SOLDIER*/,   2/*14:BASHER*/,    1/*15:MINER*/,
        3/*16:DRONE*/,     4/*17:TANK*/,      5/*18:COMMANDER*/, 5/*19:LAUNCHER*/,
        3/*20:MISSILE*/
    };
    
    /**
     * Danger rating is higher if launcher prioritize launching away from it first
     */
    private static int[] friendlyDangerRating = {
        100/*0:HQ*/,         100/*1:TOWER*/,      1/*2:SUPPLYDPT*/,   1/*3:TECHINST*/,
        3/*4:BARRACKS*/,    2/*5:HELIPAD*/,     2/*6:TRNGFIELD*/,   3/*7:TANKFCTRY*/,
        2/*8:MINERFCTRY*/,  1/*9:HNDWSHSTN*/,   3/*10:AEROLAB*/,   2/*11:BEAVER*/,
        1/*12:COMPUTER*/,   2/*13:SOLDIER*/,   2/*14:BASHER*/,    1/*15:MINER*/,
        3/*16:DRONE*/,     4/*17:TANK*/,      5/*18:COMMANDER*/, 5/*19:LAUNCHER*/,
        2/*20:MISSILE*/
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
     * Danger rating is 3 if one should retreat from it.
     * Danger rating is 2 if one can attack it if it has low hp.
     * Danger rating is 1 if one should follow and attack it.
     * Danger rating is 0 if one should ignore it.
     */
    private static int[] retreatRating = {
        5/*0:HQ*/,         5/*1:TOWER*/,      0/*2:SUPPLYDPT*/,   0/*3:TECHINST*/,
        0/*4:BARRACKS*/,    0/*5:HELIPAD*/,     0/*6:TRNGFIELD*/,   0/*7:TANKFCTRY*/,
        0/*8:MINERFCTRY*/,  0/*9:HNDWSHSTN*/,   0/*10:AEROLAB*/,   1/*11:BEAVER*/,
        0/*12:COMPUTER*/,   1/*13:SOLDIER*/,   2/*14:BASHER*/,    1/*15:MINER*/,
        3/*16:DRONE*/,     5/*17:TANK*/,      5/*18:COMMANDER*/, 5/*19:LAUNCHER*/,
        5/*20:MISSILE*/
    };

    

}