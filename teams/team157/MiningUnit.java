package team157;

import java.util.Random;

import battlecode.common.*;
import team157.Utility.RobotCount;
import team157.Utility.Map;

public class MiningUnit extends MovableUnit {
    
    
    //Global variables ========================================================
    
    /**
     * Minimum mining rate acceptable.
     */
    public static double minMiningRate;
    
    /**
     * Minimum ore that permits mining rate at least minMiningRate.
     */
    public static double minOreWorthMining;
    
    /**
     * Minimum ore that permits mining rate at least GameConstants.MINIMUM_MINE_AMOUNT.
     */
    public static double minOreWorthConsidering;
    
    
    
    //Movement ================================================================
    
    /**
     * Priority ratings for going towards each direction.
     */
    private static int[] directionPriority;
    
    //Coordinates for shells of increasing distance (hill climb levels, or HCL)
    //Inner shells have higher priority in deciding the direction to go to mine.
    private static final int[] HCL0X = { 0, 1};
    private static final int[] HCL0Y = { 1, 1};
    private static final int[] HCL1X = {-1, 0, 1, 2};
    private static final int[] HCL1Y = { 2, 2, 2, 2};
    private static final int[] HCL2X = {-2,-1, 0, 1, 2};
    private static final int[] HCL2Y = { 3, 3, 3, 3, 3};
    private static final int[] HCL3X = {-2,-1, 0, 1, 2, 3};
    private static final int[] HCL3Y = { 4, 4, 4, 4, 4, 3};
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
            directionPriority = new int[8];
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
                //Prefer conserving momentum
                int momentum = 1000;
                directionPriority[oreSearchDirInt] += momentum;
                directionPriority[(oreSearchDirInt+4)%8] -= momentum;
                momentum = 707;
                directionPriority[(oreSearchDirInt+1)%8] += momentum; 
                directionPriority[(oreSearchDirInt+7)%8] += momentum;
                directionPriority[(oreSearchDirInt+3)%8] -= momentum;
                directionPriority[(oreSearchDirInt+5)%8] -= momentum;
                
                //Repel other miners
                for(RobotInfo robotInfo : friends) {
                    RobotType type = robotInfo.type;
                    if(type == myType || type == RobotType.HQ) {
                        MapLocation loc = robotInfo.location;
                        int distance = myLocation.distanceSquaredTo(loc);
                        if(distance < 15) {
                            int dirInt = loc.directionTo(myLocation).ordinal();
                            //impulse value chosen so that it matches momentum at 
                            //distance of 9. Miners 3 squares apart going towards 
                            //each other then can bounce in any direction.
                            int impulse = 9*1000/distance;
                            directionPriority[dirInt] += impulse;
                            directionPriority[(dirInt+4)%8] -= impulse;
                            impulse = 9*707/distance;
                            directionPriority[(dirInt+1)%8] += impulse;
                            directionPriority[(dirInt+7)%8] += impulse;
                            directionPriority[(dirInt+3)%8] -= impulse;
                            directionPriority[(dirInt+5)%8] -= impulse;
                        }
                    }
                }
            }
            
            //Find most preferred direction
            int[] bestDirInts = {-1,-1,-1,-1,-1,-1,-1,-1};
            int maxCount = 0;
            int bestdirectionPriority = -10000000;
            for(int dirInt=0; dirInt<8; dirInt++) {
                if(movePossible(directions[dirInt])) {
                    if(directionPriority[dirInt] > bestdirectionPriority) {
                        //Reset list to include new best direction
                        bestdirectionPriority = directionPriority[dirInt];
                        bestDirInts[0] = dirInt;
                        maxCount = 1;
                    } else if(directionPriority[dirInt] == bestdirectionPriority) {
                        //Add direction to list
                        bestDirInts[maxCount] = dirInt;
                        maxCount++;
                    }
                }
            }
            //rc.setIndicatorString(2,"dirpri = "+directionPriority[0]+", "+directionPriority[1]+", "+directionPriority[2]+", "+directionPriority[3]+", "+directionPriority[4]+", "+directionPriority[5]+", "+directionPriority[6]+", "+directionPriority[7]+"; bestDirints = "+bestDirInts[0]+", "+bestDirInts[1]+", "+bestDirInts[2]+", "+bestDirInts[3]+", "+bestDirInts[4]+", "+bestDirInts[5]+", "+bestDirInts[6]+", "+bestDirInts[7]);
            
            //Move
            if(maxCount > 0) {
                Direction dirToMove = directions[bestDirInts[rand.nextInt(maxCount)]];
                oreSearchDirInt = dirToMove.ordinal();
                if(rc.canMove(dirToMove)) {
                    rc.move(dirToMove);
                    previousDirection = dirToMove;
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
    private static void countOreInRelativeLocation(int dx, int dy) throws GameActionException {
        MapLocation loc = myLocation.add(dx,dy);
        int dirInt = myLocation.directionTo(loc).ordinal();
        if(movePossible(loc)) {
            //Add ore contribution (10 times ore amount)
            double ore = rc.senseOre(loc);
            if(ore >= minOreWorthMining) directionPriority[dirInt] += (int)(10*ore);
        } else if(myLocation.distanceSquaredTo(loc) <= 2) {
            //block the direction if obstructed
            //subtract a value much more than typical ore contribution
            directionPriority[dirInt] -= 1000000;
        }
    }
    
    
    

}