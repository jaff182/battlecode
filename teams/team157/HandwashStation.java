package team157;

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
        
    }
    
    private static void loop() throws GameActionException {
        
        
    }
    
    //Specific methods =========================================================
    

    
    
}