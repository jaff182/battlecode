package team157;

import java.util.Random;
import battlecode.common.*;

public class Barracks extends SpawnableStructure {
    
    //General methods =========================================================
    
    public static void start() throws GameActionException {
        //SpawnableStructure.start(RobotType.SOLDIER);
        while (true) {
            rc.yield();
        }
    }
}