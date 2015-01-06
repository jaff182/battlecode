package donothing;

import battlecode.common.*;

public class RobotPlayer {
	
	public static void run(RobotController RC) throws GameActionException {
		while(true) {
            RC.yield();
        }
    }
}
