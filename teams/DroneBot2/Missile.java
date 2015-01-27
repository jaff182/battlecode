package DroneBot2;

import java.util.Random;

import battlecode.common.*;

public class Missile {
    // note: do not extend movable unit! or missile will hit bytecode limit
    
    //General methods =========================================================
    //public static MapLocation targetLocation = null;

    //public static final int CONTACT_RADIUS = 5;
    
    private static int roundsLeft;
    private static RobotInfo[] enemiesInAttackRange;
    private static RobotInfo[] unitsInSight;
    private static MapLocation myLocation;
    private static RobotController rc = RobotPlayer.rc;
    private static Random rand = new Random(rc.getID()); //seed random number generator
    

    private static Team enemyTeam = rc.getTeam().opponent();
    private static int numberOfEnemiesInAttackRange = 0;
    
    public final static Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST}; //call directions[i] for the ith direction


    public static void start() throws GameActionException {
        roundsLeft = 4;
        while(true) {
            loop();
            rc.yield(); //Yield the round
        }
    }
    

    private static void loop() throws GameActionException {
        myLocation = rc.getLocation();

        
        if (roundsLeft == 4) {
            moveInOptimalDir();
        } else {
            enemiesInAttackRange = rc.senseNearbyRobots(2, enemyTeam);
            numberOfEnemiesInAttackRange = enemiesInAttackRange.length;
            
            if (numberOfEnemiesInAttackRange > 4) {
                // explode if too many enemies nearby
                rc.explode();
            } else if (roundsLeft < 3 && numberOfEnemiesInAttackRange > 1) {
                // explode if many enemies nearby and far enough from spawn location
                rc.explode();
            } else if (numberOfEnemiesInAttackRange > 0 && (roundsLeft < 1 || rc.getHealth() == 1)) {
                //TODO fix roundsLeft
                // explode if too little rounds left to move and can attack enemies
                rc.explode();
            } else if (roundsLeft < 1) {
                rc.disintegrate();
            } else {
                moveInOptimalDir();
            }
        }
        rc.setIndicatorString(1, "Rounds left " + roundsLeft);
        roundsLeft--;
    }
    
    
    
    /**
     * Moves missile in optimal direction taking into account number of 
     * enemy units in sight range.
     * @throws GameActionException
     */
    private static void moveInOptimalDir() throws GameActionException {
        unitsInSight = rc.senseNearbyRobots((roundsLeft+2)*(roundsLeft+2), enemyTeam);
        int numberOfUnitsInSight = unitsInSight.length;
        if (numberOfUnitsInSight != 0) {
            Direction dir = myLocation.directionTo(unitsInSight[rand.nextInt(numberOfUnitsInSight)].location);
            if (rc.isCoreReady()) {
                if (rc.canMove(dir)) {
                    rc.move(dir);
                } else if (rc.canMove(dir.rotateLeft())) {
                    rc.move(dir.rotateLeft());
                } else if (rc.canMove(dir.rotateRight())) {
                    rc.move(dir.rotateRight());
                }
            }
        }
        /**
        if (unitsInSight.length != 0) {
            int[] dangerInDir = new int[8];
            for (RobotInfo info: unitsInSight) {
                dangerInDir[myLocation.directionTo(info.location).ordinal()]++;
            }
            int maxDirScore = 0;
            int dirScore = 0;
            int maxIndex = 0;
            for (int i = 0; i < 8; i++) {
                dirScore = dangerInDir[i];
                if (dirScore >= maxDirScore) {
                        maxDirScore = dirScore;
                        maxIndex = i;         
                }
            }
            Direction dir = directions[maxIndex];
            if (rc.isCoreReady()) {
                if (rc.canMove(dir)) {
                    rc.move(dir);
                } else if (rc.canMove(dir.rotateLeft())) {
                    rc.move(dir.rotateLeft());
                } else if (rc.canMove(dir.rotateRight())) {
                    rc.move(dir.rotateRight());
                }
            }
        } 
        **/
        
    }
    

}