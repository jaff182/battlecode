package launcherBot.Utility;

import launcherBot.Channels;
import launcherBot.Common;
import battlecode.common.*;


public class TankDefenseCount {

    public static final int TOWER_BASE_CHANNEL = Channels.TANK_DEFENSE_COUNT + 1;
    public static final int HQ_CHANNEL = Channels.TANK_DEFENSE_COUNT;
    
    /**
     * ONLY CALL IN TANKS.
     * 
     * Call whenever round number mod 10 is 5.
     * Statistic is used by tanks to decide on location to defend.
     * Reports if tank is defending tower or hq corresponding to channel.
     * 
     * @param channel
     * @return true if tank needs to continue defending tower or hq corresponding to channel, and false otherwise. 
     * @throws GameActionException
     */
    public static boolean report(int channel) throws GameActionException {
        if (Common.rc.getHealth() > 30) {
            int channelValue = Common.rc.readBroadcast(channel);
            if (channelValue > 0) {
                Common.rc.broadcast(channel, channelValue - 1);  
                return true;
            } else {
                return false;
            }
        }
        return true;
    }
    
    
    /**
     * Reset count of number of defense units.
     * 
     * HQ/TOWERS MUST EACH CALL THIS EVERY 10 ROUNDS. (when round number mod 10 is 0).
     * 
     * @param unitsNeeded number of defense units needed
     * @throws GameActionException 
     */
    public static void reset(int channel, int unitsNeeded) throws GameActionException {
        Common.rc.broadcast(channel, unitsNeeded);
    }
}
