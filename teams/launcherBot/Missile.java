package launcherBot;

import battlecode.common.*;

public class Missile {
    // note: do not extend movable unit! or missile will hit bytecode limit
    
    //General methods =========================================================
    //public static MapLocation targetLocation = null;

    //public static final int CONTACT_RADIUS = 5;
    
    private static int roundsLeft;
    private static RobotInfo[] enemiesInRange;
    private static RobotInfo[] enemiesInAttackRange;
    //private static MapLocation spawnLocation;
    private static MapLocation myLocation;
    private static RobotController rc = RobotPlayer.rc;
    //public final static Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST}; //call directions[i] for the ith direction
    private static Team enemyTeam = rc.getTeam().opponent();
    private static int numberOfEnemiesInAttackRange = 0;
 
    public static void start() throws GameActionException {
        roundsLeft = 5;
        //spawnLocation = rc.getLocation();
        //updateTargetLocation();
        //System.out.println(Clock.getBytecodeNum());
        while(true) {
            loop();
            rc.yield(); //Yield the round
        }
    }
    
    //Specific methods =========================================================
    
    private static void loop() throws GameActionException {
        myLocation = rc.getLocation();
        
        
        if (roundsLeft == 5) {
            
            enemiesInRange = rc.senseNearbyRobots(24, enemyTeam);
            if (enemiesInRange.length != 0) {
                for (RobotInfo info: enemiesInRange) {
                    Direction dir = myLocation.directionTo(info.location);
                    if (rc.canMove(dir)) {
                        if (rc.isCoreReady()) {
                            rc.move(dir);
                            break;
                        }
                    } else if (rc.canMove(dir.rotateLeft())) {
                        if(rc.isCoreReady()) {
                            rc.move(dir.rotateLeft());
                            break;
                        }
                    } else if (rc.canMove(dir.rotateRight())) {
                        if (rc.isCoreReady()) {
                            rc.move(dir.rotateRight());
                            break;
                        }
                    }
                }
            }  
             
        } else {
            enemiesInAttackRange = rc.senseNearbyRobots(2, enemyTeam);
            numberOfEnemiesInAttackRange = enemiesInAttackRange.length;
            
            if (numberOfEnemiesInAttackRange > 4) {
                // explode if too many enemies nearby
                rc.explode();
            } else if (roundsLeft < 3 && numberOfEnemiesInAttackRange > 1) {
                // explode if many enemies nearby and far enough from spawn location
                rc.explode();
            } else if (numberOfEnemiesInAttackRange > 0 && (roundsLeft <2||rc.getHealth()==1)) {
                // explode if too little rounds left to move and can attack enemies
                rc.explode();
            } else {
                enemiesInRange = rc.senseNearbyRobots(24, enemyTeam);
                if (enemiesInRange.length != 0) {
                    // simply micro: path to any enemy in sight
                    for (RobotInfo info: enemiesInRange) {
                        Direction dir = myLocation.directionTo(info.location);
                        if (rc.canMove(dir)) {
                            if (rc.isCoreReady()) {
                                rc.move(dir);
                                break;
                            }
                        } else if (rc.canMove(dir.rotateLeft())) {
                            if(rc.isCoreReady()) {
                                rc.move(dir.rotateLeft());
                                break;
                            }
                        } else if (rc.canMove(dir.rotateRight())) {
                            if (rc.isCoreReady()) {
                                rc.move(dir.rotateRight());
                                break;
                            }
                        }
                    }
                } else {
                    if (roundsLeft == 1) {
                        // suicide if too little moves and no enemies in sight
                        rc.disintegrate();
                    }
                }  
            }
  
        }
        rc.setIndicatorString(1, "Rounds left " + roundsLeft);
        roundsLeft--;
    }

}