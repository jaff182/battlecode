package team157;

import java.util.Random;
import battlecode.common.*;

public class beaver extends RobotPlayer {
    
    //General methods =========================================================
    
    public static void start() throws GameActionException {
        init();
        while(true) {
            loop();
            rc.yield(); //Yield the round
        }
    }
    
    public static void init() throws GameActionException {
        rc.setIndicatorString(0,"hello i'm a beaver.");
        
    }
    
    public static void loop() throws GameActionException {
        
        
    }
    
    //Specific methods =========================================================
    
    
    
}