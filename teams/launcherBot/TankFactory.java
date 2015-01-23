package launcherBot;

import java.util.Random;

import launcherBot.Utility.*;
import battlecode.common.*;

public class TankFactory extends SpawnableStructure {
    
    private static int maxNumberOfTanks = 50;
    
    //General methods =========================================================
    
    public static void start() throws GameActionException {
        init();
        while(true) {
            loop();
            rc.yield(); //Yield the round
        }
    }
    
    private static void init() throws GameActionException {
        
    }
    
    private static void loop() throws GameActionException {
        // Code that runs in every robot (including buildings, excepting missiles)
        sharedLoopCode();
        
        //Report existence if built because of build order
        claimBuildOrderEntry();
        
       //Spawn
        if(RobotCount.read(RobotType.TANK) < maxNumberOfTanks
                && RobotCount.read(RobotType.AEROSPACELAB) < 2) {
            //System.out.println("Trying to spawn tanks");
            trySpawn(myLocation.directionTo(enemyHQLocation), RobotType.TANK);
        }

        //Dispense Supply
        dispenseSupply(suppliabilityMultiplier_Preattack);
        
        
        /**
        if (Clock.getRoundNum()%100 ==7) {
            System.out.println(TankDefenseCount.HQ_CHANNEL + " " + rc.readBroadcast(TankDefenseCount.HQ_CHANNEL));
            for (int i=0; i< 20; i++) {
                System.out.println(TankDefenseCount.TOWER_BASE_CHANNEL + i + " " + rc.readBroadcast(TankDefenseCount.TOWER_BASE_CHANNEL + i));
            }
            
        }
        **/

        
    }

}