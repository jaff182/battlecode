package team157;

import java.util.Random;

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
        myLocation = rc.getLocation();
        
        //get teams
        myTeam = rc.getTeam();
        enemyTeam = myTeam.opponent();
        
        //internal map
        //This year's implementation randomizes offsets to the x,y coordinates
        //Coordinate offsets at map[60][60]
        mapx0 = (HQLocation.x+enemyHQLocation.x)/2;
        mapy0 = (HQLocation.y+enemyHQLocation.y)/2;
        //computeMap();
        
        try {
            
            //set countingID for messaging (WARNING, ASSUMES MESSAGING ARRAY IS INITIALIZED TO ZERO)
            if(myType != RobotType.MISSILE) {
                countingID = rc.readBroadcast(getChannel(ChannelName.SEQ_UNIT_NUMBER));
                rc.broadcast(getChannel(ChannelName.SEQ_UNIT_NUMBER), countingID+1);
            }
            
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
    
    public static int mapx0, mapy0, symmetry=0;
    public static int allocatedWidth = GameConstants.MAP_MAX_WIDTH+2;
    public static int allocatedHeight = GameConstants.MAP_MAX_HEIGHT+2;
    public static int[][] map = new int[allocatedWidth][allocatedHeight];
    
    /**
     * Internal map is toroidal and approximately centered at midpoint of HQs.
     * Map representation modulo 6:
     *  0: unknown,  1: nonvoid,  2: enemy HQ,  3: HQ,  4: void,  5: out of map
     * Symmetry representation:
     *  0: unknown,  1: rotational,  2: x-reflection,  3: y-reflection
     */
    public static void computeMap() {
        
        
    }
    
    /**
     * Sets value in internal map
     * @param loc MapLocation to set value.
     */
    public static void setInternalMap(MapLocation loc, int value) {
        int xidx = (3*allocatedWidth/2+loc.x-mapx0)%allocatedWidth;
        int yidx = (3*allocatedHeight/2+loc.y-mapy0)%allocatedHeight;
        map[yidx][xidx] = value;
    }
    
    /**
     * Gets value in internal map
     * @param loc MapLocation to get value.
     * @return map value
     */
    public static int getInternalMap(MapLocation loc) {
        int xidx = (3*allocatedWidth/2+loc.x-mapx0)%allocatedWidth;
        int yidx = (3*allocatedHeight/2+loc.y-mapy0)%allocatedHeight;
        return map[yidx][xidx];
    }
    
    /**
     * Updates internal map with radio map value
     * @param loc MapLocation to update value.
     */
    public static void updateInternalMap(MapLocation loc) throws GameActionException {
        int xidx = (3*allocatedWidth/2+loc.x-mapx0)%allocatedWidth;
        int yidx = (3*allocatedHeight/2+loc.y-mapy0)%allocatedHeight;
        map[yidx][xidx] = rc.readBroadcast(xidx*allocatedHeight+yidx+getChannel(ChannelName.MAP_DATA));
    }
    
    /**
     * Sets value in radio map
     * @param loc MapLocation to set value.
     */
    public static void setRadioMap(MapLocation loc, int value) throws GameActionException {
        int xidx = (3*allocatedWidth/2+loc.x-mapx0)%allocatedWidth;
        int yidx = (3*allocatedHeight/2+loc.y-mapy0)%allocatedHeight;
        rc.broadcast(xidx*allocatedHeight+yidx+getChannel(ChannelName.MAP_DATA), value);
    }
    
    /**
     * Gets value in radio map
     * @param loc MapLocation to get value.
     * @return map value
     */
    public static int getRadioMap(MapLocation loc) throws GameActionException {
        int xidx = (3*allocatedWidth/2+loc.x-mapx0)%allocatedWidth;
        int yidx = (3*allocatedHeight/2+loc.y-mapy0)%allocatedHeight;
        return rc.readBroadcast(xidx*allocatedHeight+yidx+getChannel(ChannelName.MAP_DATA));
    }
    
    /**
     * Updates radio map with internal map value
     * @param loc MapLocation to update value.
     */
    public static void updateRadioMap(MapLocation loc) throws GameActionException {
        int xidx = (3*allocatedWidth/2+loc.x-mapx0)%allocatedWidth;
        int yidx = (3*allocatedHeight/2+loc.y-mapy0)%allocatedHeight;
        rc.broadcast(xidx*122+yidx+getChannel(ChannelName.MAP_DATA), map[yidx][xidx]);
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
        MAP_SYMMETRY, MAP_DATA,
        BARRACKS, TECHINST, HELIPAD, MINERFACTORY,
        SEQ_UNIT_NUMBER
    }
    
    /**
     * Gets the index into the messaging array.
     * 
     * The messaging array is a size-65536 array of ints that can be broadcasted
     * to and read from by almost all Robots.
     * 
     * See "Messaging [bcd09]" from the spec document
     * 
     * Allocations:<br>
     * 0 - type of symmetry of map (rotational, type)<br>
     * 1-14884 - global shared map data<br>
     * 16001 - number of units produced since start of game by you (including towers, HQ) <br>
     * 16002-16010 - number of buildings of different types currently built
     * (read on even round number, write on odd rounds)<br>
     * 16012-16020 - number of buildings of different types currently built
     * (read on odd round number, write on even rounds)<br>
     * 17001-21000 - reserved, possible unit command mechanism
     * 21001-25000 - reserved, possible unit response mechanism
     * 
     * @param channelName
     *            the friendly name for the particular index into array (ie
     *            variable name with array being the variables)
     * @return integer between 0-65535 indexing into messaging array
     */
    public static int getChannel(ChannelName channelName) {
        switch(channelName) {
            case MAP_SYMMETRY:
                return 0;
            case MAP_DATA:
                return 1; //14884 channels from 1 to 14884
            case BARRACKS:
                return 15500;
            case TECHINST:
                return 15600;
            case HELIPAD:
                return 15700;
            case MINERFACTORY:
                return 15800;
            case SEQ_UNIT_NUMBER:
                return 16001;
            default:
                return -1;
        }
    }
    
    
    //Attack ==================================================================
    
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
        int targetidx = -1, targettype = 21;
        double minhp = 100000;
        
        for(int i=0; i<enemies.length; i++) {
            int type = enemies[i].type.ordinal();
            if(atkorder[type] < targettype) {
                targettype = atkorder[type];
                minhp = enemies[i].health;
                targetidx = i;
            } else if(atkorder[type] == targettype && enemies[i].health < minhp) {
                minhp = enemies[i].health;
                targetidx = i;
            }
        }
        if (targetidx != -1) {
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
    
    
}