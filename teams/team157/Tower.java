package team157;

import java.util.Random;

import battlecode.common.*;
import team157.Utility.*;

public class Tower extends Structure {
    
    private static int tankDefenseChannel;
    private static int baseNumberOfTanksNeeded = 0;
    private static int numberOfTanksNeeded = baseNumberOfTanksNeeded;
    
    //General methods =========================================================
    
    public static void start() throws GameActionException {
        init();
        while(true) {
            loop();
            rc.yield(); //Yield the round
        }
    }
    
    private static void init() throws GameActionException {
        initTankDefenseChannel();
        //initialSense(rc.getLocation());
    }
    
    private static void loop() throws GameActionException {
        // Call for tank defense units every 10 rounds
        if (Clock.getRoundNum()%10 == 0) {
            TankDefenseCount.reset(tankDefenseChannel, numberOfTanksNeeded);
        }
        
        // Even though bytecode limited, report any attacks on this structure too.
        LastAttackedLocationsReport.report();

        //Vigilance
        //Stops everything and attacks when enemies are in attack range.
        updateEnemyInRange(attackRange);
        callForTankReinforcements();
        
        while(enemies.length > 0) {
            if(rc.isWeaponReady()) {
                //basicAttack(enemies);
                priorityAttack(enemies,attackPriorities);
            }
            updateEnemyInRange(attackRange);
            RobotCount.report();
            rc.yield();
        }
        
        //Dispense supply
        dispenseSupply(suppliabilityMultiplier_Preattack);
        
    }
    
    //Specific methods =========================================================
    
    /**
     * Initialize tank defense channels with number of tanks needed
     * and location of tower. Called only in init.
     * @throws GameActionException
     */
    private static void initTankDefenseChannel() throws GameActionException {
        for (int i=0; i<16; i+=3) {
            // call for tank defense units
            if (rc.readBroadcast(TankDefenseCount.TOWER_BASE_CHANNEL + i) == 0) {
                tankDefenseChannel = TankDefenseCount.TOWER_BASE_CHANNEL + i;
                rc.broadcast(tankDefenseChannel, numberOfTanksNeeded);
                rc.broadcast(tankDefenseChannel + 1, myLocation.x);
                rc.broadcast(tankDefenseChannel + 2, myLocation.y);
                return;
            }
        }
    }
    
    private static void callForTankReinforcements() {
        if (rc.senseNearbyRobots(81, enemyTeam).length > 10) {
            numberOfTanksNeeded += 2;
        } else {
            numberOfTanksNeeded = baseNumberOfTanksNeeded;
        }
    }
    
    /**
     * The importance rating that enemy units of each RobotType should be attacked 
     * (so higher means attack first). Needs to be adjusted dynamically based on 
     * defence strategy.
     */
    private static int[] attackPriorities = {
        20/*0:HQ*/,         21/*1:TOWER*/,     6/*2:SUPPLYDPT*/,    3/*3:TECHINST*/,
        7/*4:BARRACKS*/,    8/*5:HELIPAD*/,    5/*6:TRNGFIELD*/,    9/*7:TANKFCTRY*/,
        4/*8:MINERFCTRY*/,  1/*9:HNDWSHSTN*/,  10/*10:AEROLAB*/,    12/*11:BEAVER*/,
        2/*12:COMPUTER*/,   14/*13:SOLDIER*/,  13/*14:BASHER*/,     11/*15:MINER*/,
        17/*16:DRONE*/,     18/*17:TANK*/,     16/*18:COMMANDER*/,  15/*19:LAUNCHER*/,
        19/*20:MISSILE*/
    };
    
    /**
     * Multipliers for the effective supply capacity for friendly unit robotTypes, by 
     * which the dispenseSupply() and distributeSupply() methods allocate supply (so 
     * higher means give more supply to units of that type).
     */
    private static double[] suppliabilityMultiplier_Conservative = {
        0/*0:HQ*/,          1/*1:TOWER*/,       0/*2:SUPPLYDPT*/,   0/*3:TECHINST*/,
        1/*4:BARRACKS*/,    1/*5:HELIPAD*/,     0/*6:TRNGFIELD*/,   1/*7:TANKFCTRY*/,
        1/*8:MINERFCTRY*/,  0/*9:HNDWSHSTN*/,   1/*10:AEROLAB*/,    0/*11:BEAVER*/,
        0/*12:COMPUTER*/,   0/*13:SOLDIER*/,    0/*14:BASHER*/,     0/*15:MINER*/,
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