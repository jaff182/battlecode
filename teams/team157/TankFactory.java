package team157;

import java.util.Random;
import battlecode.common.*;

public class TankFactory extends SpawnableStructure {
    
    //General methods =========================================================
    
    public static void start() throws GameActionException {
        SpawnableStructure.start(RobotType.TANK);
    }

}