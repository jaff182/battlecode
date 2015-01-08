package team157;

import java.util.Random;
import battlecode.common.*;

public class Launcher extends MovableUnit {
    
    //General methods =========================================================
    
    public static void start() throws GameActionException {
        init();
        while(true) {
            loop();
            rc.yield(); //Yield the round
        }
    }
    
    private static void init() throws GameActionException {
        rc.setIndicatorString(0,"hello i'm a launcher.");
        
    }
    
    private static void loop() throws GameActionException {
        
        
    }
    
    //Specific methods =========================================================
    
    /**
     * Spawn robot of type rbtype in direction dir0 if allowed, transfers supply
     * @param dir0 Direction to spawn at
     * @param robotType RobotType of robot to spawn
     * @throws GameActionException
     */
    public static void launchMissile(Direction dir0) throws GameActionException {
        if(rc.isCoreReady() && rc.getTeamOre() >= RobotType.MISSILE.oreCost) {
            int dirint0 = dir0.ordinal();
            for(int offset : offsets) {
                int dirint = (dirint0+offset+8)%8;
                if(rc.canSpawn(directions[dirint],RobotType.MISSILE)) {
                    rc.spawn(directions[dirint],RobotType.MISSILE);
                    break;
                }
            }
        }
    }
    
    
}