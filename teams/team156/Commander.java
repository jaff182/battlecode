package team156;

import java.util.Random;
import battlecode.common.*;

public class Commander extends MovableUnit {
    
    //General methods =========================================================
    
    public static void start() throws GameActionException {
        init();
        while(true) {
            loop();
            rc.yield(); //Yield the round
        }
    }
    
    private static void init() throws GameActionException {
        initInternalMap(); //set locations within attack radius of enemy tower or hq as unpathable
        
    }
    
    private static void loop() throws GameActionException {
        // Code that runs in every robot (including buildings, excepting missiles)
        sharedLoopCode();
        
        //Sense nearby units
        enemies = rc.senseNearbyRobots(100000,enemyTeam);
        
        //State machine -------------------------------------------------------
        //Switch state
        switch (robotState) {
            case WANDER: switchStateFromWanderState(); break;
        }
        
        //Display state
        rc.setIndicatorString(1, "In state: " + robotState);

        // Perform action based on state
        switch (robotState) {
            case WANDER: commanderWander(); break;
            default: throw new IllegalStateException();
        }
        
    }
    
    //State switching ==========================================================
    
    /**
     * State transitions from the Wander state.
     */
    private static void switchStateFromWanderState() throws GameActionException {
        if(enemies.length != 0) {
            
        }
    }
    
    //Specific methods =========================================================
    
    /**
     * Wander around.
     */
    private static void commanderWander() throws GameActionException {
        //Wander around
        wander();
        
        rc.setIndicatorString(2,"enemies: "+enemies.length);
    }
    
    
}