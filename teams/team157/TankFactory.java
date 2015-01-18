package team157;

import java.util.Random;

import team157.Utility.*;
import battlecode.common.*;

public class TankFactory extends SpawnableStructure {
    
    //General methods =========================================================
    
    public static void start() throws GameActionException {
        while(true) {
            loop();
            rc.yield(); //Yield the round
        }
    }
    
    private static void loop() throws GameActionException {
        // Code that runs in every robot (including buildings, excepting missiles)
        sharedLoopCode();

        //Spawn
        trySpawn(myLocation.directionTo(enemyHQLocation), RobotType.TANK);

        //Dispense Supply
        dispenseSupply(suppliabilityMultiplier_Preattack);
        
        
        /**
        if (Clock.getRoundNum()%100 ==7) {
            System.out.println(rc.readBroadcast(TankDefenseCount.HQ_CHANNEL));
            for (int i=0; i< 20; i++) {
                System.out.println(rc.readBroadcast(TankDefenseCount.TOWER_BASE_CHANNEL + i));
            }
            
        }
        **/
        
    }

}