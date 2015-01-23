package team157;

import java.util.Random;

import battlecode.common.*;
import team157.Utility.Measure;

public class Missile {
    // note: do not extende movable unit! or missile will hit bytecode limit
    
    //General methods =========================================================
    public static MapLocation targetLocation = null;

    public static final int CONTACT_RADIUS = 5;
    
    private static int roundsLeft;
    private static RobotInfo[] enemiesInRange;
    private static RobotInfo[] enemiesInAttackRange;
    private static MapLocation spawnLocation;
    private static MapLocation myLocation;
    private static RobotController rc = RobotPlayer.rc;
    public final static Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST}; //call directions[i] for the ith direction
    private static Team enemyTeam = rc.getTeam().opponent();
 
    public static void start() throws GameActionException {
        roundsLeft = 5;
        spawnLocation = myLocation;
        //updateTargetLocation();
        while(true) {
            loop();
            rc.yield(); //Yield the round
        }
    }
    
    private static void init() throws GameActionException {
        roundsLeft = 5;
        spawnLocation = myLocation;
    }
    
    //Specific methods =========================================================
    
    private static void loop() throws GameActionException {
        myLocation = rc.getLocation();
        
        enemiesInAttackRange = rc.senseNearbyRobots(2, enemyTeam);
        int numberOfEnemiesInAttackRange = enemiesInAttackRange.length;
        
        if (numberOfEnemiesInAttackRange > 4) {
            // explode if too many enemies nearby
            rc.explode();
        } else if (numberOfEnemiesInAttackRange > 2 && myLocation.distanceSquaredTo(spawnLocation) > 4 ) {
            // explode if many enemies nearby and far enough from spawn location
            rc.explode();
        } else if (numberOfEnemiesInAttackRange > 0 && (roundsLeft <2||rc.getHealth()==1)) {
            // explode if too little rounds left to move and can attack enemies
            rc.explode();
        } else {
            enemiesInRange = rc.senseNearbyRobots(24, enemyTeam);
            int numberOfEnemiesInRange = enemiesInRange.length;
            if (numberOfEnemiesInRange != 0) {
                for (RobotInfo info: enemiesInRange) {
                    Direction dir = myLocation.directionTo(info.location);
                    if (rc.canMove(dir)) {
                        if (rc.isCoreReady()) {
                            rc.move(dir);
                        }
                     break;
                    }
                }
                
                /**
                int[] enemiesInDir = new int[8];
                for (RobotInfo info: enemiesInRange) {
                    enemiesInDir[myLocation.directionTo(info.location).ordinal()]++;
                }
                int maxDirScore = 0;
                int dirScore = 0;
                int maxIndex = 0;
                for (int i = 0; i < 8; i++) {
                    dirScore = enemiesInDir[i] + enemiesInDir[(i+7)%8] + enemiesInDir[(i+1)%8] + enemiesInDir[(i+6)%8] + enemiesInDir[(i+2)%8];
                    if (dirScore >= maxDirScore && rc.canMove(directions[i])) {
                            maxDirScore = dirScore;
                            maxIndex = i;         
                    }
                }
                rc.move(directions[maxIndex]);
                **/
            } else {
                if (roundsLeft == 2) {
                    // suicide if too little moves and no enemies in sight
                    rc.disintegrate();
                }
            }  

            rc.setIndicatorString(1, "Rounds left " + roundsLeft);
            roundsLeft--;
        }
        
        
        /**
        if (isCloseToTargetLocation() || sensedEnemyNearBy())
        {
            rc.explode();
        }
        **/
    }

    private static boolean isCloseToTargetLocation()
    {
        return (targetLocation != null && Measure.distance(targetLocation, myLocation) < CONTACT_RADIUS);
    }

    private static boolean sensedEnemyNearBy()
    {
        MovableUnit.updateEnemyInRange(2);
        return (MovableUnit.enemies.length > 4);
    }

    private static void updateTargetLocation() throws GameActionException
    {
        targetLocation = new MapLocation(rc.readBroadcast(Channels.MAP_SYMMETRY + 1),
                rc.readBroadcast(Channels.MAP_SYMMETRY + 2));
    }
}