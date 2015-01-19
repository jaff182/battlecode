package team157;

import battlecode.common.*;
import team157.Utility.*;

public class Beaver extends MiningUnit {

    //Global variables =========================================================
    
    /**
     * Stores the checkerboard parity of the HQ, structures must be built on tiles 
     * with the same parity.
     */
    private static int CHECKERBOARD_PARITY;
    
    /**
     * The building to be built at moveTargetLocation. Null if we aren't looking to 
     * build anything.
     */
    private static RobotType buildingType = null;
    
    /**
     * Stores the build order index for updating the id of the structure.
     */
    private static int buildOrderIndex = -1;
    
    /**
     * Stores the ID of the building currently being built.
     */
    private static int buildingID = 0;
    
    
    
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
        
        //Set mining parameters
        MIN_MINING_RATE = GameConstants.BEAVER_MINE_MAX;
        MIN_ORE_WORTH_MINING = MIN_MINING_RATE*GameConstants.BEAVER_MINE_RATE;
        MIN_ORE_WORTH_CONSIDERING = GameConstants.MINIMUM_MINE_AMOUNT*GameConstants.BEAVER_MINE_RATE;
        
        //Set building parameters
        CHECKERBOARD_PARITY = (HQLocation.x+HQLocation.y)%2;
        
        //set locations within attack radius of enemy tower or hq as unpathable
        initInternalMap();
        
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
        //Switch state
        switch (robotState) {
            case WANDER: switchStateFromWanderState(); break;
            case MINE: switchStateFromMineState(); break;
        }
        
        //Display state
        rc.setIndicatorString(1, "In state: " + robotState);

        // Perform action based on state
        switch (robotState) {
            case ATTACK_MOVE: beaverAttack(); break;
            case WANDER: beaverWander(); break;
            case MINE: beaverMine(); break;
            case BUILD: beaverBuild(); break;
            default: throw new IllegalStateException();
        }
        
    }
    
    
    //State switching =========================================================
    
    private static void switchStateFromWanderState() throws GameActionException {
        //check if need to build stuff
        int index = BuildOrder.doIHaveToBuildABuilding();
        if(index != -1) {
                //Respond
                buildOrderIndex = index;
                BuildOrder.IAmTheBuilding(buildOrderIndex);
                int value = BuildOrder.get(buildOrderIndex);
                buildingType = robotTypes[BuildOrder.decodeTypeOrdinal(value)];
                moveTargetLocation = null;
                robotState = RobotState.BUILD;
        } else if (rc.isCoreReady()) {
            //Mine
            double ore = rc.senseOre(myLocation);
            double miningProbability = 0.5*(ore-MIN_ORE_WORTH_CONSIDERING)/(MIN_ORE_WORTH_MINING-MIN_ORE_WORTH_CONSIDERING);
            if(ore >= MIN_ORE_WORTH_MINING || rand.nextDouble() <= miningProbability) {
                robotState = RobotState.MINE;
            }
        }
    }

    private static void switchStateFromMineState() throws GameActionException {
        //check if need to build stuff
        int index = BuildOrder.doIHaveToBuildABuilding();
        if(index != -1) {
                //Respond
                buildOrderIndex = index;
                BuildOrder.IAmTheBuilding(buildOrderIndex);
                int value = BuildOrder.get(buildOrderIndex);
                buildingType = robotTypes[BuildOrder.decodeTypeOrdinal(value)];
                moveTargetLocation = null;
                robotState = RobotState.BUILD;
        } else if (Clock.getRoundNum() > 1750 && rc.getHealth() > 10 
            && RobotCount.read(RobotType.HANDWASHSTATION) < 10) {
            //Lategame handwash station attack
            robotState = RobotState.BUILD;
            moveTargetLocation = myLocation;
            buildingType = RobotType.HANDWASHSTATION;
        } else if (rc.isCoreReady()) {
            //Transition to wandering around if ore level is too low
            double ore = rc.senseOre(myLocation);
            double miningProbability = 0.5*(ore-MIN_ORE_WORTH_CONSIDERING)/(MIN_ORE_WORTH_MINING-MIN_ORE_WORTH_CONSIDERING);
            if(ore < MIN_ORE_WORTH_MINING && rand.nextDouble() > miningProbability) {
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
    
    private static void beaverWander() throws GameActionException {
        //Vigilance
        checkForEnemies();
        
        // wander around near HQ
        if (myLocation.distanceSquaredTo(HQLocation) > 35) {
            bug(HQLocation);
        } else {
            wander();
        }
        
        //Distribute supply
        distributeSupply(suppliabilityMultiplier_Preattack);
    }
    
    // TODO: this is identical to miner's mine
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
        
        //Update the ID on the build order to the structure's
        if(rc.isBuildingSomething()) {
            if(buildingID == 0) {
                //Get beaver's own robotinfo's building location field
                MapLocation loc = RobotPlayer.rc.senseRobot(RobotPlayer.rc.getID()).buildingLocation;
                if(loc != null) {
                    buildingID = RobotPlayer.rc.senseRobotAtLocation(loc).ID;
                    BuildOrder.IAmBuildingTheBuilding(buildOrderIndex,buildingID);
                }
            } else {
                //Continue claiming using recorded ID
                BuildOrder.IAmBuildingTheBuilding(buildOrderIndex,buildingID);
            }
            
        } else if(buildingID != 0) {
            //Just finished building, reset building parameters
            buildingID = 0;
            buildOrderIndex = -1;
            buildingType = null;
            robotState = RobotState.WANDER;
            
        } else {
            //Recently accepted building task
            if(buildOrderIndex != -1) {
                //Continue claiming job
                BuildOrder.IAmTheBuilding(buildOrderIndex);
            }
            
            if(moveTargetLocation == null) {
                //No specified build location
                tryBuild(myLocation.directionTo(enemyHQLocation),buildingType);
            } else {
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
                    }
                }
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
    
    // Vigilance: stops everything and attacks when enemies are in attack range.
    private static void checkForEnemies() throws GameActionException {
        while (enemies.length > 0) {
            if (rc.isWeaponReady()) {
                // basicAttack(enemies);
                priorityAttack(enemies, attackPriorities);
            }
            updateEnemyInRange(attackRange);
            RobotCount.report();
            rc.yield();
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
            int relativeParity = (myLocation.x+myLocation.y-CHECKERBOARD_PARITY+2)%2;
            int dirInt0 = dir0.ordinal();
            for(int offset : offsets) {
                int dirInt = (dirInt0+offset+8)%8;
                if((dirInt+relativeParity)%2 == 1 
                    && rc.canBuild(directions[dirInt],robotType)) {
                        rc.build(directions[dirInt],robotType);
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
    
    //Parameters ==============================================================

    /**
     * The importance rating that enemy units of each RobotType should be attacked
     * (so higher means attack first). Needs to be adjusted dynamically based on
     * defence strategy.
     */
    private static int[] attackPriorities = {
        20/*0:HQ*/,         21/*1:TOWER*/,      6/*2:SUPPLYDPT*/,   3/*3:TECHINST*/,
        7/*4:BARRACKS*/,    8/*5:HELIPAD*/,     5/*6:TRNGFIELD*/,   9/*7:TANKFCTRY*/,
        4/*8:MINERFCTRY*/,  1/*9:HNDWSHSTN*/,   10/*10:AEROLAB*/,   13/*11:BEAVER*/,
        2/*12:COMPUTER*/,   16/*13:SOLDIER*/,   15/*14:BASHER*/,    12/*15:MINER*/,
        14/*16:DRONE*/,     17/*17:TANK*/,      18/*18:COMMANDER*/, 11/*19:LAUNCHER*/,
        19/*20:MISSILE*/
    };

    /**
     * Multipliers for the effective supply capacity for friendly unit robotTypes, by
     * which the dispenseSupply() and distributeSupply() methods allocate supply (so
     * higher means give more supply to units of that type).
     */
    private static double[] suppliabilityMultiplier_Conservative = {
        1/*0:HQ*/,          1/*1:TOWER*/,       1/*2:SUPPLYDPT*/,   1/*3:TECHINST*/,
        1/*4:BARRACKS*/,    1/*5:HELIPAD*/,     1/*6:TRNGFIELD*/,   1/*7:TANKFCTRY*/,
        1/*8:MINERFCTRY*/,  1/*9:HNDWSHSTN*/,   1/*10:AEROLAB*/,    0/*11:BEAVER*/,
        0/*12:COMPUTER*/,   0/*13:SOLDIER*/,    0/*14:BASHER*/,     0.5/*15:MINER*/,
        0/*16:DRONE*/,      0/*17:TANK*/,       0/*18:COMMANDER*/,  0/*19:LAUNCHER*/,
        0/*20:MISSILE*/
    };

    private static double[] suppliabilityMultiplier_Preattack = {
        0/*0:HQ*/,          0/*1:TOWER*/,       0/*2:SUPPLYDPT*/,   0/*3:TECHINST*/,
        0/*4:BARRACKS*/,    0/*5:HELIPAD*/,     0/*6:TRNGFIELD*/,   0/*7:TANKFCTRY*/,
        0/*8:MINERFCTRY*/,  0/*9:HNDWSHSTN*/,   0/*10:AEROLAB*/,    1/*11:BEAVER*/,
        0/*12:COMPUTER*/,   1/*13:SOLDIER*/,    1/*14:BASHER*/,     1/*15:MINER*/,
        1/*16:DRONE*/,      1/*17:TANK*/,       1/*18:COMMANDER*/,  1/*19:LAUNCHER*/,
        0/*20:MISSILE*/
    };

}
