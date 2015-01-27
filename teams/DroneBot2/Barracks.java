package DroneBot2;

import java.util.Random;

import DroneBot2.Utility.RobotCount;
import battlecode.common.*;

public class Barracks extends SpawnableStructure {

    public static void start() throws GameActionException {
        init();
        while(true) {
            loop();
            rc.yield(); //Yield the round
        }
    }
    
    private static void init() throws GameActionException {
        
    }

    private static void loop() throws GameActionException {
        // Code that runs in every robot (including buildings, excepting missiles)
        sharedLoopCode();
        
        //Report existence if built because of build order
        claimBuildOrderEntry();

        //Spawn
        //if (RobotCount.read(RobotType.SOLDIER) < 10) {
        //    trySpawn(myLocation.directionTo(enemyHQLocation), RobotType.SOLDIER);
        //}

        //Dispense Supply
        dispenseSupply(suppliabilityMultiplier_Preattack);
    }
    
}