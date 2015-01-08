package team157;

import java.util.Random;
import battlecode.common.*;

public class HQ extends RobotPlayer {
    
    //General methods =========================================================
    
    public static void start() throws GameActionException {
        init();
        while(true) {
            loop();
            rc.yield(); //Yield the round
        }
    }
    
    private static void init() throws GameActionException {
        rc.setIndicatorString(0,"hello i'm a hq.");
        
        
    }
    
    private static void loop() throws GameActionException {
        
        //Vigilance
        RobotInfo[] enemies = rc.senseNearbyRobots(atkrange, enmteam);
        while(enemies.length > 0) {
            if(rc.isWeaponReady()) {
                //basicAttack(enemies);
                priorityAttack(enemies,atkpriorities);
            }
            enemies = rc.senseNearbyRobots(atkrange, enmteam);
            rc.yield();
        }
        
        //Spawn
        trySpawn(hqloc.directionTo(enmloc),RobotType.BEAVER);
        
        //Dispense supply
        dispenseSupply(hpcapacity);
    }
    
    //Specific methods =========================================================
    
    private static int[] atkpriorities = {
        20/*0:HQ*/,         19/*1:TOWER*/,      13/*2:SUPPLYDPT*/,  16/*3:TECHINST*/,
        12/*4:BARRACKS*/,   11/*5:HELIPAD*/,    14/*6:TRNGFIELD*/,  10/*7:TANKFCTRY*/,
        15/*8:MINERFCTRY*/, 18/*9:HNDWSHSTN*/,  9/*10:AEROLAB*/,    7/*11:BEAVER*/,
        17/*12:COMPUTER*/,  5/*13:SOLDIER*/,    6/*14:BASHER*/,     8/*15:MINER*/,
        4/*16:DRONE*/,      2/*17:TANK*/,       3/*18:COMMANDER*/,  0/*19:LAUNCHER*/,
        1/*20:MISSILE*/
    };
    //lower means more important
    //needs to be adjusted based on defence strategy
    
    private static double[] hpcapacity = {
        0/*0:HQ*/,          0/*1:TOWER*/,       0/*2:SUPPLYDPT*/,   0/*3:TECHINST*/,
        0/*4:BARRACKS*/,    0/*5:HELIPAD*/,     0/*6:TRNGFIELD*/,   0/*7:TANKFCTRY*/,
        0/*8:MINERFCTRY*/,  0/*9:HNDWSHSTN*/,   0/*10:AEROLAB*/,    1/*11:BEAVER*/,
        0/*12:COMPUTER*/,   1/*13:SOLDIER*/,    1/*14:BASHER*/,     1/*15:MINER*/,
        1/*16:DRONE*/,      1/*17:TANK*/,       1/*18:COMMANDER*/,  1/*19:LAUNCHER*/,
        0/*20:MISSILE*/
    };
    
}