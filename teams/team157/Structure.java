package team157;

import java.util.Random;
import battlecode.common.*;
import team157.Utility.*;

public class Structure extends Common {
    
    //Global Variables ========================================================
    
    protected static MapLocation spawnLocation;
    
    protected static int buildOrderIndex;
    
    //Building methods ========================================================
    
    /**
     * Every building runs this every round to report their existence and claim their 
     * build order entry.
     */
    protected static void claimBuildOrderEntry() throws GameActionException {
        if(buildOrderIndex != -1) {
            buildOrderIndex = BuildOrder.AmIOnBuildOrder(rc.getID());
            if(buildOrderIndex != -1) {
                BuildOrder.IAmTheBuilding(buildOrderIndex);
            }
        }
    }
    
    
    //Spawning ================================================================
    
    /**
     * Spawn robot of type rbtype in direction dir0 if allowed, transfers supply
     * @param dir0 Direction to spawn at
     * @param robotType RobotType of robot to spawn
     * @return true if the robot is spawned, false otherwise
     * @throws GameActionException
     */
    public static boolean trySpawn(Direction dir0, RobotType robotType) throws GameActionException {
        if(rc.isCoreReady() && rc.getTeamOre() >= robotType.oreCost) {
            int dirint0 = dir0.ordinal();
            for(int offset : offsets) {
                int dirint = (dirint0+offset+8)%8;
                if(rc.canSpawn(directions[dirint],robotType)) {
                    rc.spawn(directions[dirint],robotType);
                    return true;
                }
            }
        }
        return false;
    }
    
    
    //Dispense Supply =========================================================
    
    /**
     * Dispense supply to neighboring units according to health/supplyUpkeep. 
     * Primarily used by a large source/storage of supply, eq HQ/building.
     * @param multiplier Double array of multipliers to supply capacity per unit health of each RobotType.
     * @throws GameActionException
     */
    public static void dispenseSupply(double[] multiplier) throws GameActionException {
        if(Clock.getBytecodesLeft() > 800) {
            //Sense nearby friendly robots
            RobotInfo[] friends = rc.senseNearbyRobots(GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED,myTeam);
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
                rc.transferSupplies((int)(targetTotalSupply-totalSupply),loc);
            }
        }
    }
    
}