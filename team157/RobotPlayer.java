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
    public final static int[] offsets = {0,1,-1,2,-2,3,-3,4};
    
    //Internal map
    public static int[][] map;
    
    // For pathing
    private static PathingState pathingState = PathingState.BUGGING;
    private static Direction previousDir = Direction.NORTH;
    private static int turnClockwise;
    private static int totalTurnOffset = 0;
    
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
     * Primitive pathing to target location, with no knowledge of terrain.
     * @param target
     * @throws GameActionException
     */
    public static void explore(MapLocation target) throws GameActionException {
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
    
    /**
     * Primitive pathing with randomness to target location, with no knowledge of terrain.
     * @param target
     * @throws GameActionException
     */
    public static void exploreRandom(MapLocation target) throws GameActionException {
        if(rc.isCoreReady()) {
            myloc = rc.getLocation();
            int dirInt = myloc.directionTo(target).ordinal() + rand.nextInt(5)-2;
            int offsetIndex = 0;
            while (offsetIndex < 5 && !rc.canMove(dirs[(dirInt+offsets[offsetIndex]+8)%8])) {
                offsetIndex++;
            }
            if (offsetIndex < 5) {
                rc.move(dirs[(dirInt+offsets[offsetIndex]+8)%8]);
            }
        }
    }
    
    /**
     * Move around randomly.
     * @throws GameActionException
     */
    public static void wander() throws GameActionException {
        if(rc.isCoreReady()) {
            int dirInt = rand.nextInt(8);
            int offsetIndex = 0;
            while (offsetIndex < 8 && !rc.canMove(dirs[(dirInt+offsets[offsetIndex]+8)%8])) {
                offsetIndex++;
            }
            if (offsetIndex < 8) {
                rc.move(dirs[(dirInt+offsets[offsetIndex]+8)%8]);
            }
        }
    }
    
    /**
     * Basic bugging around obstacles
     * @param target
     * @throws GameActionException
     */
    public static void bug(MapLocation target) throws GameActionException {
        if (rc.isCoreReady()) {
            if (pathingState == PathingState.BUGGING) {
                Direction targetDir = rc.getLocation().directionTo(target);
                if (rc.canMove(targetDir)) {
                    // target is not blocked
                    rc.move(targetDir);
                } else {
                    // target is blocked, move clockwise/counterclockwise around obstacle
                    pathingState = PathingState.HUGGING;
                    turnClockwise = rand.nextInt(2)*2 - 1;
                    totalTurnOffset = 0;
                    hug(targetDir, turnClockwise);
                }
            } else {
                if (totalTurnOffset == 8) {
                    pathingState = PathingState.BUGGING;
                } else if (rc.canMove(previousDir)) {
                    pathingState = PathingState.BUGGING;
                    rc.move(previousDir);
                } else if (totalTurnOffset > 12) {
                    // robot turns one whole round but still does not clear obstacle
                    turnClockwise *= -1; // bug in opposite direction  
                    hug(previousDir, turnClockwise);
                } else {
                    hug(previousDir, turnClockwise);
                }
            }       
        }
    }
    
    /**
     * Helper method to bug, hugs around obstacle in obstacleDir.
     * @param obstacleDir direction of obstacle
     * @param turnClockwise 1 if robot should go clockwise around obstacle, -1 if robot should go counterclockwise.
     * @throws GameActionException
     */
    private static void hug(Direction obstacleDir, int turnClockwise) throws GameActionException {
        int ordinalOffset = turnClockwise;
        while (Math.abs(ordinalOffset) < 8 && !rc.canMove(dirs[(obstacleDir.ordinal()+ordinalOffset+8)%8])) {          
            ordinalOffset += turnClockwise;
        }
        if (Math.abs(ordinalOffset) < 8) {
            totalTurnOffset += ordinalOffset;
            rc.move(dirs[(obstacleDir.ordinal()+ordinalOffset+8)%8]);
            // offset previousDir by 2 to point towards obstacle
            previousDir = dirs[(obstacleDir.ordinal()+ordinalOffset-2*turnClockwise + 8)%8];
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