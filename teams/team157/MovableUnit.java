package team157;

import team157.Utility.*;
import team157.Utility.Map;
import battlecode.common.*;

public class MovableUnit extends Common {
    
    
    // Parameters ===========================================================

    
    // Attacking
    public static final int towerAttackRadius = RobotType.TOWER.attackRadiusSquared;
    public static final int HQ0towerAttackRadius = RobotType.HQ.attackRadiusSquared;
    public static final int HQ2towerAttackRadius = GameConstants.HQ_BUFFED_ATTACK_RADIUS_SQUARED;
    public static final int HQ5towerAttackRadius = 52;//Math.pow(Math.sqrt(GameConstants.HQ_BUFFED_ATTACK_RADIUS_SQUARED)+Math.sqrt(GameConstants.HQ_BUFFED_SPLASH_RADIUS_SQUARED),2);
    public static int HQAttackRadius = HQ2towerAttackRadius;
    public static MapLocation[] previousTowerLocations = enemyTowers;
    
    // TODO: be careful with using this variable - what does "target" mean? should this be in its own class
    public static MapLocation target = Common.enemyHQLocation; //attack target 
    public static int numberOfEnemiesInSight = 0;
    public static RobotInfo[] enemiesInSight;
    public static int indexInWaypoints = 0;
    public static int waypointTimeout = 100; // timeout before waypoint changes
    public static final int roundNumAttack = rc.getRoundLimit() - 200; // round number when end game attack starts
    public static boolean keepAwayFromTarget = false; // true if target is tower or hq, false otherwise
    public static RobotInfo attackTarget; // current attack target
    
    // Robot overall state ====================================================
    // Only modify these variables before and after loop(), at clearly specified locations
    /**
     * State of robot. Some states are invalid in certain robots. Mark clearly which will be used.
     */
    public static RobotState robotState = RobotState.WANDER;
    
    /**
     * If robot is set to move (ADVANCE, RETREAT), this is where it will go
     * TODO: is this a dangerous behaviour?
     */
    public static MapLocation moveTargetLocation = enemyHQLocation;
    
    
    
    /**
     * Wrapper for canMove(). Checks pathability using in order: Internal 
     * map, radio map, direct sensing, isPathable(). Returns true if robot can move to 
     * the input location, return false otherwise.
     * @param dir Target direction
     * @return true if robot can move in dir, false otherwise.
     */
    protected static boolean movePossible(Direction dir) throws GameActionException {
        if(rc.canMove(dir)) {
            MapLocation loc = rc.getLocation().add(dir);
            return Map.checkNotBlocked(loc);
        } else return false;
    }
    
    /**
     * Wrapper for isPathable(). Checks pathability using in order: Internal 
     * map, radio map, direct sensing, isPathable(). Returns true if robot can move to 
     * the input location, return false otherwise.
     * @param loc Target location
     * @return true if robot can move in dir, false otherwise.
     */
    protected static boolean movePossible(MapLocation loc) throws GameActionException {
        return rc.isPathable(myType,loc) && Map.checkNotBlocked(loc);
    }
    
    
    /**
     * Set all MapLocations in attack radius of tower or hq as pathable.
     * @param target MapLocation of tower or hq.
     * @param targetAttackRadiusSquared attack radius squared of target tower or hq.
     */
    public static void setTargetToTowerOrHQ(MapLocation target, int targetAttackRadiusSquared) throws GameActionException {
        int value = Map.getRadioMap(target.x,target.y);
        if(target == enemyHQLocation) {
            //It is the enemy HQ
            Map.letMeInEnemyHQSplashRegion();
            Map.letMeInEnemyHQBuffedRange();
            Map.letMeInEnemyHQBaseRange();
            return;
        }
        //It is an enemy tower
        int towerIndex = getEnemyTowerIndex(target);
        if(towerIndex != -1) {
            Map.letMeInEnemyTowerRange(towerIndex);
        }
    }
    
    /**
     * Sense terrain while moving.
     * @param dir Direction of movement 
     * @throws GameActionException
     */
    public static void moveSense(Direction dir) throws GameActionException {
        rc.move(dir);
        previousDirection = dir;
    }
    
    /**
     * Primitive pathing to target location, with no knowledge of terrain.
     * @param target Destination location
     * @throws GameActionException
     */
    public static void explore(MapLocation target) throws GameActionException {
        if(rc.isCoreReady()) {
            updateMyLocation();

            int dirInt = myLocation.directionTo(target).ordinal();
            int offsetIndex = 0;
            while (offsetIndex < 5 && !movePossible(directions[(dirInt+offsets[offsetIndex]+8)%8])) {
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
     * Primitive pathing in specified direction
     * @param dir Preferred direction
     * @return True if successfully moved.
     * @throws GameActionException
     */
    public static void explore(Direction dir) throws GameActionException {
        if(rc.isCoreReady()) {
            int dirInt = dir.ordinal();
            int offsetIndex = 0;
            while (offsetIndex < 5 && !movePossible(directions[(dirInt+offsets[offsetIndex]+8)%8])) {
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
     * Move around randomly.
     * @throws GameActionException
     */
    public static void wander() throws GameActionException {
        if(rc.isCoreReady()) {
            int dirInt = rand.nextInt(8);
            int offsetIndex = 0;
            while (offsetIndex < 8 && !movePossible(directions[(dirInt+offsets[offsetIndex]+8)%8])) {
                offsetIndex++;
            }
            if (offsetIndex < 8) {
                Direction dirToMove = directions[(dirInt+offsets[offsetIndex]+8)%8];
                rc.move(dirToMove);
                previousDirection = dirToMove;
            }
        }
    }
    

    // Retreating ==============================================================
    
    /**
     * Retreat in preference of direction with least enemies
     * Update enemiesInSight before using!
     * @return true if unit was moved by this function, false otherwise
     * @throws GameActionException
     */
    public static boolean retreat() throws GameActionException {
        if (rc.isCoreReady() && enemiesInSight != null && enemiesInSight.length != 0) {
            int[] enemiesInDir = new int[8];
            for (RobotInfo info: enemiesInSight) {
                enemiesInDir[myLocation.directionTo(info.location).ordinal()]+= retreatRating[info.type.ordinal()];
            }
            for (RobotInfo info: rc.senseNearbyRobots(sightRange, myTeam)) {
                enemiesInDir[myLocation.directionTo(info.location).ordinal()] -= friendlyRetreatRating[info.type.ordinal()];
            }
            int minDirScore = Integer.MAX_VALUE;
            int dirScore = 0;
            int minIndex = 0;
            for (int i = 0; i < 8; i++) {
                dirScore = enemiesInDir[i] + 7*enemiesInDir[(i+7)%8] + 10*enemiesInDir[(i+1)%8] + 7*enemiesInDir[(i+6)%8] + enemiesInDir[(i+2)%8];
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
                    if (rc.isWeaponReady() && Common.myType.cooldownDelay == 0) {
                        // basicAttack(enemies);
                        priorityAttack(enemies, attackPriorities);
                        return false;
                    }
                }
            }
        }
        return false;
    }
    
    public static void setRetreatRating(int[] rating, int[] friendlyRating) {
        retreatRating = rating;
        friendlyRetreatRating = friendlyRating;
    }
    
    
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
    
    /**
     * Danger rating is 3 if one should retreat from it.
     * Danger rating is 2 if one can attack it if it has low hp.
     * Danger rating is 1 if one should follow and attack it.
     * Danger rating is 0 if one should ignore it.
     */
    private static int[] friendlyRetreatRating = {
        5/*0:HQ*/,         5/*1:TOWER*/,      0/*2:SUPPLYDPT*/,   0/*3:TECHINST*/,
        0/*4:BARRACKS*/,    0/*5:HELIPAD*/,     0/*6:TRNGFIELD*/,   0/*7:TANKFCTRY*/,
        0/*8:MINERFCTRY*/,  0/*9:HNDWSHSTN*/,   0/*10:AEROLAB*/,   1/*11:BEAVER*/,
        0/*12:COMPUTER*/,   1/*13:SOLDIER*/,   2/*14:BASHER*/,    1/*15:MINER*/,
        3/*16:DRONE*/,     5/*17:TANK*/,      5/*18:COMMANDER*/, 5/*19:LAUNCHER*/,
        -5/*20:MISSILE*/
    };

    

    
    // Bugging ==================================================================
    
    
    // For pathing
    private static PathingState pathingState = PathingState.BUGGING;
    private static boolean turnClockwise = rand.nextBoolean();
    private static double startDistance; //distance squared before hugging
    private static Direction startTargetDir; //target direction before hugging.
    private static final int noDir = 8;
    private static int[] prohibitedDir = {noDir, noDir};
    private static boolean goneAround = false;
    
    //Previously moved directions
    public static Direction previousDirection = Direction.NONE;
    public static Direction previousPreviousDirection = Direction.NONE;
    
    public static Direction[] intToDirection =
            {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST,
            Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST,
            Direction.NORTH_WEST, Direction.NONE, Direction.OMNI};
    
    private static final boolean[][][] blockedDirs; 
    static {
        blockedDirs = new boolean[10][10][10];
        for (Direction d: Direction.values()) {
            if (d == Direction.NONE || d == Direction.OMNI || d.isDiagonal())
                continue;
            for (Direction b: Direction.values()) {
                // Blocking a dir that is the first prohibited dir, or one
                // rotation to the side
                blockedDirs[d.ordinal()][b.ordinal()][d.ordinal()] = true;
                blockedDirs[d.ordinal()][b.ordinal()][d.rotateLeft().ordinal()] = true;
                blockedDirs[d.ordinal()][b.ordinal()][d.rotateRight().ordinal()] = true;
                // b is diagonal, ignore it
                if (!b.isDiagonal() && b != Direction.NONE && b != Direction.OMNI) {
                    // Blocking a dir that is the second prohibited dir, or one
                    // rotation to the side
                    blockedDirs[d.ordinal()][b.ordinal()][b.ordinal()] = true;
                    blockedDirs[d.ordinal()][b.ordinal()][b.rotateLeft().ordinal()] = true;
                    blockedDirs[d.ordinal()][b.ordinal()][b.rotateRight().ordinal()] = true;
                }
            }
        }
    }
    
    /**
     * Return direction to move in given input Direction.
     * @param targetDir desired direction to move in.
     * @return preferred direction to move in, or NONE if cannot move into 3 forward directions.
     * @throws GameActionException
     */
    private static Direction chooseForwardDir(Direction targetDir) throws GameActionException {
        if (movePossible(targetDir)) {
            return targetDir;
        } else {
            Direction leftDir = targetDir.rotateLeft();
            Direction rightDir = targetDir.rotateRight();
            if (movePossible(leftDir)) {
                return leftDir;
            } else if (movePossible(rightDir)) {
                return rightDir;
            }
            
        }
        return Direction.NONE;
    }
    
    
    
    /**
     * Return direction to bug in.
     * @param target location of target.
     * @return next direction to move in, robot should be able to move in this direction; 
     * returns NONE if no directions possible.
     * @throws GameActionException
     */
    public static Direction bugDirection(MapLocation target) throws GameActionException {
        updateMyLocation();

        Direction targetDir = myLocation.directionTo(target);
        
        if (targetDir == Direction.NONE || targetDir == Direction.OMNI){
            return Direction.NONE;
        }
        
        if (pathingState == PathingState.HUGGING){
            if (myLocation.distanceSquaredTo(target) <= startDistance 
                    && bugMovePossible(targetDir)) {
                        // closer to target than at the start of hugging
                        pathingState = PathingState.BUGGING;
                        prohibitedDir = new int[]{noDir, noDir};
                        goneAround = false;
                    }
        }
        
        switch(pathingState) {
        case BUGGING:
            rc.setIndicatorString(0,"bug " + targetDir);
            Direction forwardDir = chooseForwardDir(targetDir);
            if (forwardDir!= Direction.NONE) {
                return forwardDir;
            }
            pathingState = PathingState.HUGGING;
            startDistance = myLocation.distanceSquaredTo(target);
            startTargetDir = targetDir;
            
        case HUGGING:
            rc.setIndicatorString(0, "HUG " + targetDir);
            if (goneAround && (targetDir == startTargetDir.rotateLeft().rotateLeft() ||
                    targetDir == startTargetDir.rotateRight().rotateRight())) {
                prohibitedDir[0] = noDir;
            }
            if (targetDir == startTargetDir.opposite()) {
                prohibitedDir[0] = noDir;
                goneAround = true;
            }
            Direction nextDir = hug(targetDir, false);
            if (nextDir == Direction.NONE) {
                nextDir = targetDir;
            }
            return nextDir;
        default:
            break;
        }
        return Direction.NONE;
    }
    
    /**
     * Basic bugging around obstacles
     * @param target
     * @throws GameActionException
     */
    public static void bug(MapLocation target) throws GameActionException {
        if (rc.isCoreReady()) {
            Direction nextDir = bugDirection(target);
            if ((nextDir != Direction.NONE) && bugMovePossible(nextDir)) {
                previousDirection = nextDir;
                rc.move(nextDir);
            }
        }
    }
    
    /**
     * Returns rotated direction in clockwise direction if turnClockwise is true,
     * or rotated direction in counter-clockwise direction otherwise.
     * @param dir direction.
     * @return rotated direction.
     */
    private static Direction turn(Direction dir) {
        return (turnClockwise ? dir.rotateRight() : dir.rotateLeft());
    }
    
    /**
     * Helper method to bug, hugs around obstacle.
     * @param targetDir direction of target
     * @param tried true if hug attempted in opposite direction, false otherwise.
     * @return next direction to move in.
     * @throws GameActionException
     */
    private static Direction hug(Direction targetDir, boolean tried) throws GameActionException {    
        if (bugMovePossible(targetDir)) {
            return targetDir;
        }
        Direction tryDir = turn(targetDir);
        MapLocation tryLoc = myLocation.add(tryDir);
        for (int i = 0; i < 8 && !bugMovePossible(tryDir) && !(rc.senseTerrainTile(tryLoc).equals(TerrainTile.OFF_MAP)); i++) {
            tryDir = turn(tryDir);
            tryLoc = myLocation.add(tryDir);
        }
        
        // If the loop failed (found no directions or encountered the map edge)
        if (!bugMovePossible(tryDir) || rc.senseTerrainTile(tryLoc).equals(TerrainTile.OFF_MAP)) {
            turnClockwise = !turnClockwise;
            if (tried) {
             // hugging has been tried in both directions
                if (prohibitedDir[0] != noDir && prohibitedDir[1] != noDir) {
                    // allow prohibited direction
                    prohibitedDir[0] = noDir;
                    prohibitedDir[1] = noDir;
                    return hug(targetDir, false);
                } else if (rc.senseNearbyRobots(8, myTeam).length > 8) {
                    // too many friendly units blocking
                    return Direction.NONE;
                } else {
                    // reset and start over.
                    bugReset();
                    return Direction.NONE;
                }
            }
            // hug the other direction
            return hug(targetDir, true);
        }
        
        // store past two cardinal directions that were used in hugging to make
        // sure that these directions are not used again.
        if (tryDir != previousDirection && !tryDir.isDiagonal()) {
            if (turn(turn(intToDirection[prohibitedDir[0]])) == tryDir) {
                prohibitedDir[0] = tryDir.opposite().ordinal();
                prohibitedDir[1] = noDir;
            } else {
                prohibitedDir[1] = prohibitedDir[0];
                prohibitedDir[0] = tryDir.opposite().ordinal();
            }
        }
        return tryDir;
    }
    
    /**
     * Used as helper method to bug, returns true if robot can move in input direction, 
     * returns false otherwise.
     * @param dir target direction
     * @return true if robot can move in dir, false otherwise.
     */
    private static boolean bugMovePossible(Direction dir) throws GameActionException {
        if (blockedDirs[prohibitedDir[0]][prohibitedDir[1]][dir.ordinal()]) {
            return false;
        }
        return movePossible(dir);
    }
    
    /**
     * Reset states and variables before starting bug.
     */
    public static void bugReset() {
        pathingState = PathingState.BUGGING;
        prohibitedDir = new int[]{noDir, noDir};
        goneAround = false;
    }

    
    
    

    
    // Following =============================================================
    /**
     * Follow target based on priority. Attacks target if in attack range, and moves towards it.
     * @param enemies RobotInfo array of enemies in sensing range
     * @param followOrder int array of follow priority rank for each corresponding RobotType ordinal in robotTypes
     * @throws GameActionException
     */
    public static void followTarget(RobotInfo[] enemies, int[] followOrder) throws GameActionException {
        //Initiate
        int targetidx = 0, targettype = 1;
        double minhp = 100000;
        
        //Check for weakest of highest priority enemy type
        for(int i=0; i<enemies.length; i++) {
            int type = enemies[i].type.ordinal();
            if(followOrder[type] > targettype) {
                //More important enemy to attack
                targettype = followOrder[type];
                minhp = enemies[i].health;
                targetidx = i;
            } else if(followOrder[type] == targettype && enemies[i].health < minhp) {
                //Same priority enemy but lower health
                minhp = enemies[i].health;
                targetidx = i;
            }
        }    
        MapLocation followTargetLoc = enemies[targetidx].location;
        int followTargetDistance = followTargetLoc.distanceSquaredTo(myLocation);
        if (followTargetDistance <= attackRange) {
            if (rc.isWeaponReady()) {
                // basicAttack(enemies);
                rc.attackLocation(followTargetLoc);
            }
            boolean shouldRetreat = shouldRetreat();
            if (followTargetDistance <= 2 && shouldRetreat) {
                retreat();
            } else if (shouldRetreat){
                Direction dirToTarget = myLocation.directionTo(followTargetLoc);
                Direction tryDir = turnTwice(dirToTarget);
                if (movePossible(tryDir) && rc.isCoreReady()) {
                    rc.move(tryDir);
                } else {
                    turnClockwise = !turnClockwise;
                    tryDir = turnTwice(dirToTarget);
                    if (movePossible(tryDir) && rc.isCoreReady()) {
                        rc.move(tryDir);
                    } else {
                        retreat();
                    }
                }
            } 
        } else {
            bug(followTargetLoc);
        }
    }
    
    /**
     * Returns true if robot is building or is computer, and false otherwise.
     * @param robotOrdinal ordinal of robot type.
     * @return true if robot is building or is computer, and false otherwise.
     */
    public static boolean isBuilding(int robotOrdinal) {
        return (robotOrdinal < 11 || robotOrdinal == 12);
    }
    


    /**
     * Returns rotated direction in clockwise direction if turnClockwise is true,
     * or rotated direction in counter-clockwise direction otherwise.
     * @param dir direction.
     * @return rotated direction.
     */
    private static Direction turnTwice(Direction dir) {
        return (turnClockwise ? dir.rotateRight().rotateRight() : dir.rotateLeft().rotateLeft());
    }
    
    
    
    /**
     * Returns true if one should retreat from enemies in sight and 
     * false otherwise, based on danger rating and health of enemy.
     * @return
     */
    private static boolean shouldRetreat() {
        if (numberOfEnemiesInSight == 0) {
            return false;
        } else if (numberOfEnemiesInSight == 1) {
            int enemyType = enemiesInSight[0].type.ordinal();
            int enemyDangerRating = dangerRating[enemyType];
            if (enemyDangerRating == 0 || enemyDangerRating == 1) {
                return false;
            } else if (enemyDangerRating == 2) {
                if (enemiesInSight[0].health < lowHP[enemyType]) {
                    return false;
                }
            }
        }
        return true;
    }

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
        0/*12:COMPUTER*/,   1/*13:SOLDIER*/,   2/*14:BASHER*/,    1/*15:MINER*/,
        2/*16:DRONE*/,     2/*17:TANK*/,      2/*18:COMMANDER*/, 2/*19:LAUNCHER*/,
        2/*20:MISSILE*/
    };
    
    /**
     * Values of hp if respective units such that if the unit has lower hp, then
     * one should attack it.
     */
    private static int[] lowHP = {
        100/*0:HQ*/,         100/*1:TOWER*/,      8/*2:SUPPLYDPT*/,   8/*3:TECHINST*/,
        100/*4:BARRACKS*/,    100/*5:HELIPAD*/,     50/*6:TRNGFIELD*/,   100/*7:TANKFCTRY*/,
        100/*8:MINERFCTRY*/,  8/*9:HNDWSHSTN*/,   100/*10:AEROLAB*/,   40/*11:BEAVER*/,
        8/*12:COMPUTER*/,   20/*13:SOLDIER*/,   20/*14:BASHER*/,    50/*15:MINER*/,
        30/*16:DRONE*/,     40/*17:TANK*/,      40/*18:COMMANDER*/, 40/*19:LAUNCHER*/,
        2/*20:MISSILE*/
    };
    
    // Attacking =============================================================
    
    /**
     * Sets target to closest enemy tower, or the enemy hq if there are no towers remaining. 
     * Sets area around the target as pathable in the internal map.
     * @throws GameActionException
     */
    public static void setTargetToClosestTowerOrHQ() throws GameActionException {
        MapLocation[] towerLoc = rc.senseEnemyTowerLocations();
        int distanceToClosestTower = Integer.MAX_VALUE;
        if (towerLoc.length != 0) {
            for (MapLocation loc: towerLoc) {
                int towerDist = myLocation.distanceSquaredTo(loc);
                if (towerDist <= distanceToClosestTower) {
                    target = loc;
                    distanceToClosestTower = towerDist;
                }
            }
            int towerIndex = getEnemyTowerIndex(target);
            if(towerIndex != -1) {
                Map.letMeInEnemyTowerRange(towerIndex);
            }
        } else {
            target = enemyHQLocation;
            Map.letMeInEnemyHQSplashRegion();
            Map.letMeInEnemyHQBuffedRange();
            Map.letMeInEnemyHQBaseRange();
        }
        
    }

    /**
     * Set target based on waypoints. If round number is greater than cutoff for end game attack, then
     * set target to closest tower or hq (if no towers remaining).
     * @throws GameActionException
     */
    public static void setTargetToWayPoints() throws GameActionException {
        if (Clock.getRoundNum() > roundNumAttack) {
            // end game attack on towers and then hq
            setTargetToClosestTowerOrHQ();
        } else if (waypointTimeout <= 0 ||
                (myLocation.distanceSquaredTo(target) < 24 && numberOfEnemiesInSight == 0)) {
            // switch target to next waypoint if timeout is reached or close to target but no enemies in sight
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
    
    
    // TODO: refactor this method, looks useful
    /**
     * Returns true if current target is an enemy tower or HQ, and false otherwise.
     * @param target current target
     * @return true if current target is enemy tower or HQ, and false otherwise.
     */
    public static boolean targetIsTowerOrHQ(MapLocation target) {
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
     * Set internal map around enemy tower as pathable if tower dies.
     * Must manually update previousTowerLocations at the end of every round to use this.
     */
    public static void setInternalMapAroundTowers() throws GameActionException {
        enemyTowers = rc.senseEnemyTowerLocations();
        int towersDead = previousTowerLocations.length - enemyTowers.length;
        if (towersDead > 0) {
            for (MapLocation tower: previousTowerLocations) {
                if (towersDead <= 0) {
                    break;
                }
                boolean towerAlive = false;
                for (MapLocation otherTower : enemyTowers) {
                    if (tower == otherTower) {
                        towerAlive = true;
                        break;
                    }
                }
                if (!towerAlive) {
                    int towerIndex = getEnemyTowerIndex(tower);
                    if(towerIndex != -1) {
                        Map.turnOffEnemyTowerRange(towerIndex);
                    }
                    towersDead--;
                }
            }
            previousTowerLocations = enemyTowers;
        }
    }
    
    //Move and sense ==========================================================
    
    /**
     * Return map statistics over 6 squares in given direction from input.
     * @param x x coordinate of location in internal map
     * @param y y coordinate of location in internal map
     * @param dir direction from location
     * @return sum of values in internal map in input direction.
     */
    private static int mapStats(int x, int y, Direction dir) {
        int sum = 0;
        switch(dir) {
        case NORTH:
            sum += Map.decodePathStateOrdinal(Map.getInternalMap(x,y-1)) + Map.decodePathStateOrdinal(Map.getInternalMap(x,y-2));
            break;
        case EAST:
            sum += Map.decodePathStateOrdinal(Map.getInternalMap(x+1,y)) + Map.decodePathStateOrdinal(Map.getInternalMap(x+2,y));
            break;
        case SOUTH:
            sum += Map.decodePathStateOrdinal(Map.getInternalMap(x,y+1)) + Map.decodePathStateOrdinal(Map.getInternalMap(x,y+2));
            break;
        case WEST:
            sum += Map.decodePathStateOrdinal(Map.getInternalMap(x-1,y)) + Map.decodePathStateOrdinal(Map.getInternalMap(x-2,y));
            break;
        case NORTH_EAST:
            sum += Map.decodePathStateOrdinal(Map.getInternalMap(x+1,y-1)) + Map.decodePathStateOrdinal(Map.getInternalMap(x+2,y-2));
            break;
        case SOUTH_EAST:
            sum += Map.decodePathStateOrdinal(Map.getInternalMap(x+1,y+1)) + Map.decodePathStateOrdinal(Map.getInternalMap(x+2,y+2));
            break;
        case NORTH_WEST:
            sum += Map.decodePathStateOrdinal(Map.getInternalMap(x-1,y-1)) + Map.decodePathStateOrdinal(Map.getInternalMap(x-2,y-2));
            break;
        case SOUTH_WEST:
            sum += Map.decodePathStateOrdinal(Map.getInternalMap(x-1,y+1)) + Map.decodePathStateOrdinal(Map.getInternalMap(x-2,y+2));
            break;
        default:
            break;
        }
        return sum;
    }
    
    //Sensing variables    
    // Wx = Ny, Ey = Nx, Sx = Nx, Ex = Sy, Wy = Nx
    private final static int[] senseNx = { 0, 1,-1, 2,-2, 3,-3, 4,-4};
    private final static int[] senseNy = {-5,-5,-5,-5,-5,-4,-4,-3,-3};
    private final static int[] senseSy = { 5, 5, 5, 5, 5, 4, 4, 3, 3};
    // NEx = SEy, SEx = SWy, SWx = NWy, NEy = NWx
    private final static int[] senseNWx = {-4,-4,-3,-5,-3,-5,-2,-5,-1,-5, 0,-5, 1};
    private final static int[] senseNWy = {-4,-3,-4,-3,-5,-2,-5,-1,-5, 0,-5, 1,-5};
    private final static int[] senseSEx = { 4, 4, 3, 5, 3, 5, 2, 5, 1, 5, 0, 5,-1};
    private final static int[] senseSEy = { 4, 3, 4, 3, 5, 2, 5, 1, 5, 0, 5,-1, 5};
    
    /**
     * Update internal and radio map after moving once in specified direction.
     * @param robotLoc location of robot
     * @param dir Direction of movement.
     * @throws GameActionException
     */
    public static void senseWhenMove(MapLocation robotLoc, Direction dir) throws GameActionException {
        int i = 0;
        if(dir.ordinal()%2 == 0) {
            while(i < 9 && Clock.getBytecodesLeft() > 450) {
                switch (dir) {
                    // Wx = Ny, Ey = Nx, Sx = Nx, Ex = Sy, Wy = Nx
                    case NORTH:
                        Map.updateOrSense(robotLoc.add(senseNx[i], senseNy[i]));
                        break;
                    case EAST:
                        Map.updateOrSense(robotLoc.add(senseSy[i], senseNx[i]));
                        break;
                    case SOUTH:
                        Map.updateOrSense(robotLoc.add(senseNx[i], senseSy[i]));
                        break;
                    case WEST:
                        Map.updateOrSense(robotLoc.add(senseNy[i], senseNx[i]));
                        break;
                    default: break;
                }
                i++;
            }
        } else {
            while(i < 13 && Clock.getBytecodesLeft() > 450) {
                switch (dir) {
                    // NEx = SEy, SEx = SWy, SWx = NWy, NEy = NWx
                    case NORTH_WEST:
                        Map.updateOrSense(robotLoc.add(senseNWx[i], senseNWy[i]));
                        break;
                    case NORTH_EAST:
                        Map.updateOrSense(robotLoc.add(senseSEy[i], senseNWx[i]));
                        break;
                    case SOUTH_EAST:
                        Map.updateOrSense(robotLoc.add(senseSEx[i], senseSEy[i]));
                        break;
                    case SOUTH_WEST:
                        Map.updateOrSense(robotLoc.add(senseNWy[i], senseSEx[i]));
                        break;
                    default: break;
                }
                i++;
            }
        }
    }
    
    

    /**
     * AF:<br>
     * Represents an attack unit that has state represented by variable state. <br>
     *
     * Three states are available:<br>
     * 1) ATTACKING_UNIT, where the unit moves closer to attack the unit specified by attackTarget<br>
     * 2) RETREATING, where the unit attempts to move to retreatLocation without firing its weapon<br>
     * 3) ADVANCING, where the unit attempts to move towards advanceLocation without firing its weapon<br>
     *
     * RI:<br>
     * When the unit is ATTACKING_UNIT, attackTarget may not be null.<br>
     * Both advanceLocation, state and retreatLocation may never be null.<br>
     *
     * These rep invariants must be satisfied after init() is called.
     */

    // Variables controlling state ==============================
    enum MovableUnitState {
        ATTACKING_UNIT,
        RETREATING,
        ADVANCING
    }

    public static enum PathingState {
        BUGGING, HUGGING;
    }

    /**
     * Reflects the state of a robot. Configure individual behaviours based on this.
     * @author Josiah
     *
     */
    public static enum RobotState {
        ATTACK_MOVE, // Move and attack
        RETREAT, // Retreat
        HOLD_ACTIVE, // Provide supply to enable instantaneous attacking
        WANDER, // Random walk, attack when needed
        MINE, // Mining mode, for beavers, miners
        BUILD, //Building mode, for beavers
    }
}