package team157;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotType;

/**
 *
 */
public class SpawnableStructure extends Structure{

    protected static MapLocation spawnLocation;

    protected static RobotType spawnUnit;

    public static void start(RobotType robotType) throws GameActionException {
        init(robotType);
        while(true) {
            loop();
            rc.yield(); //Yield the round
        }
    }

    private static void init(RobotType robotType) throws GameActionException {
        rc.setIndicatorString(0,"hello i'm a tank factory.");
        spawnLocation = HQLocation;
        spawnUnit = robotType;
    }

    private static void loop() throws GameActionException {
        // Code that runs in every robot (including buildings, excepting missiles)
        sharedLoopCode();

        //Spawn
        trySpawn(myLocation.directionTo(spawnLocation), spawnUnit);

        //Dispense Supply
        dispenseSupply(suppliabilityMultiplier_Preattack);
    }
}
