package team157;

import java.util.Random;
import battlecode.common.*;
import team157.Utility.Measure;

public class Missile extends MovableUnit {
    
    //General methods =========================================================
    public static MapLocation targetLocation = null;

    public static final int CONTACT_RADIUS = 5;

    public static void start() throws GameActionException {
        init();
        while(true) {
            loop();
            rc.yield(); //Yield the round
        }
    }
    
    private static void init() throws GameActionException {
        rc.setIndicatorString(0,"hello i'm a missile.");
    }
    
    //Specific methods =========================================================
    
    private static void loop() throws GameActionException {
        updateMyLocation();
        if (sensedEnemyNearBy())
        {
            rc.explode();
        }
    }

    private static boolean isCloseToTargetLocation()
    {
        return (targetLocation != null && Measure.distance(targetLocation, myLocation) < CONTACT_RADIUS);
    }

    private static boolean sensedEnemyNearBy()
    {
        updateEnemyInRange(2);
        return (enemies.length > 4);
    }
}