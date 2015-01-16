package team157;

import battlecode.common.*;
import team157.Utility.*;

public class Beaver extends MiningUnit {

    //Global variables =========================================================
    
    /**
     * The building to be built at moveTargetLocation
     * 
     * null if we aren't looking to build anything
     */
    public static RobotType buildingType = null;
    
    /**
    private static boolean stuck = false;
    private static MapLocation stuckLocation = myLocation;
    private static int stuckDistanceSquared = 9;
    private static MapLocation unstuckLocation;
    **/
    
    // General methods =========================================================
    
    public static void start() throws GameActionException {
        init();
        while (true) {
            loop();
            rc.yield(); // Yield the round
        }
    }

    private static void init() throws GameActionException {
        robotState = RobotState.WANDER;
        //initialSense(rc.getLocation());
    }
    
    
    private static void loop() throws GameActionException {
        //Update location
        myLocation = rc.getLocation();
        
        //Sense map
        //Must be before movement methods
        if(previousDirection != Direction.NONE) {
            senseWhenMove(myLocation, previousDirection);
            previousDirection = Direction.NONE;
        }
        
        // Code that runs in every robot (including buildings, excepting missiles)
        sharedLoopCode();
        //checkMailbox();

        //Sense nearby units
        updateEnemyInSight();

        //State machine -------------------------------------------------------
        //Switch states
        switch (robotState) {
            case WANDER: switchStateFromWanderState(); break;
            case MINE: switchStateFromMineState(); break;
        }
        
        //Display state
        rc.setIndicatorString(1, "In state: " + robotState);

        // Perform action based on state
        switch (robotState) {
            case ATTACK_MOVE: beaverAttack(); break;
            case WANDER: minerWander(); break;
            case MINE: beaverMine(); break;
            case BUILD: beaverBuild(); break;
            default: throw new IllegalStateException();
        }
        
    }
    
    
    //State switching =========================================================
    
    private static void switchStateFromWanderState() throws GameActionException {
        //check if need to build stuff
        buildingType = BeaversBuildRequest.doIHaveToBuildABuilding();
        if(buildingType != null) {
            BeaversBuildRequest.yesIWillBuildABuilding();
            robotState = RobotState.BUILD;
            moveTargetLocation = myLocation;
            //need to add response
        } else if (rc.isCoreReady()) {
            //Mine
            double ore = rc.senseOre(myLocation);
            double miningProbability = 1 - 1/(1+2.0*ore/(GameConstants.BEAVER_MINE_MAX*GameConstants.BEAVER_MINE_RATE));
            if(rand.nextDouble() <= miningProbability) {
                robotState = RobotState.MINE;
            }
        }
    }

    private static void switchStateFromMineState() throws GameActionException {
        
        //check if need to build stuff
        buildingType = BeaversBuildRequest.doIHaveToBuildABuilding();
        if(buildingType != null) {
            BeaversBuildRequest.yesIWillBuildABuilding();
            robotState = RobotState.BUILD;
            moveTargetLocation = myLocation;
            //need to add response
        } else if (Clock.getRoundNum() > 1750 && rc.getHealth() > 10 
            && RobotCount.read(RobotType.HANDWASHSTATION) < 10) {
            //Lategame handwash station attack
            robotState = RobotState.BUILD;
            moveTargetLocation = myLocation;
            buildingType = RobotType.HANDWASHSTATION;
        } else if (rc.isCoreReady()) {
            //Transition to wandering around if ore level is too low
            double ore = rc.senseOre(myLocation);
            double miningProbability = 1 - 1/(1+2.0*ore/(GameConstants.BEAVER_MINE_MAX*GameConstants.BEAVER_MINE_RATE));
            if(rand.nextDouble() > miningProbability) {
                robotState = RobotState.WANDER;
            }
        }
    }

    
    //State methods ===========================================================
    
    private static void beaverAttack() throws GameActionException {
        //Vigilance
        checkForEnemies();

        // Go to Enemy HQ
        bug(enemyHQLocation);

        //Distribute supply
        distributeSupply(suppliabilityMultiplier_Preattack);
    }

    private static void beaverMine() throws GameActionException {
        //Vigilance
        checkForEnemies();

        //Mine
        if (rc.getCoreDelay()< 1) rc.mine();

        //Distribute supply
        distributeSupply(suppliabilityMultiplier_Preattack);
    }


    /**
     * Move towards moveTargetLocation and builds if it is nearby.
     * @throws GameActionException
     */
    private static void beaverBuild() throws GameActionException {
        //Vigilance
        checkForEnemies();
        
        // Go closer to build location.
        // When the beaver is there, we cans start building immediately
        int distance = myLocation.distanceSquaredTo(moveTargetLocation);
        if(distance == 0) bug(HQLocation); //move next to build spot
        else if(distance > 2) bug(moveTargetLocation); //travel to build spot
        else {
            Direction dirToBuild = myLocation.directionTo(moveTargetLocation);
            if(rc.isCoreReady() && rc.hasBuildRequirements(buildingType) 
                && rc.canBuild(dirToBuild,buildingType)) {
                //Can build building
                rc.build(dirToBuild,buildingType);
                buildingType = null;
                robotState = RobotState.WANDER;
            }
        }
        
        //Distribute supply
        distributeSupply(suppliabilityMultiplier_Preattack);
    }
    

    //Other methods ===========================================================
    
    private static void idle() throws GameActionException {
        long request = Request.checkForRequest(myLocation.x, myLocation.y, RobotPlayer.myType.ordinal());
        if (request != 0)
               scoreRequest(request);
    }
    
    private static void checkMailbox() throws GameActionException {
        switch (Request.workerState) {
            case IDLE: idle(); break;
            case ON_JOB: break;
            case REQUESTING_JOB:
                if (Request.isJobClaimSuccessful()) {
                    handleRequest(Request.claimRequest);
                }
                break;
            default: break;
        }
    }
    
    
    /**
     * Check robot suitability for the request and score
     * @param request
     * @throws GameActionException
     */
    private static void scoreRequest(long request) throws GameActionException {
        switch (Request.claimJobType) {
        // TODO: Actual scoring
        default:
//            System.out.println("Beaver atttempts to claim job");
            Request.attemptToClaimJob(0);
        }
    }

    /**
     * Handles a request we're committed to, and updates internal variables as it 
     * does so.
     * @throws GameActionException 
     */
    private static void handleRequest(long request) throws GameActionException {
//        System.out.println("Beaver atttempts to handle job  " + Request.claimJobType);
//        System.out.println(Request.claimJobType & Request.JobType.BUILD_BUILDING_MASK);
        switch (Request.claimJobType) {
        case (Request.JobType.MOVE):
            // Immediately override
            // TODO: unpack x, y target coordinates
            robotState = RobotState.ATTACK_MOVE;
            break;
        default:
            if ((Request.claimJobType & Request.JobType.BUILD_BUILDING_MASK) != 0) {
//                System.out.println("Beaver sets buildingtype to  " + Request.getRobotType(request));
                buildingType = RobotPlayer.robotTypes[Request.getRobotType(request)];
            }
            break;
        }
    }

    //Build methods ===========================================================
    
    /**
     * Build robot of type rbtype in direction dir0 if allowed
     * @param dir0 Direction to build at
     * @param robotType RobotType of building to build
     * @throws GameActionException
     */
    public static void tryBuild(Direction dir0, RobotType robotType) throws GameActionException {
        if(rc.isCoreReady() && rc.hasBuildRequirements(robotType)) {
            int dirint0 = dir0.ordinal();
            for(int offset : offsets) {
                int dirint = (dirint0+offset+8)%8;
                if(rc.canBuild(directions[dirint],robotType)) {
                    rc.build(directions[dirint],robotType);
                    break;
                }
            }
        }
    }
    
    
    // Methods to build buildings - Josiah
    // Procedure to be used
    // 1) HQ broadcasts on specific radio channel the desired number of
    // buildings
    // 2) Buildings report their existence on each round to produce total count
    // 3) Beavers check for need to build any buildings, and claim the job to
    // build it
    // 4) Beaver builds building. (for now, on the spot)

    /**
     * checkAndRequestBuilding has to perform the following functions in a
     * single turn:
     * 
     * 1) check for need to build building <br>
     * 2) if building is needed, read off messaging array to check building
     * type, parameters (say, coordinates of build site), and score suitability<br>
     * 3) 
     * 
     * MUST COMPLETE IN ONE TURN OR CODE WILL BREAK (POSSIBLY GLOBALLY)
     */
    public static void checkAndRequestBuilding() {

    }

}
