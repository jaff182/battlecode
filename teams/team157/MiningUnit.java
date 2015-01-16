package team157;

import java.util.Random;

import battlecode.common.*;

public class MiningUnit extends MovableUnit {
    
    
    //Global variables ========================================================
    
    /**
     * Minimum mining rate acceptable.
     */
    public static final double MIN_MINING_RATE = 1;
    
    /**
     * Minimum ore that permits mining rate above MIN_MINING_RATE.
     */
    public static double MIN_ORE_WORTH_MINING = 10;
    
    
    
    //Movement ================================================================
    
    /**
     * Attractive force towards each direction.
     */
    private static double[] attraction;
    
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
    private static final int oreSearchTurnDir = 2*rand.nextInt(2)-1;
    
    /**
     * Hill climb towards more ore.
     */
    public static void goTowardsOre() throws GameActionException {
        if(rc.isCoreReady()) {
            //Initialize variables
            int HCL = 0; //hill climb level
            boolean noPreferredDirFound = false;
            attraction = new double[8];
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
                    if(attraction[dirInt] > 0) {
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
                    if(type == myType) {
                        MapLocation loc = robotInfo.location;
                        int distance = myLocation.distanceSquaredTo(loc);
                        if(distance < 15) {
                            int dirInt = loc.directionTo(myLocation).ordinal();
                            double force = 1000;
                            attraction[dirInt] += force;
                            attraction[(dirInt+1)%8] += force;
                            attraction[(dirInt+7)%8] += force;
                        }
                    }
                }
                
                //Find best ore direction
                for(int dirInt=0; dirInt<8; dirInt++) {
                    if(attraction[dirInt] > 0) {
                        noPreferredDirFound = false;
                        break;
                    }
                }
                
                //No fellow miners around
                if(noPreferredDirFound) {
                    //Skewed dumb bug around
                    for(int i=0; i<=2; i++) {
                        attraction[(oreSearchDirInt+oreSearchTurnDir*i+8)%8] += 8-i;
                    }
                }
            }
            
            //Find most preferred direction
            int[] bestDirInts = {-1,-1,-1,-1,-1,-1,-1,-1};
            int maxCount = 0;
            double bestAttraction = -10000000;
            for(int dirInt=0; dirInt<8; dirInt++) {
                if(rc.canMove(directions[dirInt])) {
                    if(attraction[dirInt] > bestAttraction) {
                        bestAttraction = attraction[dirInt];
                        bestDirInts[0] = dirInt;
                        maxCount = 1;
                    } else if(attraction[dirInt] == bestAttraction) {
                        bestDirInts[maxCount] = dirInt;
                        maxCount++;
                    }
                }
            }
            
            //Move
            if(maxCount > 0) {
                Direction dirToMove = directions[bestDirInts[rand.nextInt(maxCount)]];
                oreSearchDirInt = dirToMove.ordinal();
                rc.move(dirToMove);
            }

        }
    }
    
    /**
     * Helper function for goTowardsOre(). Adds the ore contribution to the index of 
     * the attraction array corresponding to the appropriate relative location. The 
     * location should be within the sensing radius of the robot.
     * @param dx Relative X coordinate
     * @param dy Relative Y coordinate
     */
    private static void countOreInRelativeLocation(int dx, int dy) {
        MapLocation loc = myLocation.add(dx,dy);
        double ore = rc.senseOre(loc);
        if(ore >= MIN_ORE_WORTH_MINING) {
            int dirInt = myLocation.directionTo(loc).ordinal();
            if(rc.isPathable(myType,loc)) attraction[dirInt] += ore;
            else {
                //discourage the direction if obstructed
                attraction[dirInt] -= 20000.0/(dx*dx+dy*dy);
            }
        }
    }
    
}