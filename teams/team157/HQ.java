package team157;

import java.util.Random;

import team157.Utility.*;
import battlecode.common.*;

public class HQ extends Structure {
    
    //General methods =========================================================

    private final static RobotType[] buildOrder1 = {
            RobotType.BARRACKS, RobotType.BARRACKS,
            RobotType.SUPPLYDEPOT, RobotType.SUPPLYDEPOT, RobotType.HELIPAD,
            RobotType.SUPPLYDEPOT, RobotType.SUPPLYDEPOT, RobotType.HELIPAD,
            RobotType.SUPPLYDEPOT, RobotType.SUPPLYDEPOT, RobotType.HELIPAD
    };

    private enum HqState {
       BUILD_BUILDING, BUILD_UNIT
    }

    private static HqState state = HqState.BUILD_BUILDING;
    private static int numBuilding = 0;

    public static void start() throws GameActionException {
        init();
        while(true) {
            loop();
            //Yield the round
            rc.yield();
        }
    }
    
    private static void init() throws GameActionException {
        rc.setIndicatorString(0,"hello i'm a hq.");
        
        //Initiate radio map
        setMaps(HQLocation,3);
        setMaps(enemyHQLocation,2);
        if(HQLocation.x != enemyHQLocation.x && HQLocation.y != enemyHQLocation.y) {
            //rotational symmetry
            symmetry = 3;
            rc.broadcast(getChannel(ChannelName.MAP_SYMMETRY),3);
        }
        
        // Init LastAttackedLocations
        team157.Utility.LastAttackedLocationsReport.HQinit();
        
        team157.Utility.LastAttackedLocationsReport.everyRobotInit();
    }

    // TODO: consider to refactor this method
    private static void checkForEnemies() throws GameActionException
    {
        RobotInfo[] enemies = rc.senseNearbyRobots(sightRange, enemyTeam);

        // Vigilance: stops everything and attacks when enemies are in attack range.
        while (enemies.length > 0) {
            if (rc.isWeaponReady()) {
                // basicAttack(enemies);
                priorityAttack(enemies, attackPriorities);
            }
            enemies = rc.senseNearbyRobots(attackRange, enemyTeam);
            rc.yield();
        }
    }

    // TODO:  Just for thought - modify this method so that we can reserve some minimum amounts of emergency fund.
    private static boolean hasFunds(double cost)
    {
        return rc.getTeamOre() > cost;
    }

    private static RobotType getNextBuilding()
    {
        if (numBuilding < buildOrder1.length)
        {
            return buildOrder1[numBuilding];
        }
        // We want to build as many supply depots as possible, whenever we have funds
        return RobotType.SUPPLYDEPOT;
    }

    // TODO: @Josiah I need your help to figure out how to communicate.
    // Also, where do I need to put the building?
    // Should the beaver decide, or should HQ decide?
    private static void build(RobotType building) throws GameActionException
    {
        Request.broadcastToUnitType(
            Request.getConstructBuildingRequest(
            building.ordinal(), 0, 0, 10),
            RobotType.BEAVER.ordinal()
        );

        numBuilding += 1;
    }
    
    private static void loop() throws GameActionException {
        // Code that runs in every robot (including buildings, excepting missiles)
        sharedLoopCode();
        
        checkForEnemies();
        
        //Debug
        //for (RobotType robotType: RobotType.values()) {
        //    System.out.println("The number of " + robotType + " is " + RobotCount.read(robotType));
        //}

        RobotType nextBuilding = getNextBuilding();

        // In the future we can add some probabilistic constants so that we can switch between buildings and units
        if (state == HqState.BUILD_BUILDING && hasFunds(nextBuilding.oreCost))
        {
//            System.out.println("Sending barracks build request");
     //       build(nextBuilding);
        }
        
        if (RobotCount.read(RobotType.BEAVER) < 15)
            trySpawn(HQLocation.directionTo(enemyHQLocation), RobotType.BEAVER);
        
        dispenseSupply(suppliabilityMultiplier);
        //if(Clock.getRoundNum() == 1500) printRadioMap();

        Waypoints.refreshLocalCache();

        // Clean up robot count data for next round -- do not remove, do not
        // attempt to use RobotCount after this line before the next turn.
        RobotCount.reset();
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