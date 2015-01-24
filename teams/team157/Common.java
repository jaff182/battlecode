package team157;

import java.util.Random;

import team157.Utility.*;
import team157.Utility.Map;
import battlecode.common.*;

public class Common extends RobotPlayer {
    
    //Global variables ========================================================
  
    //Unset Variables
    public static MapLocation HQLocation, enemyHQLocation, myLocation; //locations
    public static MapLocation[] myTowers, enemyTowers; //tower location arrays
    public static Team myTeam, enemyTeam;
    public static int sightRange, attackRange; //ranges
    public static Random rand;
    public static int distanceBetweenHQs;
    
    public static final int SMALL_MAP_SIZE = 2000;
    public static final int LARGE_MAP_SIZE = 5000;
    
    public final static Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST}; //call directions[i] for the ith direction
    public final static int[] offsets = {0,1,-1,2,-2,3,-3,4};
    public final static RobotType[] robotTypes = {RobotType.HQ,RobotType.TOWER,RobotType.SUPPLYDEPOT,RobotType.TECHNOLOGYINSTITUTE,RobotType.BARRACKS,RobotType.HELIPAD,RobotType.TRAININGFIELD,RobotType.TANKFACTORY,RobotType.MINERFACTORY,RobotType.HANDWASHSTATION,RobotType.AEROSPACELAB,RobotType.BEAVER,RobotType.COMPUTER,RobotType.SOLDIER,RobotType.BASHER,RobotType.MINER,RobotType.DRONE,RobotType.TANK,RobotType.COMMANDER,RobotType.LAUNCHER,RobotType.MISSILE}; //in order of ordinal
        
    /**
     * One way to prevent ore hijacking is to make everybody wait for a fixed ore 
     * level, that is more than the ore cost of anything.
     */
    public static final int UNIFIED_ORE_COST = 1000;

    // The number of robots produced before this robot.
    // Includes HQ and towers in count, also determines execution order ingame
    // (lower countingIDs move before higher ones).
    public static int countingID;
    
    //Main script =============================================================
    public static void commonInit() throws GameActionException {
        
        //Global inits --------------------------------------------------------
        rand = new Random(rc.getID()); //seed random number generator
        //my properties
        sightRange = myType.sensorRadiusSquared;
        attackRange = myType.attackRadiusSquared;
        
        //sense locations
        HQLocation = rc.senseHQLocation();
        enemyHQLocation = rc.senseEnemyHQLocation();
        myTowers = rc.senseTowerLocations();
        enemyTowers = rc.senseEnemyTowerLocations();
        distanceBetweenHQs = HQLocation.distanceSquaredTo(enemyHQLocation);
        updateMyLocation();
        
        //get teams
        myTeam = rc.getTeam();
        enemyTeam = myTeam.opponent();
        
        try {
            //Internal map
            //This year's implementation randomizes offsets to the x,y coordinates
            //Coordinate offsets at map[60][60]
            Map.mapx0 = (HQLocation.x+enemyHQLocation.x)/2;
            Map.mapy0 = (HQLocation.y+enemyHQLocation.y)/2;
            //Get symmetry from radio
            Map.symmetry = rc.readBroadcast(Channels.MAP_SYMMETRY);
            
            //set countingID for messaging (WARNING, ASSUMES MESSAGING ARRAY IS INITIALIZED TO ZERO)
            if(myType != RobotType.MISSILE) {
                countingID = rc.readBroadcast(Channels.SEQ_UNIT_NUMBER);
                rc.broadcast(Channels.SEQ_UNIT_NUMBER, countingID+1);
            }
            
            // Init the reporting system for enemy attacks
            LastAttackedLocationsReport.everyRobotInit();
            
          //RobotType specific methods --------------------------------------
            switch(myType) {
                case AEROSPACELAB: AerospaceLab.start(); break;
                case BARRACKS: Barracks.start(); break;
                case BASHER: Basher.start(); break;
                case BEAVER: Beaver.start(); break;
                case COMMANDER: AttackingUnit.start(); break;
                case COMPUTER: Computer.start(); break;
                case DRONE: Drone.start(); break;
                case HANDWASHSTATION: HandwashStation.start(); break;
                case HELIPAD: Helipad.start(); break;
                case HQ: HQ.start(); break;
                case LAUNCHER: Launcher.start(); break;
                case MINER: Miner.start(); break;
                case MINERFACTORY: MinerFactory.start(); break;
                case SOLDIER: Soldier.start(); break;
                case SUPPLYDEPOT: SupplyDepot.start(); break;
                case TANK: AttackingUnit2.start(); break;
                case TANKFACTORY: TankFactory.start(); break;
                case TECHNOLOGYINSTITUTE: TechnologyInstitute.start(); break;
                case TOWER: Tower.start(); break;
                case TRAININGFIELD: TrainingField.start(); break;
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
    //Map methods =============================================================
    
    /**
     * Update internal map within sensor radius using values from radio map.
     * If some locations are unknown, then sense them and update radio map and
     * internal map.
     * @param robotLoc location of robot
     * @throws GameActionException
     */
    public static void initialSense(MapLocation robotLoc) throws GameActionException {
        MapLocation[] sensingLoc = MapLocation.getAllMapLocationsWithinRadiusSq(robotLoc, sightRange);
        for (MapLocation loc: sensingLoc) {
            Map.checkPathable(loc);
        }
    }
    
    
    
    //Attack ==================================================================
    
    //RobotInfo array of nearby units
    public static RobotInfo[] enemies;
    public static RobotInfo[] friends;
    
    //Attack range opening triggers
    public static int mobLevel;
    
    /**
     * Checks whether loc is in the splash damage region of an HQ at hqloc.
     * @param loc Location to test
     * @param hqloc Location of HQ
     * @return True iff in splash damage region
     */
    public static boolean isInSplashRegion(MapLocation loc, MapLocation hqloc) {
        int dx = Math.abs(hqloc.x-loc.x);
        int dy = Math.abs(hqloc.y-loc.y);
        return (dx+dy==10 && Math.abs(dx-dy)<=2) || hqloc.distanceSquaredTo(loc) <= 48;
    }
    
    /**
     * Basic attack on the first of detected nearby enemies
     * @param enemies RobotInfo array of enemies in attack range
     * @throws GameActionException
     */
    public static void basicAttack(RobotInfo[] enemies) throws GameActionException {
        if (enemies.length != 0)
            rc.attackLocation(enemies[0].location);
    }
    
    /**
     * Prioritized attack on the weakest of nearby enemies based on rating 
     * for each corresponding RobotType ordinal in robotTypes (eg: atkorder[1] = 20, 
     * atkorder[2] = 19 means TOWERs are more important to attack than SUPPLYDEPOTs)
     * @param enemies RobotInfo array of enemies in attack range
     * @param atkorder int array of attack priority rating
     * @throws GameActionException
     */
    public static void priorityAttack(RobotInfo[] enemies, int[] atkorder) throws GameActionException {
        RobotInfo attackTarget = choosePriorityAttackTarget(enemies, atkorder);
        if ( attackTarget != null) {
          //Attack selected target
            rc.attackLocation(attackTarget.location);
        }
    }
    
    /**
     * Choose priority attack target on the weakest of nearby enemies based on rating 
     * for each corresponding RobotType ordinal in robotTypes (eg: atkorder[1] = 20, 
     * atkorder[2] = 19 means TOWERs are more important to attack than SUPPLYDEPOTs)
     * @param enemies RobotInfo array of enemies in attack range
     * @param atkorder int array of attack priority rating
     * @return RobotInfo representing chosen priority attack target.
     * @throws GameActionException
     */
    public static RobotInfo choosePriorityAttackTarget(RobotInfo[] enemies, int[] atkorder) {
      //Initiate
        int targetidx = -1, targettype = 1;
        double minhp = 100000;
        
        //Check for weakest of highest priority enemy type
        for(int i=0; i<enemies.length; i++) {
            int type = enemies[i].type.ordinal();
            if(rc.canAttackLocation(enemies[i].location)) {         
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
            return enemies[targetidx];
        }
        return null;
    }
    
    
    /**
     * Choose priority attack target on nearby enemies to location of friendly unit to defend
     * @param defendLoc location of friendly unit to defend
     * @param enemies RobotInfo array of enemies in sight range of defended unit
     * @param atkorder int array of attack priority rank (0 to 20, allowing ties) for each corresponding RobotType ordinal in robotTypes (eg: atkorder[1] = 5 means TOWERs are the 6th most important RobotType to attack)
     * @return RobotInfo representing chosen priority attack target.
     * @throws GameActionException
     */
    public static RobotInfo chooseDefensePriorityAttackTarget(MapLocation defendLoc, RobotInfo[] enemies, int[] atkorder) {
      //Initiate
        int targetidx = -1, targettype = 1;
        double minhp = 100000;
        
        //Check for weakest of highest priority enemy type
        for(int i=0; i<enemies.length; i++) {
            int type = enemies[i].type.ordinal();
            if(rc.canAttackLocation(enemies[i].location)) {
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
            return enemies[targetidx];
        }
        return null;
    }
    
    
    //Tests ===================================================================
    
    /**
     * Check RobotType ordinal agreement with order in robotTypes
     */
    public static void checkRobotTypeOrdinal() {
        for(int i=0; i<=20; i++) {
            assert robotTypes[i].ordinal() == i : robotTypes[i];
        }
        System.out.println("RobotType Ordinal Test passed.");
    }
    
    
    /**
     * Code that runs at the start of every loop (shared by every robot).
     * 
     * Note that this code runs in buildings too.
     * 
     * Does not run in missile and towers.
     * 
     * @throws GameActionException 
     */
    public static void sharedLoopCode() throws GameActionException {
        // Update global counts of robots - do not remove
        RobotCount.report();
        
        //Update Mob level to know which enemy structure ranges can be traversed
        mobLevel = rc.readBroadcast(Channels.MOB_LEVEL);
        
        // Report any drops in HP
        // TODO: I'm not entirely sure whether you can do this without creating an instance
        LastAttackedLocationsReport.report();
    }
    
    
    public static void updateMyLocation() {
        myLocation = rc.getLocation();
    }

    public static void updateFriendlyInRange(int range)
    {
        friends = rc.senseNearbyRobots(range, myTeam);
    }

    public static void updateFriendlyInSight()
    {
        updateFriendlyInRange(sightRange);
    }

    public static void updateEnemyInRange(int range)
    {
        enemies = rc.senseNearbyRobots(range, enemyTeam);
    }

    public static void updateEnemyInSight()
    {
        updateEnemyInRange(sightRange);
    }

    //Parameters ==============================================================
    
    /**
     * The importance rating that enemy units of each RobotType should be attacked
     * (so higher means attack first). Needs to be adjusted dynamically based on
     * defence strategy.
     */
    protected static int[] attackPriorities = {
        20/*0:HQ*/,         21/*1:TOWER*/,      6/*2:SUPPLYDPT*/,   3/*3:TECHINST*/,
        7/*4:BARRACKS*/,    8/*5:HELIPAD*/,     5/*6:TRNGFIELD*/,   9/*7:TANKFCTRY*/,
        4/*8:MINERFCTRY*/,  1/*9:HNDWSHSTN*/,   10/*10:AEROLAB*/,   13/*11:BEAVER*/,
        2/*12:COMPUTER*/,   16/*13:SOLDIER*/,   15/*14:BASHER*/,    12/*15:MINER*/,
        14/*16:DRONE*/,     17/*17:TANK*/,      18/*18:COMMANDER*/, 11/*19:LAUNCHER*/,
        19/*20:MISSILE*/
    };

    /**
     * Multipliers for the effective supply capacity for friendly unit robotTypes, by
     * which the dispenseSupply() and distributeSupply() methods allocate supply (so
     * higher means give more supply to units of that type).
     */
    protected static double[] suppliabilityMultiplier_Conservative = {
        1/*0:HQ*/,          1/*1:TOWER*/,       1/*2:SUPPLYDPT*/,   1/*3:TECHINST*/,
        1/*4:BARRACKS*/,    1/*5:HELIPAD*/,     1/*6:TRNGFIELD*/,   1/*7:TANKFCTRY*/,
        1/*8:MINERFCTRY*/,  1/*9:HNDWSHSTN*/,   1/*10:AEROLAB*/,    0/*11:BEAVER*/,
        0/*12:COMPUTER*/,   0/*13:SOLDIER*/,    0/*14:BASHER*/,     0.5/*15:MINER*/,
        0/*16:DRONE*/,      0/*17:TANK*/,       0/*18:COMMANDER*/,  0/*19:LAUNCHER*/,
        0/*20:MISSILE*/
    };

    protected static double[] suppliabilityMultiplier_Preattack = {
        0/*0:HQ*/,          0/*1:TOWER*/,       0/*2:SUPPLYDPT*/,   0/*3:TECHINST*/,
        0/*4:BARRACKS*/,    0/*5:HELIPAD*/,     0/*6:TRNGFIELD*/,   0/*7:TANKFCTRY*/,
        0/*8:MINERFCTRY*/,  0/*9:HNDWSHSTN*/,   0/*10:AEROLAB*/,    1/*11:BEAVER*/,
        0/*12:COMPUTER*/,   1/*13:SOLDIER*/,    1/*14:BASHER*/,     1/*15:MINER*/,
        1/*16:DRONE*/,      1/*17:TANK*/,       1/*18:COMMANDER*/,  1/*19:LAUNCHER*/,
        0/*20:MISSILE*/
    };
}