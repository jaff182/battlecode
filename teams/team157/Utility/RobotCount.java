package team157.Utility;

import battlecode.common.GameActionException;
import battlecode.common.RobotType;
import team157.Channels;
import team157.RobotPlayer;

public class RobotCount {
    /**
     * Returns a count of robots in the area.
     * 
     * Accessible only by HQ.
     * 
     * Other robots accessing this array using read() will get corrupt values
     * 
     */
    public final static int BASE_CHANNEL = Channels.UNIT_COUNT_BASE;
    
    /**
     * All robots should call report() once per turn, to report in that they
     * exist (and be included in the count)
     * 
     * ALL ROBOTS SHOULD CALL THIS AFTER myType IS DEFINED (that is, all the time).
     * 
     * @return
     * @throws GameActionException 
     */
    public static void report() throws GameActionException {
        int count = RobotPlayer.rc.readBroadcast(BASE_CHANNEL+RobotPlayer.myType.ordinal());

        RobotPlayer.rc.broadcast(BASE_CHANNEL+RobotPlayer.myType.ordinal(), count+1);
    }
    
    /**
     * Read the amount of robots of that type.
     * 
     * ONLY HQ SHOULD CALL THIS, WHEN IT WANTS STATISTICS.
     * 
     * @param robotType
     * @return
     * @throws GameActionException 
     */
    public static int read(RobotType robotType) throws GameActionException {
        return RobotPlayer.rc.readBroadcast(BASE_CHANNEL+robotType.ordinal());
    }
    
    /**
     * Reset counts of robots.
     * 
     * Calls to read() after this are invalid.
     * 
     * HQ MUST CALL THIS AT END OF ROUND
     * 
     * @throws GameActionException 
     */
    public static void reset() throws GameActionException {
        int length = RobotPlayer.robotTypes.length;
        for (int i=0; i!=length; ++i)
            RobotPlayer.rc.broadcast(BASE_CHANNEL+i, 0);
    }
}
