package team157;

import team157.Utility.*;
import battlecode.common.*;

public class HQ extends Structure {

    //Global variables ========================================================
    
    private static final int tankDefenseChannel = Channels.TANK_DEFENSE_COUNT;
    private static int baseNumberOfTanksNeeded = 0;
    private static int numberOfTanksNeeded = baseNumberOfTanksNeeded;
        
    //Old building request implementation -------------------------------------
    private final static RobotType[] buildOrder1 = {
            //RobotType.BARRACKS, RobotType.BARRACKS,
            RobotType.HELIPAD,
            RobotType.HELIPAD,
            RobotType.HELIPAD,
            RobotType.SUPPLYDEPOT, RobotType.HELIPAD,
            RobotType.SUPPLYDEPOT, RobotType.HELIPAD,
    };
    
    private final static RobotType[] buildOrder2 = {
        RobotType.BARRACKS, RobotType.TANKFACTORY, RobotType.TANKFACTORY,
        RobotType.TANKFACTORY,
    };
    
    /**
     * Keeps track of whether HQ has requested a beaver to build a building (and
     * hasn't gotten a response)
     * 
     * @author Josiah
     *
     */
    private enum BuildingRequestState {
        REQUESTING_BUILDING, NO_PENDING_REQUEST
    }
    
    private static BuildingRequestState buildingRequestState = BuildingRequestState.NO_PENDING_REQUEST;

    private static BuildingQueue queue;
    
    
    //Declarative building implementation -------------------------------------
    
    
    
    //General methods =========================================================
    
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
        // call for tank defense units
        rc.broadcast(tankDefenseChannel, numberOfTanksNeeded);

        //old building code
        //queue = new BuildingQueue(buildOrder1, RobotType.SUPPLYDEPOT);


        //Initiate radio map TODO: towers locations?
        Map.setMaps(HQLocation.x,HQLocation.y,3);
        if(HQLocation.x != enemyHQLocation.x && HQLocation.y != enemyHQLocation.y) {
            //rotational symmetry
            Map.symmetry = 3;//Map.rotationSymmetry;
            rc.broadcast(Channels.MAP_SYMMETRY, Map.symmetry);
        }

        
        // Init LastAttackedLocations
        team157.Utility.LastAttackedLocationsReport.HQinit();
        team157.Utility.LastAttackedLocationsReport.everyRobotInit();
        //team157.Utility.BeaversBuildRequest.HQinit();
        
        if (distanceBetweenHQs < SMALL_MAP_SIZE) {
            // drone rush on small map
            BuildOrder.add(RobotType.HELIPAD);
        } else {
            //Add MinerFactory at the start
            BuildOrder.add(RobotType.MINERFACTORY);
        }

    }
    
    private static void loop() throws GameActionException {
        // Clean up robot count data for this round -- do not remove, will break invariants
        RobotCount.reset();
        MinerEffectivenessCount.reset();
        
        // Code that runs in every robot (including buildings, excepting missiles)
        sharedLoopCode();
        
        callForTankReinforcements();
        checkForEnemies();
        
        
        /*/old building code ---------------------------------------------------
        RobotType nextBuilding = queue.getNextBuilding();

        // In the future we can add some probabilistic constants so that we can switch between buildings and units
        if(Clock.getRoundNum() < 1800) {
            build(nextBuilding); // Read javadoc of build for caveats
        }
        //*///-----------------------------------------------------------------
        
        
        //Testing new build system
        //Add 2 Helipads on round 100, 1 Barracks on round 500
        if(Clock.getRoundNum() == 100) {
            if (distanceBetweenHQs < SMALL_MAP_SIZE) {
                BuildOrder.add(RobotType.MINERFACTORY);
            } else {
                BuildOrder.add(RobotType.HELIPAD);
            }
        }
        if(Clock.getRoundNum() == 250) {
            BuildOrder.add(RobotType.HELIPAD);
            BuildOrder.add(RobotType.SUPPLYDEPOT);
        }
        if(Clock.getRoundNum() == 500) {
            // change strategy based on map size
            if (distanceBetweenHQs < SMALL_MAP_SIZE) {
                rc.setIndicatorString(1, "small map");
                BuildOrder.add(RobotType.HELIPAD);
                BuildOrder.add(RobotType.HELIPAD);
                BuildOrder.add(RobotType.SUPPLYDEPOT);
                BuildOrder.add(RobotType.SUPPLYDEPOT);
            }
            else if (distanceBetweenHQs < LARGE_MAP_SIZE) {
                rc.setIndicatorString(1, "medium map");
                BuildOrder.add(RobotType.HELIPAD);
                BuildOrder.add(RobotType.BARRACKS);
                BuildOrder.add(RobotType.TANKFACTORY);
                BuildOrder.add(RobotType.SUPPLYDEPOT);
            }
            else {
                rc.setIndicatorString(1, "large map");
                BuildOrder.add(RobotType.BARRACKS);
                BuildOrder.add(RobotType.TANKFACTORY);
                BuildOrder.add(RobotType.TANKFACTORY);
                BuildOrder.add(RobotType.SUPPLYDEPOT);
            }
        }
        
        if(Clock.getRoundNum() == 1000 || Clock.getRoundNum() == 1200 && rc.getTeamOre() > 1000) {
            if (distanceBetweenHQs < SMALL_MAP_SIZE) {
                BuildOrder.add(RobotType.HELIPAD);
                BuildOrder.add(RobotType.HELIPAD);
                BuildOrder.add(RobotType.SUPPLYDEPOT);
                BuildOrder.add(RobotType.SUPPLYDEPOT);
            } else {
                BuildOrder.add(RobotType.HELIPAD);
                BuildOrder.add(RobotType.TANKFACTORY);
                BuildOrder.add(RobotType.SUPPLYDEPOT);
            }
        }
        
        
        //Spawn beavers
        if (hasFewBeavers()) { 
            trySpawn(HQLocation.directionTo(enemyHQLocation), RobotType.BEAVER);
        }
        
        //Dispense supply
        dispenseSupply(suppliabilityMultiplier);
        
        //Debug
        //if(Clock.getRoundNum() == 1500) Map.debug_printRadioMap();
        //if(Clock.getRoundNum() == 1500) BuildOrder.printBuildOrder();

    }
    
    //Other methods ===========================================================
    
    // TODO: consider to refactor this method
    private static void checkForEnemies() throws GameActionException
    {
        updateEnemyInRange(sightRange);

        // Vigilance: stops everything and attacks when enemies are in attack range.
        while (enemies.length > 0) {
            if (rc.isWeaponReady()) {
                // basicAttack(enemies);
                priorityAttack(enemies, attackPriorities);
            }
            updateEnemyInRange(attackRange);
            rc.yield();
        }
    }
    
    private static boolean hasFewBeavers() throws GameActionException {
        if (Clock.getRoundNum() < 150) { 
        //hard code initial miner factory and helipad
            return RobotCount.read(RobotType.BEAVER)<1;
        }
        return RobotCount.read(RobotType.BEAVER) < 3;
    }

    // TODO:  Just for thought - modify this method so that we can reserve some minimum amounts of emergency fund.
    private static boolean hasFunds(double cost)
    {
        if (Clock.getRoundNum() < 200) { //TODO edit this out if necessary
            return rc.getTeamOre() > cost;
        } else {
            return rc.getTeamOre() > cost*2;
        }
    }
    
    
    
    //Old building implementation ---------------------------------------------

    /**
     * Maintains the HQ build queue. Run once per turn, with current building on
     * build queue.
     * 
     * This function must advance the build queue for you automatically. Do not
     * attempt to do so outside. Increments numBuilding when acknowledgement is
     * received from beaver.
     *
     *
     * @param building
     *            IMPORTANT: READ CAVEATS ABOVE
     * @throws GameActionException
     */
    private static void build(RobotType building) throws GameActionException
    {
        int numberOfBeavers = RobotCount.read(RobotType.BEAVER);
        switch (buildingRequestState) {
        case REQUESTING_BUILDING:
            if (BeaversBuildRequest.wasMyBuildMessageReceived()) {
                queue.updateBuildingNumber();
                buildingRequestState = BuildingRequestState.NO_PENDING_REQUEST;
                return;
            }
            // Fall through, evidently our message was not received.
        case NO_PENDING_REQUEST:
                if (hasFunds(building.oreCost) && numberOfBeavers != 0) {
                    BeaversBuildRequest.pleaseBuildABuilding(building, numberOfBeavers, rand.nextInt(numberOfBeavers));
                    buildingRequestState = BuildingRequestState.REQUESTING_BUILDING;
                }
                break;
        }
    }
    

    
    public static void debug_countTypes() throws GameActionException {
        for (RobotType robotType: RobotType.values()) {
            System.out.println("The number of " + robotType + " is " + RobotCount.read(robotType));
        }
    }
    

    private static void callForTankReinforcements() {
        if (rc.senseNearbyRobots(81, enemyTeam).length > 10) {
            numberOfTanksNeeded += 2;
        } else {
            numberOfTanksNeeded = baseNumberOfTanksNeeded;
        }
    }
    

    //Parameters ==============================================================
    
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
        16/*16:DRONE*/,     18/*17:TANK*/,      17/*18:COMMANDER*/, 20/*19:LAUNCHER*/,
        19/*20:MISSILE*/
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