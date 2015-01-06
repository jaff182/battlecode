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
            case "map":
                return 0; //10000 channels from 0 to 9999
            default:
                return -1;
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
            int dirint0 = directionToInt(dir0);
            for(int offset : offsets) {
                int dirint = (dirint0+offset+8)%8;
                if(rc.canSpawn(dirs[dirint],rbtype)) {
                    rc.spawn(dirs[dirint],rbtype);
                    break;
                }
            }
        }
    }
    
    
    //So ugly!!! =============================================================
    public static int directionToInt(Direction d) {
		switch(d) {
			case NORTH:
				return 0;
			case NORTH_EAST:
				return 1;
			case EAST:
				return 2;
			case SOUTH_EAST:
				return 3;
			case SOUTH:
				return 4;
			case SOUTH_WEST:
				return 5;
			case WEST:
				return 6;
			case NORTH_WEST:
				return 7;
			default:
				return -1;
		}
	}
}
