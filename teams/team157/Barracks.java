package team157;

import java.util.Random;
import battlecode.common.*;

public class Barracks extends Structure {
    
    //General methods =========================================================
    
    public static void start() throws GameActionException {
        init();
        while(true) {
            loop();
            rc.yield(); //Yield the round
        }
    }
    
    private static void init() throws GameActionException {
        rc.setIndicatorString(0,"hello i'm a barracks.");
    }
    
    private static void loop() throws GameActionException {
        // Code that runs in every robot (including buildings, excepting missiles)
        sharedLoopCode();

        //Spawn
        trySpawn(myLocation.directionTo(enemyHQLocation),RobotType.DRONE);


        //Dispense Supply
        dispenseSupply(suppliabilityMultiplier_Preattack);
    }
    
    //Specific methods =========================================================
}