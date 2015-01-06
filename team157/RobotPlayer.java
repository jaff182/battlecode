package team157;

import java.util.Random;
import battlecode.common.*;

public class RobotPlayer {
    
	//Global variables ========================================================
    public static RobotController rc;
	public static MapLocation hqloc, enemloc, myloc; //locations
    public static MapLocation[] mytwrs, enemtwrs; //tower location arrays
	public static Team mytm, enemtm; //teams
    public static RobotType mytype;
    public static int myrng; //range
    
	public static Random rand;
	public final static Direction[] dirs = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST}; //call dirs[i] for the ith direction
	

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
        enemloc = rc.senseEnemyHQLocation();
        mytwrs = rc.senseTowerLocations();
        enemtwrs = rc.senseEnemyTowerLocations();
        myloc = rc.getLocation();
        
        //get teams
        mytm = rc.getTeam();
        enemtm = mytm.opponent();
        
        
		try {
            switch(mytype) {
                case HQ:
                    hq.start();
                    break;
                case BEAVER:
                    beaver.start();
                    break;
            }
            
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
    
    //Basic attack on nearby enemies
    static void atk() throws GameActionException {
		RobotInfo[] enemies = rc.senseNearbyRobots(myrng, enemtm);
		if (enemies.length > 0) {
			rc.attackLocation(enemies[0].location);
		}
	}
    

}
