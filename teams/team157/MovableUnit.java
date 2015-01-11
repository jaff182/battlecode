package team157;

import java.util.Random;

import battlecode.common.*;

public class MovableUnit extends RobotPlayer {
    
    
    
    //Movement ================================================================
    
    // For pathing
    private static PathingState pathingState = PathingState.BUGGING;
    private static int turnClockwise;
    private static int totalOffsetDir = 0;
    private static Direction obstacleDir = Direction.NORTH;
    
    // Robot overall state ====================================================
    // Only modify these variables before and after loop(), at clearly specified locations
    /**
     * State of robot. Some states are invalid in certain robots. Mark clearly which will be used.
     */
    public static RobotState robotState = RobotState.WANDER;
    
    /**
     * If robot is set to move (ADVANCE, RETREAT), this is where it will go
     */
    public static MapLocation moveTargetLocation = enemyHQLocation;
    
    /**
     * Sense terrain while moving.
     * @param dir Direction of movement 
     * @throws GameActionException
     */
    public static void moveSense(Direction dir) throws GameActionException {
        rc.move(dir);
        previousDirection = dir;
    }
    
    /**
     * Primitive pathing to target location, with no knowledge of terrain.
     * @param target Destination location
     * @throws GameActionException
     */
    public static void explore(MapLocation target) throws GameActionException {
        if(rc.isCoreReady()) {
            myLocation = rc.getLocation();
            int dirInt = myLocation.directionTo(target).ordinal();
            int offsetIndex = 0;
            while (offsetIndex < 5 && !rc.canMove(directions[(dirInt+offsets[offsetIndex]+8)%8])) {
                offsetIndex++;
            }
            if (offsetIndex < 5) {
                Direction dirToMove = directions[(dirInt+offsets[offsetIndex]+8)%8];
                rc.move(dirToMove);
                previousDirection = dirToMove;
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
            myLocation = rc.getLocation();
            int dirInt = myLocation.directionTo(target).ordinal() + rand.nextInt(5)-2;
            int offsetIndex = 0;
            while (offsetIndex < 5 && !rc.canMove(directions[(dirInt+offsets[offsetIndex]+8)%8])) {
                offsetIndex++;
            }
            if (offsetIndex < 5) {
                Direction dirToMove = directions[(dirInt+offsets[offsetIndex]+8)%8];
                rc.move(dirToMove);
                previousDirection = dirToMove;
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
            while (offsetIndex < 8 && !rc.canMove(directions[(dirInt+offsets[offsetIndex]+8)%8])) {
                offsetIndex++;
            }
            if (offsetIndex < 8) {
                Direction dirToMove = directions[(dirInt+offsets[offsetIndex]+8)%8];
                rc.move(dirToMove);
                previousDirection = dirToMove;
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
                rc.setIndicatorString(1,"bug " + targetDir);
                if (rc.canMove(targetDir)) {
                    // target is not blocked
                    moveSense(targetDir);
                } else {
                    // target is blocked, move clockwise/counterclockwise around obstacle
                    pathingState = PathingState.HUGGING;
                    totalOffsetDir = 0;
                    obstacleDir = targetDir;
                    
                    // Choose direction to hug in
                    int dirInt = targetDir.ordinal();
                    int offsetIndex = rand.nextInt(5);
                    while (offsetIndex < 8 && !rc.canMove(directions[(dirInt+offsets[offsetIndex]+8)%8])) {
                        offsetIndex++;
                    }
                    if (offsetIndex < 8) {
                        int offset = offsets[offsetIndex];
                        turnClockwise = offset/Math.abs(offset);
                        hug(obstacleDir, turnClockwise);
                    }
                }
            } else {
                if (rc.canMove(obstacleDir)) {
                    moveSense(obstacleDir);
                    pathingState = PathingState.BUGGING;
                } else if (Math.abs(totalOffsetDir) > 24) { //TODO
                    pathingState = PathingState.BUGGING;
                    //System.out.println("bug around");
                } else {
                    hug(obstacleDir, turnClockwise);
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
        rc.setIndicatorString(1, "HUG " + obstacleDir);
        int ordinalOffset = turnClockwise;
        Direction nextDir = obstacleDir;
        while (Math.abs(ordinalOffset) < 8 && !rc.canMove(nextDir)) {
            ordinalOffset += turnClockwise;
            nextDir = directions[(obstacleDir.ordinal()+ordinalOffset+8)%8];
        }
        if (Math.abs(ordinalOffset) < 8) {
            moveSense(nextDir);
            obstacleDir = directions[(nextDir.ordinal()-turnClockwise+8)%8];
            totalOffsetDir += ordinalOffset;
        }
    }
    
    
    //Move and sense ==========================================================
    
    //Sensing variables    
    // Wx = Ny, Ey = Nx, Sx = Nx, Ex = Sy, Wy = Nx
    private final static int[] senseNx = { 0, 1,-1, 2,-2, 3,-3, 4,-4};
    private final static int[] senseNy = {-5,-5,-5,-5,-5,-4,-4,-3,-3};
    private final static int[] senseSy = { 5, 5, 5, 5, 5, 4, 4, 3, 3};
    // NEx = SEy, SEx = SWy, SWx = NWy, NEy = NWx
    private final static int[] senseNWx = {-4,-4,-3,-5,-3,-5,-2,-5,-1,-5, 0,-5, 1};
    private final static int[] senseNWy = {-4,-3,-4,-3,-5,-2,-5,-1,-5, 0,-5, 1,-5};
    private final static int[] senseSEx = { 4, 4, 3, 5, 3, 5, 2, 5, 1, 5, 0, 5,-1};
    private final static int[] senseSEy = { 4, 3, 4, 3, 5, 2, 5, 1, 5, 0, 5,-1, 5};
    
    //Previously moved direction
    public static Direction previousDirection = Direction.NONE;
    
    /**
     * Update internal and radio map after moving once in specified direction.
     * @param robotLoc location of robot
     * @param dir Direction of movement.
     * @throws GameActionException
     */
    public static void senseWhenMove(MapLocation robotLoc, Direction dir) throws GameActionException {
        int i = 0;
        if(dir.ordinal()%2 == 0) {
            while(i < 9 && Clock.getBytecodesLeft() > 3000) {
                switch (dir) {
                    // Wx = Ny, Ey = Nx, Sx = Nx, Ex = Sy, Wy = Nx
                    case NORTH: senseMap(robotLoc.add(senseNx[i], senseNy[i])); break;
                    case EAST: senseMap(robotLoc.add(senseSy[i], senseNx[i])); break;
                    case SOUTH: senseMap(robotLoc.add(senseNx[i], senseSy[i])); break;
                    case WEST: senseMap(robotLoc.add(senseNy[i], senseNx[i])); break;
                    default: break;
                }
                i++;
            }
        } else {
            while(i < 13 && Clock.getBytecodesLeft() > 3000) {
                switch (dir) {
                    // NEx = SEy, SEx = SWy, SWx = NWy, NEy = NWx
                    case NORTH_WEST:
                        senseMap(robotLoc.add(senseNWx[i], senseNWy[i]));
                        break;
                    case NORTH_EAST:
                        senseMap(robotLoc.add(senseSEy[i], senseNWx[i]));
                        break;
                    case SOUTH_EAST:
                        senseMap(robotLoc.add(senseSEx[i], senseSEy[i]));
                        break;
                    case SOUTH_WEST:
                        senseMap(robotLoc.add(senseNWy[i], senseSEx[i]));
                        break;
                    default: break;
                }
                i++;
            }
        }
    }
    
    
    
    
    //Distribute Supply =======================================================
    
    /**
     * Distribute supply among neighboring units, according to health/supplyUpkeep. 
     * Primarily used by a temporary holder of supply, eg beaver/soldier.
     * @param multiplier Double array of multipliers to supply capacity per unit health of each RobotType.
     * @throws GameActionException
     */
    public static void distributeSupply(double[] multiplier) throws GameActionException {
        if(4000 - Clock.getBytecodeNum() >  500) {
            //Sense nearby friendly robots
            RobotInfo[] friends = rc.senseNearbyRobots(GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED,myTeam);
            if(friends.length > 0) {
                int targetidx = -1;
                double mycapacity = rc.getHealth()*multiplier[rc.getType().ordinal()];
                double totalsupply = rc.getSupplyLevel(), totalcapacity = mycapacity;
                double minsupplyratio = 10000000;
                
                for(int i=0; i<friends.length; i++) {
                    //Keep track of total values to find mean later
                    totalsupply += friends[i].supplyLevel;
                    totalcapacity += friends[i].health/(1+0.2*friends[i].type.supplyUpkeep);
                    
                    //Find robot with lowest supply per capacity
                    double supplyratio = friends[i].supplyLevel*(1+0.2*friends[i].type.supplyUpkeep)/friends[i].health;
                    if(supplyratio < minsupplyratio) {
                        minsupplyratio = supplyratio;
                        targetidx = i;
                    }
                }
                
                //Transfer half of excess supply above mean
                double meansupply = totalsupply/totalcapacity*mycapacity;
                if(targetidx != -1 && rc.getSupplyLevel() > meansupply) {
                    MapLocation loc = friends[targetidx].location;
                    rc.transferSupplies((int)(rc.getSupplyLevel()-meansupply)/2,loc);
                }
            }
        }
    }
    
}