package launcherBot;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotType;

/**
 *
 */
public class SpawnableStructure extends Structure {

    protected static MapLocation spawnLocation;

    protected static RobotType spawnUnit;

    // by default sending them to enemy location
    public static void start(RobotType robotType) throws GameActionException {
        start(robotType, enemyHQLocation);
    }

    public static void start(RobotType robotType, MapLocation mapLocation) throws GameActionException {
        init(robotType, mapLocation);
        while(true) {
            loop();
            rc.yield(); //Yield the round
        }
    }

    private static void init(RobotType robotType, MapLocation mapLocation) throws GameActionException {
        rc.setIndicatorString(0,"hello i'm a spawnable building:" + rc.getType().name());
        spawnLocation = mapLocation;
        spawnUnit = robotType;
        
    }

    private static void loop() throws GameActionException {
        // Code that runs in every robot (including buildings, excepting missiles)
        sharedLoopCode();
        
        //Report existence if built because of build order
        claimBuildOrderEntry();

        //Spawn
        trySpawn(myLocation.directionTo(spawnLocation), spawnUnit);

        //Dispense Supply
        dispenseSupply(suppliabilityMultiplier_Preattack);
    }
}
