package team157.Utility;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.RobotType;
import team157.Miner;
import team157.Channels;
import team157.Common;


public class MinerEffectiveness {

    //Global variables ========================================================
    
    /**
     * Low radio channel for effective miner count.
     */
    public static final int LOW_CHANNEL = Channels.MINER_EFFECTIVENESS;
    
    /**
     * High radio channel for effective miner count.
     */
    public static final int HIGH_CHANNEL = Channels.MINER_EFFECTIVENESS+1;
    
    /**
     * Radio channel for announcing the mean percentage of effective miners.
     */
    public static final int SCORE_CHANNEL = Channels.MINER_EFFECTIVENESS+2;
    
    
    /**
     * The number of rounds over which each measurement of the effective miner 
     * proportion is averaged. Should be significantly more than mining and movement 
     * delays.
     */
    public static final int MEASUREMENT_PERIOD = 50;
    
    /**
     * Total sum of effectiveness proportions over the current measurement period.
     */
    public static double sum = 0;
    
    /**
     * The proportion of effective miners, averaged over the previous measurement 
     * period.
     */
    public static double mean = 0;
    
    
    //Counting methods ========================================================
    
    /**
     * ONLY CALL IN MINERS. Call when a miner is mining efficiently; statistic is 
     * used by miner factories to calibrate output.
     */
    public static void report() throws GameActionException {
        final int channel;
        if (Clock.getRoundNum() % 2 == 0) {
            // even rounds write to high address
            channel = HIGH_CHANNEL;
        } else {
            // odd rounds write to low
            channel = LOW_CHANNEL;
        }
        final int count = Common.rc.readBroadcast(channel);
        Common.rc.broadcast(channel, count + 1);
    }
    
    /**
     * Read the amount of robots that are mining efficiently.
     * Any bot can call this.
     * @param robotType
     * @return
     * @throws GameActionException 
     */
    public static int read() throws GameActionException {
        final int channel;
        if (Clock.getRoundNum()%2 == 0) {
            // even rounds read from low address
            channel = LOW_CHANNEL;
        } else {
            // odd rounds read from high
            channel = HIGH_CHANNEL;
        }
        return Common.rc.readBroadcast(channel);
    }
    
    /**
     * Reset counts of efficient robots.
     * Calls to read() after this are invalid.
     * HQ MUST CALL THIS AT START OF ROUND.
     * @throws GameActionException 
     */
    public static void reset() throws GameActionException {
        final int relBaseChannel;
        if (Clock.getRoundNum()%2 == 0) {
            // even rounds reset high address at start of round
            relBaseChannel = HIGH_CHANNEL;
        } else {
            // odd rounds reset low
            relBaseChannel = LOW_CHANNEL;
        }
        Common.rc.broadcast(relBaseChannel, 0);
    }
    
    
    //Announcing proportion ===================================================
    
    /**
     * Adds the current proportion of effective miners to a sum, and computes the 
     * mean at the end of the measurement period.
     */
    public static void update() throws GameActionException {
        int minerCount = RobotCount.read(RobotType.MINER);
        if(minerCount != 0) {
            //Add effective miner proportion to sum for current measurement period
            sum += 1.0*read()/minerCount;
        }
        if(Clock.getRoundNum()%MEASUREMENT_PERIOD == 0) {
            //Update mean effectiveness and broadcast value, reset sum
            mean = sum/MEASUREMENT_PERIOD;
            Common.rc.setIndicatorString(1,"Effectiveness is "+mean);
            Common.rc.broadcast(SCORE_CHANNEL,(int)(100*mean));
            sum = 0;
        }
    }
    
}
