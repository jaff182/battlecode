package team157;

import java.util.Random;
import battlecode.common.*;
import team157.Utility.*;

public class Miner extends MiningUnit {
    
    /**
     * Is this miner mining efficiently? That is, is it mining at 0.75*3?
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
        rc.setIndicatorString(0,"hello i'm a miner.");

    }
    
    private static void loop() throws GameActionException {
        //Update location
        myLocation = rc.getLocation();
        //Calculate whether we are mining efficiently; used in the miner effectiveness count to calibrate output
        //TODO: can we refactor this into the effectiveness count?
        miningEfficiently = (rc.senseOre(myLocation) > 0.5*GameConstants.MINER_MINE_MAX*GameConstants.MINER_MINE_RATE);
        
        // Code that runs in every robot (including buildings, excepting missiles)
        sharedLoopCode();
        
        //Update effectiveness count. Data might be *slightly* stale, but it's fine (hopefully).
        MinerEffectivenessCount.report();
        
        //Sense nearby units
        updateEnemyInSight();

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
            //Mining probability decreases with the increasing number of miners and the decreasing amount of ore
            double miningProbability = 1 - 1/(1+2.0*ore/(GameConstants.MINER_MINE_MAX*GameConstants.MINER_MINE_RATE));
            if(rand.nextDouble() <= miningProbability) {
                robotState = RobotState.MINE;
            }
        }
    }
    
    private static void switchStateFromMineState() throws GameActionException {
        if (enemies.length != 0) {
            robotState = RobotState.ATTACK_MOVE;
            moveTargetLocation = HQLocation;
        } else if (rc.isCoreReady()) {
            double ore = rc.senseOre(myLocation);
            double miningProbability = 1 - 1/(1+2.0*ore/(GameConstants.MINER_MINE_MAX*GameConstants.MINER_MINE_RATE));
            if(rand.nextDouble() > miningProbability) {
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
}