package team156.Maps;

import org.apache.commons.lang.NotImplementedException;

import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.RobotController;
import team156.*;

public class ThreatMap {
    /**
     * INCOMPLETE, DO NOT USE.
     */

    /**
     * Length of edge of each grid square inside this map.
     */
    public static final int GRID_SQUARE_LENGTH = 10;
    
    public static int X_ORIGIN = RobotPlayer.HQLocation.x;
    public static int Y_ORIGIN = RobotPlayer.HQLocation.y;

    public static int BASE_CHANNEL;
    public final static int NUMBER_OF_CHANNELS_USED = (2 * GameConstants.MAP_MAX_HEIGHT / GRID_SQUARE_LENGTH)
            * (2 * GameConstants.MAP_MAX_WIDTH / GRID_SQUARE_LENGTH);
    
    /**
     * Enum showing the type of threat in that grid location.
     * 
     * Miner - Only miners in that square sensed
     * 
     * Miners lightly guarded - Miner plus one other enemy unit sensed
     * 
     * Building_GUARDED - A building sensed in this square, together with 3 or more combat units
     * 
     * BUILDING_LIGHTLY_GUARDED - A building sensed in this square, together with 2 or less combat units
     * 
     * Enemies - 3 or more combat units (non miners/beavers) sensed in a square
     *
     */
    public enum ThreatType {
        MINER, MINERS_LIGHTLY_GUARDED, BUILDING_GUARDED, BUILDING_LIGHTLY_GUARDED
    }
    
    public static int report() {
        throw new NotImplementedException();
    }
    
    /**
     * Set the value of the grid square the point (x, y) is in to a.
     * 
     * @param x
     * The absolute x-value of the square (taken from MapLocation)
     * @param y
     * The absolute y-value of the square (taken from MapLocation)
     * @param a
     * The value to be set for that particular square
     * @throws GameActionException 
     */
    public static void set(int x, int y, int a) throws GameActionException {
        RobotPlayer.rc.broadcast(BASE_CHANNEL + getGridIndex(x, y), a);
    }
    
    /**
     * Reset the value of the grid square the point (x, y) is in.
     * 
     * @param x
     * @param y
     * @throws GameActionException 
     */
    public static void reset(int x, int y) throws GameActionException {
        RobotPlayer.rc.broadcast(BASE_CHANNEL+getGridIndex(x,y), 0);
    }
    
    /**
     * Set all values in the map to zero.
     * @throws GameActionException 
     */
    public static void reset() throws GameActionException {
        RobotController rc = RobotPlayer.rc; //bring into local scope once to avoid GETSTATIC call below
        for (int channel=BASE_CHANNEL; channel<BASE_CHANNEL+NUMBER_OF_CHANNELS_USED; ++channel) {
            rc.broadcast(channel, 0);
        }
    }
    
    /**
     * Get the corresponding index to the point given by (x, y)
     * 
     * 
     * @param x
     * The x-value corresponding to that grid location.
     * @param y
     * The y-value corresponding to that grid location.
     * @param the index representing that grid location
     */
    public static int getGridIndex(int x, int y) {
        //Max value of (x-X_ORIGIN) should be 119, min 0, so xIdx should have min value 0, max 23
        final int xIdx = ((x-X_ORIGIN)+GameConstants.MAP_MAX_WIDTH)/GRID_SQUARE_LENGTH; 
        final int yIdx = ((y-Y_ORIGIN)+GameConstants.MAP_MAX_HEIGHT)/GRID_SQUARE_LENGTH;
        return (2*GameConstants.MAP_MAX_WIDTH/GRID_SQUARE_LENGTH)*xIdx+yIdx;
    }

}
