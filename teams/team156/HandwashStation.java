package team156;

import java.util.Random;
import battlecode.common.*;

public class HandwashStation extends Structure {
    
    //General methods =========================================================
    
    public static void start() throws GameActionException {
        init();
        while(true) {
            loop();
            rc.yield(); //Yield the round
        }
    }
    
    private static void init() throws GameActionException {
        rc.setIndicatorString(0,"hello i'm a handwashstation.");
        
        //Check to see if built because of build order
        checkBuildOrderPosition();
    }
    
    private static void loop() throws GameActionException {
        // Code that runs in every robot (including buildings, excepting missiles)
        sharedLoopCode();
        
        //Report existence if built because of build order
        claimBuildOrderEntry();
    }
    
    //Specific methods =========================================================
    

    
    
}