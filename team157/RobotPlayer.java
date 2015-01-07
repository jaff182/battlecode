package team157;

import java.util.Random;

import battlecode.common.*;

public class RobotPlayer {
    
    //Global variables ========================================================
    //Unset Variables
    public static RobotController rc;
    public static MapLocation hqloc, enmloc, myloc; //locations
    public static MapLocation[] mytwrs, enmtwrs; //tower location arrays
    public static Team myteam, enmteam;
    public static RobotType mytype;
    public static int myrng; //range
    
    public static Random rand;
    public final static Direction[] dirs = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST}; //call dirs[i] for the ith direction
    public final static int[] offsets = {0,1,-1,2,-2,3,-3,4};
    public final static RobotType[] rbtypes = {RobotType.HQ,RobotType.TOWER,RobotType.SUPPLYDEPOT,RobotType.TECHNOLOGYINSTITUTE,RobotType.BARRACKS,RobotType.HELIPAD,RobotType.TRAININGFIELD,RobotType.TANKFACTORY,RobotType.MINERFACTORY,RobotType.HANDWASHSTATION,RobotType.AEROSPACELAB,RobotType.BEAVER,RobotType.COMPUTER,RobotType.SOLDIER,RobotType.BASHER,RobotType.MINER,RobotType.DRONE,RobotType.TANK,RobotType.COMMANDER,RobotType.LAUNCHER,RobotType.MISSILE}; //in order of ordinal
    
    //Internal map
    public static int mapx0, mapy0, symmetry=0;
    public static int[][] map = new int[122][122];
    
    // For pathing
    private static PathingState pathingState = PathingState.BUGGING;
    private static Direction previousDir = Direction.NORTH;
    private static int turnClockwise;
    private static int totalTurnOffset = 0;
    
    
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
        
        //internal map
        //This year's implementation randomizes offsets to the x,y coordinates
        //Coordinate offsets at map[60][60]
        mapx0 = (hqloc.x+enmloc.x)/2;
        mapy0 = (hqloc.y+enmloc.y)/2;
        //computeMap();
        
        
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
    
    
    //Map methods =============================================================
    
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
     * Sets value in internal map for MapLocation(xcoord,ycoord)
     */
    public static void setInternalMap(int xcoord, int ycoord, int value) {
        map[(182+xcoord-mapx0)%122][(182+ycoord-mapy0)%122] = value;
    }
    
    /**
     * Gets value in internal map for MapLocation(xcoord,ycoord)
     */
    public static int getInternalMap(int xcoord, int ycoord) {
        return map[(182+xcoord-mapx0)%122][(182+ycoord-mapy0)%122];
    }
    
    /**
     * Updates internal map with radio map value for MapLocation(xcoord,ycoord)
     */
    public static void updateInternalMap(int xcoord, int ycoord) throws GameActionException {
        int xidx = (182+xcoord-mapx0)%122;
        int yidx = (182+ycoord-mapy0)%122;
        map[xidx][yidx] = rc.readBroadcast(xidx*122+yidx+getChannel(ChannelName.MAP_DATA));
    }
    
    /**
     * Sets value in radio map for MapLocation(xcoord,ycoord)
     */
    public static void setRadioMap(int xcoord, int ycoord, int value) throws GameActionException {
        int xidx = (182+xcoord-mapx0)%122;
        int yidx = (182+ycoord-mapy0)%122;
        rc.broadcast(xidx*122+yidx+getChannel(ChannelName.MAP_DATA), value);
    }
    
    /**
     * Gets value in radio map for MapLocation(xcoord,ycoord)
     */
    public static int getRadioMap(int xcoord, int ycoord) throws GameActionException {
        int xidx = (182+xcoord-mapx0)%122;
        int yidx = (182+ycoord-mapy0)%122;
        return rc.readBroadcast(xidx*122+yidx+getChannel(ChannelName.MAP_DATA));
    }
    
    /**
     * Updates radio map with internal map value for MapLocation(xcoord,ycoord)
     */
    public static void updateRadioMap(int xcoord, int ycoord) throws GameActionException {
        int xidx = (182+xcoord-mapx0)%122;
        int yidx = (182+ycoord-mapy0)%122;
        rc.broadcast(xidx*122+yidx+getChannel(ChannelName.MAP_DATA), map[xidx][yidx]);
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
     * 1-14884 - global shared map data<br>
     * 16001 - reserved <br>
     * 16002-16010 - number of buildings of different types currently built
     * (read on even round number, write on odd rounds)<br>
     * 16012-16020 - number of buildings of different types currently built
     * (read on odd round number, write on even rounds)<br>
     * 17001-21000 - reserved, possible unit command mechanism
     * 21001-25000 - reserved, possible unit response mechanism
     * 
     * @param chnlname
     *            the friendly name for the particular index into array (ie
     *            variable name with array being the variables)
     * @return integer between 0-65535 indexing into messaging array
     */
    public static int getChannel(ChannelName chnlname) {
        switch(chnlname) {
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
    
    
    
    //Spawn/Build =============================================================
    
    /**
     * Spawn robot of type rbtype in direction dir0 if allowed, transfers supply
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