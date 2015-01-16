package team157;

import java.util.Random;
import battlecode.common.*;

public class TechnologyInstitute extends SpawnableStructure {
    
    //General methods =========================================================
    
    public static void start() throws GameActionException {
       // spawn computers and send them to HQ
        SpawnableStructure.start(RobotType.COMPUTER, HQLocation);
    }
}