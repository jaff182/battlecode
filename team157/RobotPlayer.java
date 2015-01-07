package team157;

import java.util.Random;
import battlecode.common.*;

public class RobotPlayer {
    
	//Global variables ========================================================
    public static RobotController rc;
	public static MapLocation hqloc, enmloc, myloc; //locations
    public static MapLocation[] mytwrs, enmtwrs; //tower location arrays
	public static Team myteam, enmteam;
    public static RobotType mytype;
    public static int myrng; //range
    
	public static Random rand;
	public final static Direction[] dirs = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST}; //call dirs[i] for the ith direction
    public final static int[] offsets = {0,1,-1,2,-2,3};
    
    //Internal map
    public static int[][] map;
	
    
	//Main script =============================================================
	public static void run(RobotController RC) {
        
        //GLOBAL INITS --------------------------------------------------------
        rc = RC; //set robotcontroller
        rand = new Random(rc.getID()); //seed random number generator
        
        //my properties
        mytype = rc.getType();
        myrng = mytype.attackRadiusSquared;
        
        //sense locations
        hqloc = rc.senseHQLocation();
        enmloc = rc.senseEnemyHQLocation();
        mytwrs = rc.senseTowerLocations();
        enmtwrs = rc.senseEnemyTowerLocations();
        myloc = rc.getLocation();
        
        //get teams
        myteam = rc.getTeam();
        enmteam = myteam.opponent();
        
        
		try {
            switch(mytype) {
                //HQ
                case HQ:
                    HQ.start();
                    break;
                case BEAVER:
                    Beaver.start();
                    break;
                case AEROSPACELAB:
                    AerospaceLab.start();
                    break;
                case BARRACKS:
                    Barracks.start();
                    break;
                case BASHER:
                    Basher.start();
                    break;
                case COMMANDER:
                    Commander.start();
                    break;
                case COMPUTER:
                    Computer.start();
                    break;
                case DRONE:
                    Drone.start();
                    break;
                case HANDWASHSTATION:
                    HandwashStation.start();
                    break;
                case HELIPAD:
                    Helipad.start();
                    break;
                case LAUNCHER:
                    Launcher.start();
                    break;
                case MINER:
                    Miner.start();
                    break;
                case MINERFACTORY:
                    MinerFactory.start();
                    break;
                case MISSILE:
                    Missile.start();
                    break;
                case SOLDIER:
                    Soldier.start();
                    break;
                case SUPPLYDEPOT:
                    SupplyDepot.start();
                    break;
                case TANK:
                    Tank.start();
                    break;
                case TANKFACTORY:
                    TankFactory.start();
                    break;
                case TECHNOLOGYINSTITUTE:
                    TechnologyInstitute.start();
                    break;
                case TOWER:
                    Tower.start();
                    break;
                case TRAININGFIELD:
                    TrainingField.start();
                    break;
            }
            
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
    
    
    
    //Comms ===================================================================
	
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
     * 1-14400 - global shared map data<br>
     * 15001 - indices of dirty variables in 15002-1520 (non-zero when buildings
     * are to be built right now)<br>
     * 15002-15010 - target number of buildings to be built (read on even round
     * number, write on odd rounds)<br>
     * 15012-15020 - target number of buildings to be built (read on odd round
     * number, write on even rounds)<br>
     * 
     * @param chnlname
     *            the friendly name for the particular index into array (ie
     *            variable name with array being the variables)
     * @return integer between 0-65535 indexing into messaging array
     */
    public static int getchnl(String chnlname) {
        switch(chnlname) {
            case "symmetry":
                return 0;
            case "map":
                return 1; //14400 channels from 1 to 14400
            case "barracks":
                return 14500;
            case "techinst":
                return 14600;
            case "helipad":
                return 14700;
            case "minerfactory":
                return 14800;
            default:
                return -1;
        }
    }
    
    
    //Pathing =================================================================
    
    /**
     * Primitive pathing to target location.
     * @param target
     * @throws GameActionException
     */
    static void walk(MapLocation target) throws GameActionException {
        if(rc.isCoreReady()) {
            myloc = rc.getLocation();
            int dirInt = myloc.directionTo(target).ordinal();
            int offsetIndex = 0;
            while (offsetIndex < 5 && !rc.canMove(dirs[(dirInt+offsets[offsetIndex]+8)%8])) {
                offsetIndex++;
            }
            if (offsetIndex < 5) {
                rc.move(dirs[(dirInt+offsets[offsetIndex]+8)%8]);
            }
        }
    }
    
    
    //Attack ==================================================================
    
    //Basic attack on nearby enemies
    public static void basicatk() throws GameActionException {
        if(rc.isWeaponReady()) {
            RobotInfo[] enemies = rc.senseNearbyRobots(myrng, enmteam);
            if (enemies.length > 0) {
                rc.attackLocation(enemies[0].location);
            }
        }
	}
    
    
    
    //Spawn/Build =============================================================
    
    //Spawn robot of type rbtype in direction dir0 if allowed
    public static void tryspawn(Direction dir0, RobotType rbtype) throws GameActionException {
        if(rc.isCoreReady() && rc.getTeamOre() >= rbtype.oreCost) {
            int dirint0 = dir0.ordinal();
            for(int offset : offsets) {
                int dirint = (dirint0+offset+8)%8;
                if(rc.canSpawn(dirs[dirint],rbtype)) {
                    rc.spawn(dirs[dirint],rbtype);
                    break;
                }
            }
        }
    }
    
    //Build robot of type rbtype in direction dir0 if allowed
    public static void trybuild(Direction dir0, RobotType rbtype) throws GameActionException {
        if(rc.isCoreReady() && rc.getTeamOre() >= rbtype.oreCost) {
            int dirint0 = dir0.ordinal();
            for(int offset : offsets) {
                int dirint = (dirint0+offset+8)%8;
                if(rc.canBuild(dirs[dirint],rbtype)) {
                    rc.build(dirs[dirint],rbtype);
                    break;
                }
            }
        }
    }
    
    
    
}