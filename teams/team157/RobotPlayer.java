package team157;

import java.util.Random;

import team157.Utility.*;
import battlecode.common.*;

public class RobotPlayer {
    
    //Global variables ========================================================
    //Unset Variables
    public static RobotController rc;
    public static MapLocation HQLocation, enemyHQLocation, myLocation; //locations
    public static MapLocation[] myTowers, enemyTowers; //tower location arrays
    public static Team myTeam, enemyTeam;
    public static RobotType myType;
    public static int sightRange, attackRange; //ranges
    public static Random rand;
    
    public final static Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST}; //call directions[i] for the ith direction
    public final static int[] offsets = {0,1,-1,2,-2,3,-3,4};
    public final static RobotType[] robotTypes = {RobotType.HQ,RobotType.TOWER,RobotType.SUPPLYDEPOT,RobotType.TECHNOLOGYINSTITUTE,RobotType.BARRACKS,RobotType.HELIPAD,RobotType.TRAININGFIELD,RobotType.TANKFACTORY,RobotType.MINERFACTORY,RobotType.HANDWASHSTATION,RobotType.AEROSPACELAB,RobotType.BEAVER,RobotType.COMPUTER,RobotType.SOLDIER,RobotType.BASHER,RobotType.MINER,RobotType.DRONE,RobotType.TANK,RobotType.COMMANDER,RobotType.LAUNCHER,RobotType.MISSILE}; //in order of ordinal

    // The number of robots produced before this robot.
    // Includes HQ and towers in count, also determines execution order ingame
    // (lower countingIDs move before higher ones).
    public static int countingID;
    
    //Main script =============================================================
    public static void run(RobotController RC) {
        
        //Global inits --------------------------------------------------------
        rc = RC; //set robotcontroller
        rand = new Random(rc.getID()); //seed random number generator
        
        //my properties
        myType = rc.getType();
        sightRange = myType.sensorRadiusSquared;
        attackRange = myType.attackRadiusSquared;
        
        //sense locations
        HQLocation = rc.senseHQLocation();
        enemyHQLocation = rc.senseEnemyHQLocation();
        myTowers = rc.senseTowerLocations();
        enemyTowers = rc.senseEnemyTowerLocations();
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
                case COMMANDER: Commander.start(); break;
                case COMPUTER: Computer.start(); break;
                case DRONE: Drone.start(); break;
                case HANDWASHSTATION: HandwashStation.start(); break;
                case HELIPAD: Helipad.start(); break;
                case HQ: HQ.start(); break;
                case LAUNCHER: Launcher.start(); break;
                case MINER: Miner.start(); break;
                case MINERFACTORY: MinerFactory.start(); break;
                case MISSILE: Missile.start(); break;
                case SOLDIER: Soldier.start(); break;
                case SUPPLYDEPOT: SupplyDepot.start(); break;
                case TANK: Tank.start(); break;
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
            senseMap(loc);
        }
    }
    
    /**
     * Update internal map at input loc using values from radio map.
     * If loc is unknown, then sense it and update radio map and
     * internal map.
     * @param loc location to sense
     * @throws GameActionException
     */
    public static void senseMap(MapLocation loc) throws GameActionException {
        int value = Map.getRadioMap(loc.x,loc.y);
        if (value == 0) {
            switch (rc.senseTerrainTile(loc)) {
                case VOID: Map.setMaps(loc.x,loc.y, 4); break;
                case NORMAL: Map.setMaps(loc.x,loc.y, 1); break;
                case OFF_MAP: Map.setMaps(loc.x,loc.y, 5); break;
                case UNKNOWN: break;
                default: break;
            }
        } else {
            Map.setInternalMap(loc.x,loc.y,value);
        }
    }
    
    //Comms ===================================================================
    
    /**
     * Names of channels. See javadoc for getChannel for more information.
     * 
     * Feel free to register and append your own names
     * 
     * @author Josiah
     *
     */
    public enum ChannelName {
        SEQ_UNIT_NUMBER, UNIT_COUNT_BASE, LAST_ATTACKED_COORDINATES, BEAVER_BUILD_REQUEST, EFFECTIVE_MINERS_COUNT,
        REQUEST_MAILBOX_BASE, REQUESTS_METADATA_BASE
    }
    
    /**
     * Deprecated. Please update javadoc here still though until code has been
     * replaced.
     * 
     * Use Channels.* to read and declare the channel directly.
     * 
     * Gets the index into the messaging array.
     * 
     * The messaging array is a size-65536 array of ints that can be broadcasted
     * to and read from by almost all Robots.
     * 
     * See "Messaging [bcd09]" from the spec document
     * 
     * Allocations:<br>
     * 16001 - number of units produced since start of game by you (including
     * towers, HQ) <br>
     * 16002-16049 - number of robots that exist now<br>
     * 16050-16070 - The last attacks that have occurred, by x, y coordinates.
     * count at 16030, pairs start from 16031 onwards. <br>
     * 16071-16074 - beaver build request system<br>
     * 16075-16076 - effective miner count system<br>
     * 16100-16140 - request system unit type mailboxes. each unit uses 2
     * channels. 17000-23999 - request system metadata
     * 
     * @param channelName
     *            the friendly name for the particular index into array (ie
     *            variable name with array being the variables)
     * @return integer between 0-65535 indexing into messaging array
     */
    public static int getChannel(ChannelName channelName) {
        switch(channelName) {
            case SEQ_UNIT_NUMBER:
                return Channels.SEQ_UNIT_NUMBER;
            case UNIT_COUNT_BASE:
                return Channels.UNIT_COUNT_BASE;
            case LAST_ATTACKED_COORDINATES:
                return Channels.LAST_ATTACKED_COORDINATES;
            case BEAVER_BUILD_REQUEST:
                return Channels.BEAVER_BUILD_REQUEST;
            case EFFECTIVE_MINERS_COUNT:
                return Channels.EFFECTIVE_MINERS_COUNT;
            case REQUEST_MAILBOX_BASE:
                return 16100;
            case REQUESTS_METADATA_BASE:
                return 17000;
            default:
                return -1;
        }
    }
    
    
    //Attack ==================================================================
    
    //RobotInfo array of nearby units
    public static RobotInfo[] enemies;
    public static RobotInfo[] friends;
    
    /**
     * Basic attack on the first of detected nearby enemies
     * @param enemies RobotInfo array of enemies in attack range
     * @throws GameActionException
     */
    public static void basicAttack(RobotInfo[] enemies) throws GameActionException {
        rc.attackLocation(enemies[0].location);
    }
    
    /**
     * Prioritized attack on the weakest of nearby enemies
     * @param enemies RobotInfo array of enemies in attack range
     * @param atkorder int array of attack priority rank (0 to 20, allowing ties) for each corresponding RobotType ordinal in robotTypes (eg: atkorder[1] = 5 means TOWERs are the 6th most important RobotType to attack)
     * @throws GameActionException
     */
    public static void priorityAttack(RobotInfo[] enemies, int[] atkorder) throws GameActionException {
        //Initiate
        int targetidx = -1, targettype = 1;
        double minhp = 100000;
        
        //Check for weakest of highest priority enemy type
        for(int i=0; i<enemies.length; i++) {
            int type = enemies[i].type.ordinal();
            myLocation = rc.getLocation();
            if(myLocation.distanceSquaredTo(enemies[i].location) <= attackRange) {
                
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
            //Attack selected target
            rc.attackLocation(enemies[targetidx].location);
        }
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
}