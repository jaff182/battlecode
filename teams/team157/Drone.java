package team157;

import java.util.Random;

import battlecode.common.*;

public class Drone extends MovableUnit {
    
    //General methods =========================================================
    
    private static MapLocation myLocation;
    
    public static void start() throws GameActionException {
        init();
        while(true) {
            loop();
            rc.yield(); //Yield the round
        }
    }
    
    private static void init() throws GameActionException {
        rc.setIndicatorString(0,"hello i'm a drone.");
        
    }
    
    
    private static void loop() throws GameActionException {
        
        
    }
    
    private static void switchStateFromUnswarmState() {
        
    }
    
    private static void switchStateFromSwarmState() {
        
    }
    
    //Specific methods =========================================================
    

    
    
}