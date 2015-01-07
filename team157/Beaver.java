package team157;

import java.util.Random;
import battlecode.common.*;

public class Beaver extends RobotPlayer {

    // General methods =========================================================

    public static void start() throws GameActionException {
        init();
        while (true) {
            loop();
            rc.yield(); // Yield the round
        }
    }

    private static void init() throws GameActionException {
        rc.setIndicatorString(0, "hello i'm a beaver.");

    }

    private static void loop() throws GameActionException {
        
        //Vigilance
        RobotInfo[] enemies = rc.senseNearbyRobots(myrng, enmteam);
        while(enemies.length > 0) {
            if(rc.isWeaponReady()) {
                basicAttack(enemies);
            }
            enemies = rc.senseNearbyRobots(myrng, enmteam);
            rc.yield();
        }
        
        //Go to Enemy HQ
        walk(enmloc);

    }

    // Specific methods
    // =========================================================

    // Methods to build buildings - Josiah
    // Procedure to be used
    // 1) HQ broadcasts on specific radio channel the desired number of
    // buildings
    // 2) Buildings report their existence on each round to produce total count
    // 3) Beavers check for need to build any buildings, and claim the job to
    // build it
    // 4) Beaver builds building. (for now, on the spot)

    /**
     * checkAndRequestBuilding has to perform the following functions in a
     * single turn:
     * 
     * 1) check for need to build building <br>
     * 2) if building is needed, read off messaging array to check building
     * type, parameters (say, coordinates of build site), and score suitability<br>
     * 3) 
     * 
     * MUST COMPLETE IN ONE TURN OR CODE WILL BREAK (POSSIBLY GLOBALLY)
     */
    public static void checkAndRequestBuilding() {

    }

}
