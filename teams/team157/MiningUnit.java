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
    
    private static MapLocation lastOreLocation;
    private static int lastOreHCL = 4;
    private static int bestOreHCL = -1;
    
    private static int HCL = 0;
    private static boolean noPreferredDirFound = true;
    
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
        if(rc.isCoreReady() || HCL == 0) {
            //Initialize variables
            myLocation = rc.getLocation();
            
            //Add attractive forces from ore around robot
            while(HCL < lastOreHCL && HCL < 4) {
                //Add ore contributions
                countOreInRelativeShell();
                
                //Go to next hill climb level if no direction is preferred
                HCL++;
                
                //Take a break after a few 
                if(HCL == 3 && rc.getSupplyLevel() == 0) break;
            }
            //Reset
            if(!rc.isCoreReady()) {
                return;
            }
            HCL = 0;
            
            //Still no preferred direction
            if(lastOreHCL != 4) {
                int distance = myLocation.distanceSquaredTo(lastOreLocation);
                if(distance <= 2) {
                    //Just go there
                    explore(lastOreLocation);
                    lastOreHCL = 4;
                    bugReset();
                } else {
                    //bug there
                    bug(lastOreLocation);
                }
                
                
            } else {
                //Prefer conserving momentum
                int[] directionPriority = new int[8];
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
    }
    
    /**
     * Helper function for goTowardsOre(). Adds the ore contribution to the index of 
     * the directionPriority array corresponding to the appropriate relative 
     * location. The location should be within the sensing radius of the robot.
     * @param dx Relative X coordinate
     * @param dy Relative Y coordinate
     */
    private static void countOreInRelativeShell() throws GameActionException {
        //Initiate
        double ore, bestOre = minOreWorthMining;
        int dx, dy;
        
        //Iterate through each cell in the shell
        for(int i=0; i<shellsX[HCL].length; i++) {
            //Get relative location
            dx = shellsX[HCL][i];
            dy = shellsY[HCL][i];
            
            //Skip alternate cells in outer 2 shells to save bytecode
            //Unroll loops to save bytecode
            if(HCL < 2 || (dx+dy+Clock.getRoundNum()+10)%2 == 0) {
            
                MapLocation loc = myLocation.add(dx,dy);
                int dirInt = myLocation.directionTo(loc).ordinal();
                if(rc.isPathable(myType,loc)
                    //The following is checkNotBlocked(loc) with no updates,
                    //abstraction thoroughly broken and inlined to save bytecodes
                    && (((4088 & rc.readBroadcast(Map.locationToMapXIndex(loc.x)*130+Map.locationToMapYIndex(loc.y)+Channels.MAP_DATA)) & ~Map.mobLevel) == 0)) {
                        ore = rc.senseOre(loc);
                        if(ore >= bestOre) {
                            lastOreLocation = loc;
                            lastOreHCL = HCL;
                            bestOre = ore;
                            bugReset();
                        }
                }
                
                loc = myLocation.add(-dy,dx);
                dirInt = (dirInt+2)%8;
                if(rc.isPathable(myType,loc)
                    //The following is checkNotBlocked(loc) with no updates,
                    //abstraction thoroughly broken and inlined to save bytecodes
                    && (((4088 & rc.readBroadcast(Map.locationToMapXIndex(loc.x)*130+Map.locationToMapYIndex(loc.y)+Channels.MAP_DATA)) & ~Map.mobLevel) == 0)) {
                        ore = rc.senseOre(loc);
                        if(ore >= bestOre) {
                            lastOreLocation = loc;
                            lastOreHCL = HCL;
                            bestOre = ore;
                            bugReset();
                        }
                }
                
                loc = myLocation.add(-dx,-dy);
                dirInt = (dirInt+2)%8;
                if(rc.isPathable(myType,loc)
                    //The following is checkNotBlocked(loc) with no updates,
                    //abstraction thoroughly broken and inlined to save bytecodes
                    && (((4088 & rc.readBroadcast(Map.locationToMapXIndex(loc.x)*130+Map.locationToMapYIndex(loc.y)+Channels.MAP_DATA)) & ~Map.mobLevel) == 0)) {
                        ore = rc.senseOre(loc);
                        if(ore >= bestOre) {
                            lastOreLocation = loc;
                            lastOreHCL = HCL;
                            bestOre = ore;
                            bugReset();
                        }
                }
                
                loc = myLocation.add(dy,-dx);
                dirInt = (dirInt+2)%8;
                if(rc.isPathable(myType,loc)
                    //The following is checkNotBlocked(loc) with no updates,
                    //abstraction thoroughly broken and inlined to save bytecodes
                    && (((4088 & rc.readBroadcast(Map.locationToMapXIndex(loc.x)*130+Map.locationToMapYIndex(loc.y)+Channels.MAP_DATA)) & ~Map.mobLevel) == 0)) {
                        ore = rc.senseOre(loc);
                        if(ore >= bestOre) {
                            lastOreLocation = loc;
                            lastOreHCL = HCL;
                            bestOre = ore;
                            bugReset();
                        }
                }
                
            }
        }
        
    }
    
    
    

}