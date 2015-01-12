package team157;

import java.util.Random;

import team157.Utility.LastAttackedLocationsReport;
import team157.Utility.Waypoints;
import battlecode.common.*;

public class HQ extends Structure {
    
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
        
        //Initiate radio map
        setMaps(HQLocation,3);
        setMaps(enemyHQLocation,2);
        if(HQLocation.x != enemyHQLocation.x &&
            HQLocation.y != enemyHQLocation.y) {
            //rotational symmetry
            symmetry = 3;
            rc.broadcast(getChannel(ChannelName.MAP_SYMMETRY),3);
        }
        
        LastAttackedLocationsReport.init();
    }
    
    private static void loop() throws GameActionException {
        
        //Vigilance
        //Stops everything and attacks when enemies are in attack range.
        RobotInfo[] enemies = rc.senseNearbyRobots(attackRange, enemyTeam);
        while(enemies.length > 0) {
            if(rc.isWeaponReady()) {
                //basicAttack(enemies);
                priorityAttack(enemies,attackPriorities);
            }
            enemies = rc.senseNearbyRobots(attackRange, enemyTeam);
            rc.yield();
        }
        if (rc.getTeamOre() > RobotType.BARRACKS.oreCost) {
//            System.out.println("Sending barracks build request");
//            Request.broadcastToUnitType(
//                    Request.getConstructBuildingRequest(
//                            RobotType.BARRACKS.ordinal(), 0, 0, 10),
//                    RobotType.BEAVER.ordinal());
        }
        
        //Spawn
        trySpawn(HQLocation.directionTo(enemyHQLocation),RobotType.BEAVER);
        
        //Dispense supply
        dispenseSupply(suppliabilityMultiplier);
        
        //if(Clock.getRoundNum() == 1500) printRadioMap();
        
        Waypoints.refreshLocalCache();
    }
    
    //Specific methods =========================================================
    
    /**
     * The importance rating that enemy units of each RobotType should be attacked 
     * (so higher means attack first). Needs to be adjusted dynamically based on 
     * defence strategy.
     */
    private static int[] attackPriorities = {
        21/*0:HQ*/,         21/*1:TOWER*/,      7/*2:SUPPLYDPT*/,   4/*3:TECHINST*/,
        8/*4:BARRACKS*/,    9/*5:HELIPAD*/,     6/*6:TRNGFIELD*/,   10/*7:TANKFCTRY*/,
        5/*8:MINERFCTRY*/,  2/*9:HNDWSHSTN*/,   11/*10:AEROLAB*/,   13/*11:BEAVER*/,
        3/*12:COMPUTER*/,   15/*13:SOLDIER*/,   14/*14:BASHER*/,    12/*15:MINER*/,
        16/*16:DRONE*/,     18/*17:TANK*/,      17/*18:COMMANDER*/, 19/*19:LAUNCHER*/,
        20/*20:MISSILE*/
    };
    
    /**
     * Multipliers for the effective supply capacity for friendly unit robotTypes, by 
     * which the dispenseSupply() and distributeSupply() methods allocate supply (so 
     * higher means give more supply to units of that type).
     */
    private static double[] suppliabilityMultiplier = {
        0/*0:HQ*/,          1/*1:TOWER*/,       0/*2:SUPPLYDPT*/,   1/*3:TECHINST*/,
        1/*4:BARRACKS*/,    1/*5:HELIPAD*/,     0/*6:TRNGFIELD*/,   1/*7:TANKFCTRY*/,
        1/*8:MINERFCTRY*/,  0/*9:HNDWSHSTN*/,   1/*10:AEROLAB*/,    1/*11:BEAVER*/,
        0/*12:COMPUTER*/,   1/*13:SOLDIER*/,    1/*14:BASHER*/,     1/*15:MINER*/,
        1/*16:DRONE*/,      1/*17:TANK*/,       1/*18:COMMANDER*/,  1/*19:LAUNCHER*/,
        0/*20:MISSILE*/
    };
    
}