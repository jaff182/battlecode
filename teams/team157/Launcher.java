package team157;

import java.util.Random;
import battlecode.common.*;

public class Launcher extends MovableUnit {
    
    //General methods =========================================================

    public enum LauncherState {
        FIRING, NOT_FIRING;
    }

    public static LauncherState state = LauncherState.NOT_FIRING;

    public static void start() throws GameActionException {
        init();
        while(true) {
            loop();
            rc.yield(); //Yield the round
        }
    }
    
    private static void init() throws GameActionException {
        initInternalMap(); //set locations within attack radius of enemy tower or hq as unpathable
        state = LauncherState.NOT_FIRING;
        target = enemyHQLocation;
    }
    
    private static void loop() throws GameActionException {
        // Code that runs in every robot (including buildings, excepting missiles)
        sharedLoopCode();
        updateMyLocation();
        switch (state) {
            case FIRING:
                Direction dir0 = myLocation.directionTo(target);
                launchMissile(dir0);
                break;
            case NOT_FIRING:
                break;
        }
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
            // TODO: more efficient launching procedures
            for(int offset : offsets) {
                int dirint = (dirint0+offset+8)%8;
                if(rc.canSpawn(directions[dirint],RobotType.MISSILE)) {
                    // Not sure if I can do this
                    Missile.target = target;
                    rc.spawn(directions[dirint],RobotType.MISSILE);
                    break;
                }
            }
        }
    }

    public static void stopFiring()
    {
        state = LauncherState.NOT_FIRING;
    }

    public static void fire(MapLocation location)
    {
        target = location;
        state = LauncherState.FIRING;
    }
    
}