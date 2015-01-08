package team157;

import java.util.Random;
import battlecode.common.*;

public class Structure extends RobotPlayer {
    
    
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
        if(2000 - Clock.getBytecodeNum() >  500) {
            //Sense nearby friendly robots
            RobotInfo[] friends = rc.senseNearbyRobots(GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED,myTeam);
            int targetidx = -1;
            double totalsupply = 0, totalcapacity = 0;
            double targetsupplyratio = 500, minsupplyratio = targetsupplyratio;
            //targetsupplyratio arbitrarily set to 250 for now
            
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