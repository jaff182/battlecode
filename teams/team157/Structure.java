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
    
    
}