package team157;

import team157.Utility.*;
import battlecode.common.*;

public class HQ extends Structure {

    //Global variables ========================================================
    
    private static final int tankDefenseChannel = Channels.TANK_DEFENSE_COUNT;
    private static int baseNumberOfTanksNeeded = 0;
    private static int numberOfTanksNeeded = baseNumberOfTanksNeeded;
    private static int numberOfTowers = rc.senseTowerLocations().length;
    
    
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

        MapLocation soldierLoc = myLocation.add(myLocation.directionTo(enemyHQLocation), 6);
        SoldierGroup.setNextWaypoint(soldierLoc.x, soldierLoc.y, null);
        // Init LastAttackedLocations
        team157.Utility.LastAttackedLocationsReport.HQinit();
        team157.Utility.LastAttackedLocationsReport.everyRobotInit();
        //team157.Utility.BeaversBuildRequest.HQinit();
        
        // Testing new build system
        // Add 2 Helipads on round 100, 1 Barracks on round 500


        if (distanceBetweenHQs < SMALL_MAP_SIZE) {
            // drone rush on small map
            BuildOrder.add(RobotType.TECHNOLOGYINSTITUTE);
            BuildOrder.add(RobotType.TRAININGFIELD);
            BuildOrder.add(RobotType.MINERFACTORY);
        } else {
            BuildOrder.add(RobotType.MINERFACTORY);
            BuildOrder.add(RobotType.TECHNOLOGYINSTITUTE);
            BuildOrder.add(RobotType.TRAININGFIELD);
        }
        
        BuildOrder.add(RobotType.HELIPAD);
        BuildOrder.add(RobotType.SUPPLYDEPOT);

        // change strategy based on map size
        if (distanceBetweenHQs < SMALL_MAP_SIZE) {
            rc.setIndicatorString(1, "small map");
            BuildOrder.add(RobotType.BARRACKS);
            BuildOrder.add(RobotType.TANKFACTORY);
            BuildOrder.add(RobotType.HELIPAD);
            BuildOrder.add(RobotType.SUPPLYDEPOT);
            BuildOrder.add(RobotType.TANKFACTORY);
        } else {
            rc.setIndicatorString(1, "large map");
            BuildOrder.add(RobotType.BARRACKS);
            BuildOrder.add(RobotType.TANKFACTORY);
            BuildOrder.add(RobotType.TANKFACTORY);
            BuildOrder.add(RobotType.SUPPLYDEPOT);
            BuildOrder.add(RobotType.HELIPAD);
            BuildOrder.add(RobotType.SUPPLYDEPOT);
            BuildOrder.add(RobotType.TANKFACTORY);
            BuildOrder.add(RobotType.TANKFACTORY);
            BuildOrder.add(RobotType.SUPPLYDEPOT);
        }

    }
    
    private static void loop() throws GameActionException {
        // Clean up robot count data for this round -- do not remove, will break invariants
        RobotCount.reset();
        MinerEffectivenessCount.reset();
        numberOfTowers = rc.senseTowerLocations().length;
        
        // Code that runs in every robot (including buildings, excepting missiles)
        sharedLoopCode();
        
        callForTankReinforcements();
        updateEnemyInRange(52);//52 includes spashable region
        checkForEnemies();
        

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
    
    /**
     * Like priority attack except that it also tries to hit enemies in splashable 
     * region.
     */
    public static void HQPriorityAttack(RobotInfo[] enemies, int[] atkorder) throws GameActionException {
      //Initiate
        int targetidx = -1, targettype = 1;
        double minhp = 100000;
        
        //Check for weakest of highest priority enemy type
        for(int i=0; i<enemies.length; i++) {
            int type = enemies[i].type.ordinal();
            if(isInSplashRegion(enemies[i].location,HQLocation)) {
                if(atkorder[type] > targettype) {
                    //More important enemy to attack
                    targettype = atkorder[type];
                    minhp = enemies[i].health;
                    targetidx = i;
                } else if(atkorder[type] == targettype && enemies[i].health < minhp) {
                    //Same priority enemy but lower health
                    minhp = enemies[i].health;
                    targetidx = i;
                }
            }
        }
        if (targetidx != -1) {
            //Attack
            MapLocation loc = enemies[targetidx].location;
            if(rc.canAttackLocation(loc)) {
                rc.attackLocation(loc);
            } else {
                //Check if can hit with splash
                loc = loc.add(loc.directionTo(HQLocation));
                if(rc.canAttackLocation(loc)) {
                    rc.attackLocation(loc);
                }
            }
        }
    }
    
    
    // TODO: consider to refactor this method
    private static void checkForEnemies() throws GameActionException {
        //No need to hijack code for structure
        if (rc.isWeaponReady()) {
            // basicAttack(enemies);
            HQPriorityAttack(enemies, attackPriorities);
        }
        
        //Old splash damage code
        //Preserve because can use to improve current splash damage code
        //But not so simple like just checking directions, since there are many more 
        //locations to consider hitting.
        /*if (numberOfTowers > 4) {
            //can do splash damage
            RobotInfo[] enemiesInSplashRange = rc.senseNearbyRobots(37, enemyTeam);
            if (enemiesInSplashRange.length > 0) {
                int[] enemiesInDir = new int[8];
                for (RobotInfo info: enemiesInSplashRange) {
                    enemiesInDir[myLocation.directionTo(info.location).ordinal()]++;
                }
                int maxDirScore = 0;
                int maxIndex = 0;
                for (int i = 0; i < 8; i++) {
                    if (enemiesInDir[i] >= maxDirScore) {
                        maxDirScore = enemiesInDir[i];
                        maxIndex = i;
                    }
                }
                MapLocation attackLoc = myLocation.add(directions[maxIndex],5);
                if (rc.isWeaponReady() && rc.canAttackLocation(attackLoc)) {
                    rc.attackLocation(attackLoc);
                }
            }
        }//*/
  
    }
    
    private static boolean hasFewBeavers() throws GameActionException {
        if (Clock.getRoundNum() < 150) { 
        //hard code initial miner factory and helipad
            return RobotCount.read(RobotType.BEAVER)<1;
        }
        return RobotCount.read(RobotType.BEAVER) < 3;
    }

    // We have enough fund above some baselines
    private static boolean hasFunds(double cost)
    {
        if (Clock.getRoundNum() < 200) { //TODO edit this out if necessary
            return rc.getTeamOre() > cost;
        } else {
            return rc.getTeamOre() > cost*2;
        }
    }
    
    
    private static void callForTankReinforcements() {
        if (rc.senseNearbyRobots(81, enemyTeam).length > 10) {
            numberOfTanksNeeded += 2;
        } else {
            numberOfTanksNeeded = baseNumberOfTanksNeeded;
        }
    }
    
    
    //Debug methods ===========================================================
    
    /**
     * Count number of each friendly robot type
     */
    public static void debug_countTypes() throws GameActionException {
        for (RobotType robotType: RobotType.values()) {
            System.out.println("The number of " + robotType + " is " + RobotCount.read(robotType));
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
        0/*0:HQ*/,          1/*1:TOWER*/,       0/*2:SUPPLYDPT*/,   0/*3:TECHINST*/,
        1/*4:BARRACKS*/,    1/*5:HELIPAD*/,     0/*6:TRNGFIELD*/,   1/*7:TANKFCTRY*/,
        1/*8:MINERFCTRY*/,  0/*9:HNDWSHSTN*/,   1/*10:AEROLAB*/,    1/*11:BEAVER*/,
        0/*12:COMPUTER*/,   1/*13:SOLDIER*/,    1/*14:BASHER*/,     1/*15:MINER*/,
        1/*16:DRONE*/,      1/*17:TANK*/,       1/*18:COMMANDER*/,  1/*19:LAUNCHER*/,
        0/*20:MISSILE*/
    };
    
}