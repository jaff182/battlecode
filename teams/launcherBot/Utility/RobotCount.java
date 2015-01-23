package launcherBot.Utility;

import launcherBot.Channels;
import launcherBot.Common;
import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.RobotType;

public class RobotCount {
    /**
     * Returns a count of robots in the area.
     * 
     * Accessible by all bots. You will access counts from last round.
     * 
     * Internally, the robotCount alternates its arrays between even and odd
     * rounds, writing and reading alternately.
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
        final int channel;
        if (Clock.getRoundNum()%2 == 0) // even rounds write to high address
            channel = BASE_CHANNEL+Common.myType.ordinal()+Common.robotTypes.length;
        else
            channel = BASE_CHANNEL+Common.myType.ordinal(); // odd rounds write to low
        final int count = Common.rc.readBroadcast(channel);
//        System.out.println("Current count is " + count + ". Writing this to " + channel);
        Common.rc.broadcast(channel, count+1);
    }
    
    /**
     * Read the amount of robots of that type.
     * 
     * Any bot can call this.
     * 
     * @param robotType
     * @return
     * @throws GameActionException 
     */
    public static int read(RobotType robotType) throws GameActionException {
        final int channel;
        if (Clock.getRoundNum()%2 == 0) // even rounds read from low address
            channel = BASE_CHANNEL+robotType.ordinal();
        else // odd rounds read from high
            channel = BASE_CHANNEL+robotType.ordinal()+Common.robotTypes.length;
//        System.out.println("Read count for " + robotType + " on channel " + channel + ", result is " + Common.rc.readBroadcast(channel));
        return Common.rc.readBroadcast(channel);
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
        int length = Common.robotTypes.length;
        final int relBaseChannel;
        if (Clock.getRoundNum()%2 == 0) // even rounds reset high address at start of round
            relBaseChannel = BASE_CHANNEL + length;
        else // odd rounds reset low
            relBaseChannel = BASE_CHANNEL;
        for (int i=0; i!=length; ++i)
            Common.rc.broadcast(relBaseChannel+i, 0);
    }
}
