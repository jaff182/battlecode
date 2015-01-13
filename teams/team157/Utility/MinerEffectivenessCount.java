package team157.Utility;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.RobotType;
import team157.Miner;
import team157.Channels;
import team157.RobotPlayer;


public class MinerEffectivenessCount {

    public static final int BASE_CHANNEL = Channels.EFFECTIVE_MINERS_COUNT;
    
    /**
     * ONLY CALL IN MINERS.

     * Call once per round; statistic is used by miner factories to calibrate output.
     * 
     * Reports whether a miner is mining efficiently.
     * 
     */
    public static void report() throws GameActionException {
        if (Miner.miningEfficiently) {
            final int channel;
            if (Clock.getRoundNum() % 2 == 0) // even rounds write to high
                                              // address
                channel = getHighChannel();
            else
                channel = getLowChannel(); // odd rounds write to low
            final int count = RobotPlayer.rc.readBroadcast(channel);
            // System.out.println("Current count is " + count +
            // ". Writing this to " + channel);
            RobotPlayer.rc.broadcast(channel, count + 1);
        }
    }
    
    /**
     * Read the amount of robots that are mining efficiently.
     * 
     * Any bot can call this.
     * 
     * @param robotType
     * @return
     * @throws GameActionException 
     */
    public static int read() throws GameActionException {
        final int channel;
        if (Clock.getRoundNum()%2 == 0) // even rounds read from low address
            channel = getLowChannel();
        else // odd rounds read from high
            channel = getHighChannel();
        return RobotPlayer.rc.readBroadcast(channel);
    }
    
    /**
     * Reset counts of efficient robots.
     * 
     * Calls to read() after this are invalid.
     * 
     * HQ MUST CALL THIS AT END OF ROUND
     * 
     * @throws GameActionException 
     */
    public static void reset() throws GameActionException {
        final int relBaseChannel;
        if (Clock.getRoundNum()%2 == 0) // even rounds reset high address at start of round
            relBaseChannel = getHighChannel();
        else // odd rounds reset low
            relBaseChannel = getLowChannel();
        RobotPlayer.rc.broadcast(relBaseChannel, 0);
    }

    private static int getHighChannel()
    {
        return BASE_CHANNEL + 1;
    }

    private static int getLowChannel()
    {
        return BASE_CHANNEL;
    }
}
