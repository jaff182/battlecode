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
            }
            
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
    
    
    
    //Comms ===================================================================
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
        myloc = rc.getLocation();
        int dirInt = directionToInt(myloc.directionTo(target));
        int offsetIndex = 0;
        
        while (offsetIndex < 5 && !rc.canMove(dirs[(dirInt+offsets[offsetIndex]+8)%8])) {
            offsetIndex++;
        }
        if (offsetIndex < 5) {
            rc.move(dirs[(dirInt+offsets[offsetIndex]+8)%8]);
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
