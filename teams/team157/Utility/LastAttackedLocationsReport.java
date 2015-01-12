package team157.Utility;

import team157.RobotPlayer;
import battlecode.common.*;

public class LastAttackedLocationsReport {
    /**
     * Attack events are stored in radio map, in a circular array.
     * 
     * From BASE_CHANNEL, the first channel stores x location (raw MapLocation
     * coordinate), and second y. They alternate all the way through, up to
     * NUMBER_OF_STORED_EVENTS*2 channels.
     * 
     * nextEventToOverwriteRelativeIndex is the next event to overwrite
     * 
     * The init() method must be called by HQ
     * 
     * 
     */
    static RobotController rc;
    
    final static int BASE_CHANNEL = 30001;
    final static int NUMBER_OF_STORED_EVENTS = 2;
    final static int NUMBER_OF_CHANNELS_USED = NUMBER_OF_STORED_EVENTS*2; //one for x, y
    
    final static int NUMBER_OF_EVENTS_THAT_HAVE_OCCURRED_CHANNEL = 30000;
    public static int numberOfEventsThatHaveOccurred = 0; // Updates every time you add/read, number of events globally
    
    public static void init() throws GameActionException {
        rc = RobotPlayer.rc;
        rc.broadcast(BASE_CHANNEL+NUMBER_OF_CHANNELS_USED-1, Integer.MAX_VALUE);
        rc.broadcast(BASE_CHANNEL+NUMBER_OF_CHANNELS_USED-2, Integer.MAX_VALUE);
    }

    /**
     * Add an event to the data structure
     * 
     * @param x
     * @param y
     * @throws GameActionException
     */
    public static void add(int x, int y) throws GameActionException {
        numberOfEventsThatHaveOccurred = rc.readBroadcast(NUMBER_OF_EVENTS_THAT_HAVE_OCCURRED_CHANNEL);
        int baseChannelIdx = BASE_CHANNEL
                + (numberOfEventsThatHaveOccurred % NUMBER_OF_STORED_EVENTS)
                * 2;
        rc.broadcast(baseChannelIdx, x);
        rc.broadcast(baseChannelIdx + 1, y);
        rc.broadcast(NUMBER_OF_EVENTS_THAT_HAVE_OCCURRED_CHANNEL, numberOfEventsThatHaveOccurred+1);
    }
    
    //TODO: Combine both methods to optimize bytecode usage
    /**
     * Query data structure
     * 
     * 0 indexed.
     * 
     * @param number
     *            0 for the last attacked location, will return
     *            Integer.MAX_VALUE on 1 + maximum number. <br>
     *            <br>
     *            You should stop iteration then.
     * @return 
     * @throws GameActionException
     */
    public static int getLastAttackXCoordinate(int number) throws GameActionException {
        numberOfEventsThatHaveOccurred = rc
                .readBroadcast(NUMBER_OF_EVENTS_THAT_HAVE_OCCURRED_CHANNEL);
        int channel = BASE_CHANNEL
                + ((numberOfEventsThatHaveOccurred - number) * 2)
                % NUMBER_OF_CHANNELS_USED;
        if (channel < 0)
            return rc.readBroadcast(channel+NUMBER_OF_CHANNELS_USED);
        else
            return rc.readBroadcast(channel);
    }
    
    /**
     * Query data structure
     * 
     * 0 indexed.
     * 
     * @param number
     *            0 for the last attacked location, will return
     *            Integer.MAX_VALUE on 1 + maximum number. <br>
     * <br>
     *            You should stop iteration then.
     * @return
     * @throws GameActionException
     */
    public static int getLastAttackYCoordinate(int number) throws GameActionException {
        numberOfEventsThatHaveOccurred = rc
                .readBroadcast(NUMBER_OF_EVENTS_THAT_HAVE_OCCURRED_CHANNEL);

        int channel = 1 + BASE_CHANNEL
                + ((numberOfEventsThatHaveOccurred - number) * 2)
                % NUMBER_OF_CHANNELS_USED;
        if (channel < 0)
            return rc.readBroadcast(channel+NUMBER_OF_CHANNELS_USED);
        else
            return rc.readBroadcast(channel);
    }
    
    /**
     * Gets the latest number of events that have occurred.
     * @return
     * @throws GameActionException
     */
    public static int getNumberOfEventsThatHaveOccurred() throws GameActionException {
        numberOfEventsThatHaveOccurred = rc
                .readBroadcast(NUMBER_OF_EVENTS_THAT_HAVE_OCCURRED_CHANNEL);

        return numberOfEventsThatHaveOccurred;
    }
    
}
