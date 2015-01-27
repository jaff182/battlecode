package team157;

import battlecode.common.*;
import team157.Common;

public class Supply {
    
    //Global variables ========================================================
    
    /**
     * The minimum number of rounds worth of supply to keep for oneself when deciding 
     * to distributing supply.
     */
    private static final int MIN_ROUNDS_TO_KEEP = 10;
    
    
    //Supply transferring methods =============================================
    
    /**
     * Distribute supply among neighboring units, according to health/supplyUpkeep. 
     * Primarily used by a temporary holder of supply, eg beaver/soldier.
     * @param multiplier Double array of multipliers to supply capacity per unit health of each RobotType.
     * @throws GameActionException
     */
    public static void distribute(double[] multiplier) throws GameActionException {
        if(Clock.getBytecodesLeft() > 1000 
            && RobotPlayer.rc.getSupplyLevel() > MIN_ROUNDS_TO_KEEP*Common.myType.supplyUpkeep) {
                //Sense nearby friendly robots
                RobotInfo[] friends = RobotPlayer.rc.senseNearbyRobots(GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED,Common.myTeam);
                if(friends.length > 0) {
                    //Initiate
                    int targetidx = -1;
                    double myCapacity = RobotPlayer.rc.getHealth()*Common.myType.supplyUpkeep*multiplier[RobotPlayer.rc.getType().ordinal()];
                    double totalSupply = RobotPlayer.rc.getSupplyLevel();
                    double totalCapacity = myCapacity;
                    double minSupplyRatio = Integer.MAX_VALUE; //some big number
                    
                    //Iterate through friends for as long as bytecodes allow
                    for(int i=0; i<friends.length; i++) {
                        //Keep track of total values to find mean later
                        totalSupply += friends[i].supplyLevel;
                        double friendCapacity = friends[i].health*friends[i].type.supplyUpkeep*multiplier[friends[i].type.ordinal()];
                        totalCapacity += friendCapacity;
                        
                        //Find robot with lowest supply per capacity and positive capacity
                        if(friendCapacity > 0) {
                            double supplyRatio = friends[i].supplyLevel/friendCapacity;
                            if(supplyRatio < minSupplyRatio) {
                                minSupplyRatio = supplyRatio;
                                targetidx = i;
                            }
                        }
                        
                        //Stop checking if insufficient bytecode left
                        if(Clock.getBytecodesLeft() < 600) break;
                    }
                    
                    //Transfer any excess supply
                    double myMeanSupply = Math.max(totalSupply/totalCapacity*myCapacity,MIN_ROUNDS_TO_KEEP*Common.myType.supplyUpkeep);
                    if(targetidx != -1 && RobotPlayer.rc.getSupplyLevel() > myMeanSupply) {
                        MapLocation loc = friends[targetidx].location;
                        //Transfer all supply above my mean level amount
                        double transferAmount = (RobotPlayer.rc.getSupplyLevel()-myMeanSupply);
                        RobotPlayer.rc.transferSupplies((int)transferAmount,loc);
                    }
                }
        }
    }
    
    /**
     * Dispense supply to neighboring units according to health/supplyUpkeep. 
     * Primarily used by a large source/storage of supply, eq HQ/building.
     * @param multiplier Double array of multipliers to supply capacity per unit health of each RobotType.
     * @throws GameActionException
     */
    public static void dispense(double[] multiplier) throws GameActionException {
        if(Clock.getBytecodesLeft() > 800
            && RobotPlayer.rc.getSupplyLevel() > MIN_ROUNDS_TO_KEEP*Common.myType.supplyUpkeep) {
                //Sense nearby friendly robots
                RobotInfo[] friends = RobotPlayer.rc.senseNearbyRobots(GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED,Common.myTeam);
                int targetidx = -1;
                double totalSupply = 0, totalCapacity = 0;
                double targetSupplyRatio = 1000, minSupplyRatio = targetSupplyRatio;
                //targetsupplyRatio arbitrarily set to 1000 for now
                
                for(int i=0; i<friends.length; i++) {
                    //Keep track of total values to find mean later
                    totalSupply += friends[i].supplyLevel;
                    double friendCapacity = friends[i].health*multiplier[friends[i].type.ordinal()];
                    totalCapacity += friendCapacity;
                    
                    //Find robot with lowest supply per capacity
                    if(friendCapacity > 0) {
                        double supplyRatio = friends[i].supplyLevel/friendCapacity;
                        if(supplyRatio < minSupplyRatio) {
                            minSupplyRatio = supplyRatio;
                            targetidx = i;
                        }
                    }
                    
                    //Stop checking if insufficient bytecode left
                    if(Clock.getBytecodesLeft() < 600) break;
                }
                
                //Replenish supply of target
                double targetTotalSupply = totalCapacity*targetSupplyRatio;
                if(targetidx != -1 && totalSupply < targetTotalSupply) {
                    MapLocation loc = friends[targetidx].location;
                    RobotPlayer.rc.transferSupplies((int)(targetTotalSupply-totalSupply),loc);
                }
        }
    }
    
    /**
     * Dump most of supply to neighboring units according to health/supplyUpkeep. 
     * Primarily used by a mobile supplier with a large amount of supply, eq drone.
     * Relies on other units' distribution to spread the supply.
     * @param loc Location to dump supply to
     * @param returnTripTime Number of rounds it takes to get back to HQ
     * @throws GameActionException
     */
    public static void dump(MapLocation loc,int returnTripTime) throws GameActionException {
        if(Clock.getBytecodesLeft() > 600) {
            double transferAmount = RobotPlayer.rc.getSupplyLevel() - Common.myType.supplyUpkeep*returnTripTime;
            RobotPlayer.rc.transferSupplies((int)transferAmount,loc);
        }
    }
    
}