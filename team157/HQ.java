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
        //rc.setIndicatorString(0,"hello i'm a hq.");
        
        //checkRobotTypeOrdinal();
        
        
    }
    
    private static void loop() throws GameActionException {
        
        //Vigilance
        RobotInfo[] enemies = rc.senseNearbyRobots(myrng, enmteam);
        while(enemies.length > 0) {
            if(rc.isWeaponReady()) {
                //basicAttack(enemies);
                priorityAttack(enemies,atkpriorities);
            }
            enemies = rc.senseNearbyRobots(myrng, enmteam);
            rc.yield();
        }
        
        //Spawn
        trySpawn(hqloc.directionTo(enmloc),RobotType.BEAVER);
        
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
    
    
    
}