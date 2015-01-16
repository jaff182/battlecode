package team157;

import java.util.Random;
import battlecode.common.*;

public class Soldier extends MovableUnit {
    
    public static SoldierState state = SoldierState.ATTACK_MOVE;
    
    //General methods =========================================================
    
    public static void start() throws GameActionException {
        init();
        while(true) {
            loop();
            rc.yield(); //Yield the round
        }
    }
    
    private static void init() throws GameActionException {
        rc.setIndicatorString(0,"hello i'm a soldier.");
        
    }
    
    private static void loop() throws GameActionException {
        // Code that runs in every robot (including buildings, excepting missiles)
        sharedLoopCode();
        
        // todo refactor
        switch (state) {
        case ATTACK_MOVE:
            
            break;
        case RETREAT:
            break;
        case WAIT:
            break;
        }
    }
    
    //Specific methods =========================================================
    

    
    
}