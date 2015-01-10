package team157;

import java.util.Random;

import battlecode.common.*;

public class Beaver extends MovableUnit {

    // General methods =========================================================

    public static void start() throws GameActionException {
        init();
        while (true) {
            loop();
            rc.yield(); // Yield the round
        }
    }

    private static void init() throws GameActionException {
        rc.setIndicatorString(0, "hello i'm a beaver.");
        initialSense(rc.getLocation());
    }

    private static void loop() throws GameActionException {
        rc.breakpoint();
        // Sensing
        RobotInfo[] enemies = rc.senseNearbyRobots(attackRange, enemyTeam);
        myLocation = rc.getLocation();
        
        
        System.out.println(" " + RobotPlayer.myLocation.distanceSquaredTo(enemyHQLocation) + " "+  RobotPlayer.myLocation
                    .distanceSquaredTo(HQLocation));
        // Switch states
        switch (robotState) {
        case WANDER:
            if (RobotPlayer.myLocation.distanceSquaredTo(enemyHQLocation) < RobotPlayer.myLocation
                    .distanceSquaredTo(HQLocation)
                    && rc.senseOre(myLocation) < GameConstants.BEAVER_MINE_MAX
                            * GameConstants.BEAVER_MINE_RATE) {
                robotState = RobotState.MINE;
            }
            break;
        case MINE:
            if (Clock.getRoundNum() > 1500 && rc.getHealth() > 10) {
                robotState = RobotState.ATTACK_MOVE;
                moveTargetLocation = enemyHQLocation;
            } else if (enemies.length != 0) {
                robotState = RobotState.ATTACK_MOVE;
                moveTargetLocation = HQLocation;
            } else if (rc.senseOre(myLocation) < GameConstants.BEAVER_MINE_MAX*GameConstants.BEAVER_MINE_RATE)
                robotState = RobotState.WANDER;
            break;

        }
        
        rc.setIndicatorString(1, "In state: " + robotState);

        // Perform action based on state
        switch (robotState) {
        case ATTACK_MOVE:
            // Vigilance
            // Stops everything and attacks when enemies are in attack range.
            while (enemies.length > 0) {
                if (rc.isWeaponReady()) {
                    // basicAttack(enemies);
                    priorityAttack(enemies, attackPriorities);
                }
                enemies = rc.senseNearbyRobots(attackRange, enemyTeam);
                rc.yield();
            }

            // Go to Enemy HQ
            exploreRandom(enemyHQLocation);
            // rc.setIndicatorString(1, "Number of bytecodes: " +
            // Clock.getBytecodeNum());

            // Distribute supply
            distributeSupply(suppliabilityMultiplier);
            break;
        case WANDER:
            // Vigilance
            // Stops everything and attacks when enemies are in attack range.
            while (enemies.length > 0) {
                if (rc.isWeaponReady()) {
                    // basicAttack(enemies);
                    priorityAttack(enemies, attackPriorities);
                }
                enemies = rc.senseNearbyRobots(attackRange, enemyTeam);
                rc.yield();
            }

            // Go to Enemy HQ
            wander();
            // rc.setIndicatorString(1, "Number of bytecodes: " +
            // Clock.getBytecodeNum());

            // Distribute supply
            distributeSupply(suppliabilityMultiplier);
            break;
        case MINE:
            if (rc.getCoreDelay()< 1)
                 rc.mine();
            // Distribute supply
            distributeSupply(suppliabilityMultiplier);
            break;
        default:
            throw new IllegalStateException();
        }

    }

    // Specific methods =======================================================
    
    
    //Parameters ==============================================================
    
    /**
     * Ranks the RobotType order in which enemy units should be attacked (so lower 
     * means attack first). Needs to be adjusted dynamically based on defence strategy.
     */
    private static int[] attackPriorities = {
        1/*0:HQ*/,          0/*1:TOWER*/,       15/*2:SUPPLYDPT*/,  18/*3:TECHINST*/,
        14/*4:BARRACKS*/,   13/*5:HELIPAD*/,    16/*6:TRNGFIELD*/,  12/*7:TANKFCTRY*/,
        17/*8:MINERFCTRY*/, 20/*9:HNDWSHSTN*/,  11/*10:AEROLAB*/,   8/*11:BEAVER*/,
        19/*12:COMPUTER*/,  5/*13:SOLDIER*/,    6/*14:BASHER*/,     9/*15:MINER*/,
        7/*16:DRONE*/,      4/*17:TANK*/,       3/*18:COMMANDER*/,  10/*19:LAUNCHER*/,
        2/*20:MISSILE*/
    };
    
    /**
     * Multipliers for the effective supply capacity for friendly unit robotTypes, by 
     * which the dispenseSupply() and distributeSupply() methods allocate supply (so 
     * higher means give more supply to units of that type).
     */
    private static double[] suppliabilityMultiplier = {
        0/*0:HQ*/,          1/*1:TOWER*/,       0/*2:SUPPLYDPT*/,   1/*3:TECHINST*/,
        1/*4:BARRACKS*/,    1/*5:HELIPAD*/,     1/*6:TRNGFIELD*/,   1/*7:TANKFCTRY*/,
        1/*8:MINERFCTRY*/,  1/*9:HNDWSHSTN*/,   0/*10:AEROLAB*/,    1/*11:BEAVER*/,
        0/*12:COMPUTER*/,   1/*13:SOLDIER*/,    1/*14:BASHER*/,     1/*15:MINER*/,
        1/*16:DRONE*/,      1/*17:TANK*/,       1/*18:COMMANDER*/,  1/*19:LAUNCHER*/,
        0/*20:MISSILE*/
    };
    
    
    //Build methods ===========================================================
    
    /**
     * Build robot of type rbtype in direction dir0 if allowed
     * @param dir0 Direction to build at
     * @param robotType RobotType of building to build
     * @throws GameActionException
     */
    public static void tryBuild(Direction dir0, RobotType robotType) throws GameActionException {
        if(rc.isCoreReady() && rc.getTeamOre() >= robotType.oreCost) {
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
