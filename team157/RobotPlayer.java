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
    public final static RobotType[] rbtypes = {RobotType.HQ,RobotType.TOWER,RobotType.SUPPLYDEPOT,RobotType.TECHNOLOGYINSTITUTE,RobotType.BARRACKS,RobotType.HELIPAD,RobotType.TRAININGFIELD,RobotType.TANKFACTORY,RobotType.MINERFACTORY,RobotType.HANDWASHSTATION,RobotType.AEROSPACELAB,RobotType.BEAVER,RobotType.COMPUTER,RobotType.SOLDIER,RobotType.BASHER,RobotType.MINER,RobotType.DRONE,RobotType.TANK,RobotType.COMMANDER,RobotType.LAUNCHER,RobotType.MISSILE}; //in order of ordinal
    
    //Internal map
    public static int[][] map;
	
    
	//Main script =============================================================
	public static void run(RobotController RC) {
        
        //Global inits --------------------------------------------------------
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
        
        //RobotType specific methods ------------------------------------------
		try {
            switch(mytype) {
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
    
    
    //Comms ===================================================================
	
	/**
	 * Names of channels. See javadoc for getchnl for more information.
	 * 
	 * Feel free to register and append your own names
	 * 
	 * @author Josiah
	 *
	 */
	public enum ChannelName {
	    MAP_SYMMETRY, MAP_DATA,
	    BARRACKS, TECHINST, HELIPAD, MINERFACTORY
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
    public static int getchnl(ChannelName chnlname) {
        switch(chnlname) {
            case MAP_SYMMETRY:
                return 0;
            case MAP_DATA:
                return 1; //14400 channels from 1 to 14400
            case BARRACKS:
                return 14500;
            case TECHINST:
                return 14600;
            case HELIPAD:
                return 14700;
            case MINERFACTORY:
                return 14800;
            default:
                return -1;
        }
    }
    
    
    //Movement ================================================================
    
    /**
     * Primitive pathing to target location.
     * @param target Destination location
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
     * @param atkorder int array of attack priority rank (0 to 20, allowing ties) for each corresponding RobotType ordinal in rbtypes (eg: atkorder[1] = 5 means TOWERs are the 6th most important RobotType to attack)
     * @throws GameActionException
     */
    public static void priorityAttack(RobotInfo[] enemies, int[] atkorder) throws GameActionException {
        int targetidx = -1, targettype = 21;
        double minhp = 100000;
        
        for(int i=0; i<enemies.length; i++) {
            int type = enemies[i].type.ordinal();
            if(atkorder[type] < atkorder[targettype]) {
                targettype = enemies[i].type.ordinal();
                minhp = enemies[i].health;
                targetidx = i;
            else if(atkorder[type] == atkorder[targettype] && enemies[i].health < minhp) {
                minhp = enemies[i].health;
                targetidx = i;
            }
        }
        if (targetidx != -1) {
            rc.attackLocation(enemies[targetidx].location);
        }
	}
    
    
    
    //Spawn/Build =============================================================
    
    /**
     * Spawn robot of type rbtype in direction dir0 if allowed
     * @param dir0 Direction to spawn at
     * @param rbtype RobotType of robot to spawn
     * @throws GameActionException
     */
    public static void trySpawn(Direction dir0, RobotType rbtype) throws GameActionException {
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
    
    
    /**
     * Build robot of type rbtype in direction dir0 if allowed
     * @param dir0 Direction to build at
     * @param rbtype RobotType of building to build
     * @throws GameActionException
     */
    public static void tryBuild(Direction dir0, RobotType rbtype) throws GameActionException {
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
    
    
    //Tests ===================================================================
    
    /**
     * Check RobotType ordinal agreement with order in rbtypes
     */
    public static void checkRobotTypeOrdinal() {
        for(int i=0; i<=20; i++) {
            assert rbtypes[i].ordinal() == i : rbtypes[i];
        }
        System.out.println("RobotType Ordinal Test passed.");
    }
    
    
}