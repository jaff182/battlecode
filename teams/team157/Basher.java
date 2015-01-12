package team157;

import java.util.Random;
import battlecode.common.*;

public class Basher extends MovableUnit {
    
    //General methods =========================================================
    
    public static void start() throws GameActionException {
        init();
        while(true) {
            loop();
            rc.yield(); //Yield the round
        }
    }
    
    private static void init() throws GameActionException {
        rc.setIndicatorString(0,"hello i'm a basher.");
        
    }
    
    private static void loop() throws GameActionException {
        // Code that runs in every robot (including buildings, excepting missiles)
        sharedLoopCode();
        
    }
    
    //Specific methods =========================================================
    

    
    
}