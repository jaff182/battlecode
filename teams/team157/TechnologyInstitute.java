package team157;

import java.util.Random;

import team157.Utility.RobotCount;
import battlecode.common.*;

public class TechnologyInstitute extends SpawnableStructure {
    
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

        //Dispense Supply
        Supply.dispense(suppliabilityMultiplier_Preattack);
    }
}