package team157;

import java.util.Random;
import battlecode.common.*;
import team157.Utility.*;

public class Miner extends MiningUnit {
    
    //Global variables ========================================================
    
    /**
     * Is this miner mining efficiently? That is, is it mining at all?
     */
    public static boolean miningEfficiently = true;
    
    
    //General methods =========================================================
    
    public static void start() throws GameActionException {
        init();
        while(true) {
            loop();
            rc.yield(); //Yield the round
        }
    }
    
    private static void init() throws GameActionException {

        //Set mining parameters
        MIN_MINING_RATE = GameConstants.MINER_MINE_MAX;
        MIN_ORE_WORTH_MINING = MIN_MINING_RATE*GameConstants.MINER_MINE_RATE;
        MIN_ORE_WORTH_CONSIDERING = GameConstants.MINIMUM_MINE_AMOUNT*GameConstants.MINER_MINE_RATE;
        
        //set locations within attack radius of enemy tower or hq as unpathable
        //Commented out because uses too much bytecode.
        initInternalMap();
        
    }
    
    private static void loop() throws GameActionException {
        //Update location
        myLocation = rc.getLocation();
        
        // Code that runs in every robot (including buildings, excepting missiles)
        sharedLoopCode();
        
        //Update effectiveness count. Data might be *slightly* stale, but it's fine (hopefully).
        MinerEffectivenessCount.report();
        
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
            case ATTACK_MOVE: minerAttack(); break;
            case WANDER: minerWander(); break;
            case MINE: minerMine(); break;
            default: throw new IllegalStateException();
        }
    }
    
    //State switching =========================================================
    
    private static void switchStateFromWanderState() throws GameActionException {
        if (rc.isCoreReady()) {
            //Mine
            double ore = rc.senseOre(myLocation);
            double miningProbability = 0.5*(ore-MIN_ORE_WORTH_CONSIDERING)/(MIN_ORE_WORTH_MINING-MIN_ORE_WORTH_CONSIDERING);
            if(ore >= MIN_ORE_WORTH_MINING || rand.nextDouble() <= miningProbability) {
                miningEfficiently = true;
                robotState = RobotState.MINE;
            }
        }
    }
    
    private static void switchStateFromMineState() throws GameActionException {
        if (rc.isCoreReady()) {
            double ore = rc.senseOre(myLocation);
            double miningProbability = 0.5*(ore-MIN_ORE_WORTH_CONSIDERING)/(MIN_ORE_WORTH_MINING-MIN_ORE_WORTH_CONSIDERING);
            if(ore < MIN_ORE_WORTH_MINING && rand.nextDouble() > miningProbability) {
                miningEfficiently = false;
                robotState = RobotState.WANDER;
            }
        }
    }
    
    //State methods ===========================================================
    
    private static void minerAttack() throws GameActionException {
        //Vigilance
        checkForEnemies();

        // Go to Enemy HQ
        bug(enemyHQLocation);
        
        //Distribute supply
        distributeSupply(suppliabilityMultiplier_Preattack);
    }

    private static void minerMine() throws GameActionException {
        //Vigilance
        checkForEnemies();
        
        //Mine
        if (rc.getCoreDelay()< 1) rc.mine();
        
        //Distribute supply
        distributeSupply(suppliabilityMultiplier_Preattack);
    }
    
    private static void minerWander() throws GameActionException {
       //Vigilance
       checkForEnemies();

       //Hill climb ore distribution while being repelled from other units
       updateFriendlyInRange(15);
       goTowardsOre();

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