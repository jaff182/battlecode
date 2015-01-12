package team157.Utility;

import team157.RobotPlayer;
import battlecode.common.*;
import team157.Channels;


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
     * The HQinit() method must be called by HQ
     * 
     * The everyRobotInit() method must be called by every unit reporting.
     * 
     * The report() method should be called every round by every unit reporting.
     */
    
    final static int BASE_CHANNEL = Channels.LAST_ATTACKED_COORDINATES+1;
    final static int NUMBER_OF_STORED_EVENTS = 10;
    final static int NUMBER_OF_CHANNELS_USED = NUMBER_OF_STORED_EVENTS*2; //one for x, y
    
    final static int NUMBER_OF_EVENTS_THAT_HAVE_OCCURRED_CHANNEL = Channels.LAST_ATTACKED_COORDINATES;
    public static int numberOfEventsThatHaveOccurred = 0; // Updates every time you add/read, number of events globally
    
    static double hpLastRound;
    static RobotController rc;
    /**
     * Run this once per game at the start to set up memory array.
     * 
     * (So HQ should run it once in its init)
     * 
     * @throws GameActionException
     */
    public static void HQinit() throws GameActionException {
        RobotPlayer.rc.broadcast(BASE_CHANNEL+NUMBER_OF_CHANNELS_USED-1, Integer.MAX_VALUE);
        RobotPlayer.rc.broadcast(BASE_CHANNEL+NUMBER_OF_CHANNELS_USED-2, Integer.MAX_VALUE);
        RobotPlayer.rc.broadcast(NUMBER_OF_EVENTS_THAT_HAVE_OCCURRED_CHANNEL, 0);
    }
    
    /**
     * Run this once per unit, in the init.
     * @return 
     * 
     */
    public static void everyRobotInit() throws GameActionException {
        hpLastRound = RobotPlayer.myType.maxHealth;
        rc = RobotPlayer.rc;
    }
    
    /**
     * Run this once per round, after myLocation is updated.
     * 
     * If any attack occurs, this function will detect and autoreport it.
     * 
     * @throws GameActionException
     */
    public static void report() throws GameActionException {
        double hpNow = rc.getHealth();
        if (hpNow < hpLastRound) {
            hpLastRound = hpNow;
            add(RobotPlayer.myLocation.x, RobotPlayer.myLocation.y);
        }
    }

    /**
     * Add an event to the data structure
     * 
     * @param x
     * @param y
     * @throws GameActionException
     */
    public static void add(int x, int y) throws GameActionException {
        numberOfEventsThatHaveOccurred = RobotPlayer.rc.readBroadcast(NUMBER_OF_EVENTS_THAT_HAVE_OCCURRED_CHANNEL);
        int baseChannelIdx = BASE_CHANNEL
                + (numberOfEventsThatHaveOccurred % NUMBER_OF_STORED_EVENTS)
                * 2;
        RobotPlayer.rc.broadcast(baseChannelIdx, x);
        RobotPlayer.rc.broadcast(baseChannelIdx + 1, y);
        RobotPlayer.rc.broadcast(NUMBER_OF_EVENTS_THAT_HAVE_OCCURRED_CHANNEL, numberOfEventsThatHaveOccurred+1);
        
//        System.out.println("Attack reported at " + x + ", " + y);
//        System.out.println("Last 3 attacks (including this) as follows:");
//
//        for (int i =0; i< 5; i++) {
//            System.out.println(getLastAttackXCoordinate(i) + "," +getLastAttackYCoordinate(i));
//            
//        }
    }
    
    //TODO: Combine both methods to optimize bytecode usage
    /**
     * Query data structure
     * 
     * 0 indexed.
     * 
     * If number >= NUMBER_OF_STORED_EVENTS, the events returned will repeat
     * cyclically.
     * 
     * @param number
     *            0 for the last attacked location, will return
     *            Integer.MAX_VALUE on 1 + maximum number. <br>
     * <br>
     *            You should stop iteration then.
     * @return
     * @throws GameActionException
     */
    public static int getLastAttackXCoordinate(int number) throws GameActionException {
        numberOfEventsThatHaveOccurred = RobotPlayer.rc
                .readBroadcast(NUMBER_OF_EVENTS_THAT_HAVE_OCCURRED_CHANNEL);
        int relChannel = ((numberOfEventsThatHaveOccurred - number-1) * 2)
                % NUMBER_OF_CHANNELS_USED;
        if (relChannel < 0)
            return RobotPlayer.rc.readBroadcast(BASE_CHANNEL+relChannel+NUMBER_OF_CHANNELS_USED);
        else
            return RobotPlayer.rc.readBroadcast(BASE_CHANNEL+relChannel);
    }
    
    /**
     * Query data structure
     * 
     * 0 indexed.
     * 
     * If number >= NUMBER_OF_STORED_EVENTS, the events returned will repeat
     * cyclically.
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
        numberOfEventsThatHaveOccurred = RobotPlayer.rc
                .readBroadcast(NUMBER_OF_EVENTS_THAT_HAVE_OCCURRED_CHANNEL);

        int relChannel = 1
                + ((numberOfEventsThatHaveOccurred - number-1) * 2)
                % NUMBER_OF_CHANNELS_USED;
        if (relChannel < 0)
            return RobotPlayer.rc.readBroadcast(relChannel+BASE_CHANNEL+NUMBER_OF_CHANNELS_USED);
        else
            return RobotPlayer.rc.readBroadcast(relChannel+BASE_CHANNEL);
    }
    
    /**
     * Gets the latest number of events that have occurred.
     * @return
     * @throws GameActionException
     */
    public static int getNumberOfEventsThatHaveOccurred() throws GameActionException {
        numberOfEventsThatHaveOccurred = RobotPlayer.rc
                .readBroadcast(NUMBER_OF_EVENTS_THAT_HAVE_OCCURRED_CHANNEL);

        return numberOfEventsThatHaveOccurred;
    }
    
}
