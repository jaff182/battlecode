package team157;

import java.util.Random;

import battlecode.common.*;
import team157.Utility.RobotCount;

public class MiningUnit extends MovableUnit {
    
    
    //Global variables ========================================================
    
    /**
     * Minimum mining rate acceptable.
     */
    public static double MIN_MINING_RATE;
    
    /**
     * Minimum ore that permits mining rate at least MIN_MINING_RATE.
     */
    public static double MIN_ORE_WORTH_MINING;
    
    /**
     * Minimum ore that permits mining rate at least GameConstants.MINIMUM_MINE_AMOUNT.
     */
    public static double MIN_ORE_WORTH_CONSIDERING;
    
    
    
    //Movement ================================================================
    
    /**
     * Attractive force towards each direction.
     */
    private static double[] directionPriority;
    
    //Coordinates for shells of increasing distance (hill climb levels, or HCL)
    //Inner shells have higher priority in deciding the direction to go to mine.
    private static final int[] HCL0X = { 0, 1};
    private static final int[] HCL0Y = { 1, 1};
    //Circular shells
    private static final int[] HCL1X = {-1, 0, 1};
    private static final int[] HCL1Y = { 2, 2, 2};
    private static final int[] HCL2X = {-2,-1, 0, 1, 2, 2};
    private static final int[] HCL2Y = { 3, 3, 3, 3, 3, 2};
    private static final int[] HCL3X = {-2,-1, 0, 1, 2, 3};
    private static final int[] HCL3Y = { 4, 4, 4, 4, 4, 3}; //*/
    /*/Square shells
    private static final int[] HCL1X = {-1, 0, 1, 2};
    private static final int[] HCL1Y = { 2, 2, 2, 2};
    private static final int[] HCL2X = {-2,-1, 0, 1, 2, 3};
    private static final int[] HCL2Y = { 3, 3, 3, 3, 3, 3};
    private static final int[] HCL3X = {-2,-1, 0, 1, 2};
    private static final int[] HCL3Y = { 4, 4, 4, 4, 4}; //*/
    private static final int[][] shellsX = {HCL0X,HCL1X,HCL2X,HCL3X};
    private static final int[][] shellsY = {HCL0Y,HCL1Y,HCL2Y,HCL3Y};
    
    //Ore searching parameters for when no direction is preferred
    private static int oreSearchDirInt = rand.nextInt(8);
    
    /**
     * Hill climb towards more ore.
     */
    public static void goTowardsOre() throws GameActionException {
        if(rc.isCoreReady()) {
            //Initialize variables
            int HCL = 0; //hill climb level
            boolean noPreferredDirFound = true;
            directionPriority = new double[8];
            myLocation = rc.getLocation();
            
            //Add attractive forces from ore around robot
            while(noPreferredDirFound && HCL < 4) {
                //Add ore contributions
                for(int i=0; i<shellsX[HCL].length; i++) {
                    countOreInRelativeLocation(shellsX[HCL][i],shellsY[HCL][i]);
                    countOreInRelativeLocation(-shellsY[HCL][i],shellsX[HCL][i]);
                    countOreInRelativeLocation(-shellsX[HCL][i],-shellsY[HCL][i]);
                    countOreInRelativeLocation(shellsY[HCL][i],-shellsX[HCL][i]);
                }
                
                //Find preferred direction
                for(int dirInt=0; dirInt<8; dirInt++) {
                    if(directionPriority[dirInt] > 0) {
                        noPreferredDirFound = false;
                        break;
                    }
                }
                
                //Go to next hill climb level if no direction is preferred
                HCL++;
            }
            
            //Still no preferred direction
            if(noPreferredDirFound) {
                //Repel other miners
                for(RobotInfo robotInfo : friends) {
                    RobotType type = robotInfo.type;
                    if(type == myType || type == RobotType.HQ) {
                        MapLocation loc = robotInfo.location;
                        int distance = myLocation.distanceSquaredTo(loc);
                        if(distance < 15) {
                            int dirInt = loc.directionTo(myLocation).ordinal();
                            double force = 1000/distance;
                            
                            directionPriority[dirInt] += force;
                            directionPriority[(dirInt+1)%8] += 0.707*force;
                            directionPriority[(dirInt+7)%8] += 0.707*force;
                            directionPriority[(dirInt+3)%8] -= 0.707*force;
                            directionPriority[(dirInt+5)%8] -= 0.707*force;
                            directionPriority[(dirInt+4)%8] -= force;
                        }
                    }
                }
                
                //Check i
                for(int dirInt=0; dirInt<8; dirInt++) {
                    if(directionPriority[dirInt] > 0) {
                        noPreferredDirFound = false;
                        break;
                    }
                }
                
                //No fellow miners around
                if(noPreferredDirFound) {
                    //randomly move around with preference for straighter paths
                    for(int i=-2; i<=2; i++) {
                        directionPriority[(oreSearchDirInt+i+8)%8] += 8-Math.abs(i);
                    }
                }
            }
            
            //Find most preferred direction
            rc.setIndicatorString(2,"dirpri = "+directionPriority[0]+", "+directionPriority[1]+", "+directionPriority[2]+", "+directionPriority[3]+", "+directionPriority[4]+", "+directionPriority[5]+", "+directionPriority[6]+", "+directionPriority[7]);
            int[] bestDirInts = {-1,-1,-1,-1,-1,-1,-1,-1};
            int maxCount = 0;
            double bestdirectionPriority = -10000000;
            for(int dirInt=0; dirInt<8; dirInt++) {
                if(movePossible(directions[dirInt])) {
                    if(directionPriority[dirInt] > bestdirectionPriority) {
                        bestdirectionPriority = directionPriority[dirInt];
                        bestDirInts[0] = dirInt;
                        maxCount = 1;
                    } else if(directionPriority[dirInt] == bestdirectionPriority) {
                        bestDirInts[maxCount] = dirInt;
                        maxCount++;
                    }
                }
            }
            
            //Move
            if(maxCount > 0) {
                Direction dirToMove = directions[bestDirInts[rand.nextInt(maxCount)]];
                oreSearchDirInt = dirToMove.ordinal();
                if (rc.canMove(dirToMove)) {
                    rc.move(dirToMove);
                }
            }
            
        }
    }
    
    /**
     * Helper function for goTowardsOre(). Adds the ore contribution to the index of 
     * the directionPriority array corresponding to the appropriate relative location. The 
     * location should be within the sensing radius of the robot.
     * @param dx Relative X coordinate
     * @param dy Relative Y coordinate
     */
    private static void countOreInRelativeLocation(int dx, int dy) {
        MapLocation loc = myLocation.add(dx,dy);
        int dirInt = myLocation.directionTo(loc).ordinal();
        if(rc.isPathable(myType,loc)) {
            double ore = rc.senseOre(loc);
            if(ore >= MIN_ORE_WORTH_MINING) directionPriority[dirInt] += ore;
        } else {
            //discourage the direction if obstructed
            double force = 1000.0/(dx*dx+dy*dy);
            directionPriority[dirInt] -= force;
        }
    }
    
    
    

}