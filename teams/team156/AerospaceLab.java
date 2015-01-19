package team156;

import java.util.Random;
import battlecode.common.*;

public class AerospaceLab extends SpawnableStructure {
    
    //General methods =========================================================
    
    public static void start() throws GameActionException {
        // spawn launchers and send them to our HQ
       start(RobotType.LAUNCHER, HQLocation);
    }
}