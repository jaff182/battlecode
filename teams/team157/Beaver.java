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
        minMiningRate = GameConstants.BEAVER_MINE_MAX;
        minOreWorthMining = minMiningRate*GameConstants.BEAVER_MINE_RATE;
        minOreWorthConsidering = GameConstants.MINIMUM_MINE_AMOUNT*GameConstants.BEAVER_MINE_RATE;
        
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
        buildOrderIndex = BuildOrder.doIHaveToBuildABuilding();
        if (buildOrderIndex != -1) {
            int value = BuildOrder.get(buildOrderIndex);
            buildingType = robotTypes[BuildOrder.decodeTypeOrdinal(value)];
            if(rc.getTeamOre() >= buildingType.oreCost) {
                //Respond and set building states
                BuildOrder.IAmTheBuilding(buildOrderIndex);
                moveTargetLocation = null;
                robotState = RobotState.BUILD;
            }
        } else if (Clock.getRoundNum() > 1750 && rc.getHealth() > 10 
            && RobotCount.read(RobotType.HANDWASHSTATION) < 10) {
                //Lategame handwash station attack
                robotState = RobotState.BUILD;
                moveTargetLocation = null;
                buildingType = RobotType.HANDWASHSTATION;
        } else if (rc.isCoreReady()) {
            //Mine
            double ore = rc.senseOre(myLocation);
            double miningProbability = 0.5*(ore-minOreWorthConsidering)/(minOreWorthMining-minOreWorthConsidering);
            if(ore >= minOreWorthMining || rand.nextDouble() <= miningProbability) {
                robotState = RobotState.MINE;
            }
        }
    }

    private static void switchStateFromMineState() throws GameActionException {
        //check if need to build stuff
        buildOrderIndex = BuildOrder.doIHaveToBuildABuilding();
        if (buildOrderIndex != -1) {
            int value = BuildOrder.get(buildOrderIndex);
            buildingType = robotTypes[BuildOrder.decodeTypeOrdinal(value)];
            if(rc.getTeamOre() >= buildingType.oreCost) {
                //Respond and set building states
                BuildOrder.IAmTheBuilding(buildOrderIndex);
                moveTargetLocation = null;
                robotState = RobotState.BUILD;
            }
        } else if (Clock.getRoundNum() > 1750 && rc.getHealth() > 10 
            && RobotCount.read(RobotType.HANDWASHSTATION) < 10) {
                //Lategame handwash station attack
                robotState = RobotState.BUILD;
                moveTargetLocation = null;
                buildingType = RobotType.HANDWASHSTATION;
        } else if (rc.isCoreReady()) {
            //Transition to wandering around if ore level is too low
            double ore = rc.senseOre(myLocation);
            double miningProbability = 0.5*(ore-minOreWorthConsidering)/(minOreWorthMining-minOreWorthConsidering);
            if(ore < minOreWorthMining && rand.nextDouble() > miningProbability) {
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
        
        //Validate Build Order
        //Important for robustness against changes to build order
        validateBuildOrder();
        
        //Act depending on the situation
        if(rc.isBuildingSomething()) {
            //In the middle of building something
            if(buildOrderIndex != -1) {
                //This means this was a building task issued by build order
                if(buildingID == 0) {
                    //Beaver just started building so buildingID is unset
                    //Get beaver's own robotinfo's building location field
                    MapLocation loc = RobotPlayer.rc.senseRobot(RobotPlayer.rc.getID()).buildingLocation;
                    //Claim build order entry
                    if(loc != null) {
                        //Record building's ID and use it to claim the build order 
                        //entry on behalf of the building
                        buildingID = RobotPlayer.rc.senseRobotAtLocation(loc).ID;
                        BuildOrder.IAmBuildingTheBuilding(buildOrderIndex,buildingID);
                    } else {
                        //Claim build order entry anyway
                        BuildOrder.IAmTheBuilding(buildOrderIndex);
                    }
                } else {
                    //BuildingID previously set
                    //This means beaver is trying to claim a build order entry on behalf
                    //Continue claiming using recorded ID
                    BuildOrder.IAmBuildingTheBuilding(buildOrderIndex,buildingID);
                }
            } else {
                //This is a non build order building task
                //Safe to reset state now
                if(robotState == RobotState.BUILD) {
                    resetBuildState();
                }
            }
            
        } else {
            //Not in the middle of building something
            if(buildingID != 0) {
                //Just finished building for a build order entry
                //Can finally stop claiming build order entry on building's behalf 
                //and reset building parameters
                resetBuildState();
            } else {
                //Recently accepted building task but not started building
                if(buildOrderIndex != -1) {
                    //This is a build order task
                    //Continue claiming job using own ID
                    BuildOrder.IAmTheBuilding(buildOrderIndex);
                }
                
                //Generic build procedure follows
                if(moveTargetLocation == null) {
                    //No specified build location, so build anywhere consistent with 
                    //checkerboard
                    tryBuild(myLocation.directionTo(enemyHQLocation),buildingType);
                } else {
                    // Go closer to build location.
                    // When the beaver is there, we can start building immediately
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
        }
        
        //Distribute supply
        distributeSupply(suppliabilityMultiplier_Preattack);
    }
    

    //Other methods ===========================================================
    
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
     * Checks that the recorded build order index matches the ID of the beaver or the 
     * building it is currently building. Updates the build order index if it has 
     * changed.
     */
    private static void validateBuildOrder() throws GameActionException {
        if(buildOrderIndex != -1) {
            int value = BuildOrder.get(buildOrderIndex);
            int id = BuildOrder.decodeID(value);
            if(id != rc.getID() && (buildingID == 0 || id != buildingID)) {
                //Mismatch of IDs, check if entry has simply been shifted
                buildOrderIndex = BuildOrder.AmIOnBuildOrder(rc.getID());
                if(buildOrderIndex == -1 && buildingID != 0) {
                    //Check if claiming using the building ID instead
                    buildOrderIndex = BuildOrder.AmIOnBuildOrder(buildingID);
                }
            }
        }
    }
    
    /**
     * Sets the parameters to the unset values and changes the beaver's state to 
     * wander.
     */
    private static void resetBuildState() {
        buildingID = 0;
        buildOrderIndex = -1;
        buildingType = null;
        robotState = RobotState.WANDER;
    }
    
    /**
     * Finds a direction to build.
     * @param dir0 Preferred Direction
     * @throws GameActionException
     */
    private static Direction findBuildDirection(Direction dir0) throws GameActionException {
        myLocation = rc.getLocation();
        int relativeParity = (myLocation.x+myLocation.y-CHECKERBOARD_PARITY+2)%2;
        int dirInt0 = dir0.ordinal();
        for(int offset : offsets) {
            int dirInt = (dirInt0+offset+8)%8;
            if((dirInt+relativeParity)%2 == 1 
                && rc.canMove(directions[dirInt])) {
                    return directions[dirInt];
            }
        }
        return Direction.NONE;
    }
    
    
    /**
     * Build robot of type rbtype in direction dir0 if allowed
     * @param dir0 Direction to build at
     * @param robotType RobotType of building to build
     * @throws GameActionException
     */
    private static void tryBuild(Direction dir0, RobotType robotType) throws GameActionException {
        if(rc.isCoreReady() && rc.hasBuildRequirements(robotType)) {
            Direction dir = findBuildDirection(dir0);
            if(dir.ordinal() < 8 && rc.canBuild(dir,robotType)) {
                //Build!
                rc.build(dir,robotType);
            } else {
                wander();
            }
        }
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
