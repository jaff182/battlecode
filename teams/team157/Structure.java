package team157;

import java.util.Random;
import battlecode.common.*;
import team157.Utility.*;

public class Structure extends RobotPlayer {
    
    //Global Variables ========================================================
    
    protected static MapLocation spawnLocation;
    
    
    //Building methods ========================================================
    
    /**
     * The position on the build order that the building is meant to satisfy.
     */
    protected static int buildOrderIndex = -1;
    
    /**
     * Every building runs this in their init code to check if they were built using 
     * the build order, and so need to report their existence every round to claim 
     * their build order entry.
     */
    protected static void checkBuildOrderPosition() throws GameActionException {
        buildOrderIndex = BuildOrder.AmIOnBuildOrder();
        //if(buildOrderIndex != -1) {
        //    System.out.println("I just spawned! My build order is "+buildOrderIndex);
        //}
    }
    
    /**
     * Every building runs this every round to report their existence and claim their 
     * build order entry.
     */
    protected static void claimBuildOrderEntry() throws GameActionException {
        if(buildOrderIndex != -1) {
            BuildOrder.IAmTheBuilding(buildOrderIndex);
        }
    }
    
    
    //Spawning ================================================================
    
    /**
     * Spawn robot of type rbtype in direction dir0 if allowed, transfers supply
     * @param dir0 Direction to spawn at
     * @param robotType RobotType of robot to spawn
     * @throws GameActionException
     */
    public static void trySpawn(Direction dir0, RobotType robotType) throws GameActionException {
        if(rc.isCoreReady() && rc.getTeamOre() >= robotType.oreCost) {
            int dirint0 = dir0.ordinal();
            for(int offset : offsets) {
                int dirint = (dirint0+offset+8)%8;
                if(rc.canSpawn(directions[dirint],robotType)) {
                    rc.spawn(directions[dirint],robotType);
                    break;
                }
            }
        }
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
            double totalsupply = 0, totalcapacity = 0;
            double targetsupplyratio = 1000, minsupplyratio = targetsupplyratio;
            //targetsupplyratio arbitrarily set to 500 for now
            
            for(int i=0; i<friends.length; i++) {
                //Keep track of total values to find mean later
                totalsupply += friends[i].supplyLevel;
                totalcapacity += friends[i].health*multiplier[friends[i].type.ordinal()];
                
                //Find robot with lowest supply per capacity
                double supplyratio = friends[i].supplyLevel/(friends[i].health*multiplier[friends[i].type.ordinal()]);
                if(supplyratio < minsupplyratio) {
                    minsupplyratio = supplyratio;
                    targetidx = i;
                }
                
                if(Clock.getBytecodesLeft() < 600) break;
            }
            
            //Replenish supply
            double targetsupply = totalcapacity*targetsupplyratio;
            if(targetidx != -1 && totalsupply < targetsupply) {
                MapLocation loc = friends[targetidx].location;
                rc.transferSupplies((int)(targetsupply-totalsupply),loc);
            }
        }
    }
    
}