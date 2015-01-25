package team157.Utility;

import java.util.Random;

import team157.RobotPlayer;
import team157.Common;
import team157.Channels;
import battlecode.common.*;

public class Map {
    
    //Global variables ========================================================
    
    /**
     * mapx0,mapy0 are the MapLocation coordinates of the HQ-to-HQ midpoint,
     */
    public static int mapx0, mapy0;

    /**
     * symmetry represents the symmetry type of the map, in its last 2 bits for y and 
     * x reflections, so 0: unknown,  1: x-reflection,  2: y-reflection, 3: rotational
     */
    public static int symmetry=0;
    
    /**
     * Allocated size of the internal and radio maps. Since sensing region can be as 
     * much as 5 units away, we add 10 to prevent overflowing to the other side of the 
     * map.
     */
    public static final int ALLOCATED_WIDTH = GameConstants.MAP_MAX_WIDTH+10;
    
    /**
     * Allocated size of the internal and radio maps. Since sensing region can be as 
     * much as 5 units away, we add 10 to prevent overflowing to the other side of the 
     * map.
     */
    public static final int ALLOCATED_HEIGHT = GameConstants.MAP_MAX_HEIGHT+10;
    
    /**
     * Internal map is toroidal and approximately centered at midpoint of HQs.
     */
    public static int[][] map = new int[ALLOCATED_HEIGHT][ALLOCATED_WIDTH];
    
    
    //Encoding and Decoding methods ===========================================
    
    //0...000000000111 in binary
    private static final int pathStateBitMask = 7;
    //0...000000001000 in binary
    private static final int enemyHQBaseRangeBitMask = 8;
    //0...000000010000 in binary
    private static final int enemyHQBuffedRangeBitMask = 16;
    //0...000000100000 in binary
    private static final int enemyHQSplashRegionBitMask = 32;
    //0...000001000000 in binary
    private static final int enemyTowerBaseBitMask = 64;
    
    
    /**
     * The pathability state of a given tile, to be recorded in the internal and 
     * radio maps.
     * Map representation modulo 8:
     *  0: unknown,  1: normal,  2: void,  3: enemy HQ,  4: HQ,  5: off map
     *  (ordinal values for terrain tiles: 2: unknown, 0: normal, 1: void, 3: offmap)
     */
    public static enum PathState {
        UNKNOWN, NORMAL, VOID, ENEMY_HQ, HQ, OFF_MAP,
    }
    public static final PathState[] pathStates = PathState.values();
    
    /**
     * Encodes the pathability state information into the allocated bits (lowest 3 
     * bits) of an integer.
     * @param value The value to replace.
     * @param pathState The pathability state to write.
     * @return The new value to use.
     */
    public static int encodePathState(int value, PathState pathState) {
        return (value & ~pathStateBitMask) | pathState.ordinal();
    }
    
    /**
     * Encodes the pathability state information into the allocated bits (lowest 3 
     * bits) of an integer.
     * @param value The value to replace.
     * @param pathStateOrdinal The ordinal of the pathability state (0 to 7) to write.
     * @return The new value to use.
     */
    public static int encodePathState(int value, int pathStateOrdinal) {
        return (value & ~pathStateBitMask) | pathStateOrdinal;
    }
    
    /**
     * Turns on the allocated bit (4th lowest bit) indicating being in base attack 
     * range (24) of the enemy HQ with 1 or less enemy towers.
     * @param value The value to replace.
     * @return The new value to use.
     */
    public static int encodeInEnemyHQBaseRange(int value) {
        return (value & ~enemyHQBaseRangeBitMask) | enemyHQBaseRangeBitMask;
    }
    
    /**
     * Turns on the allocated bit (5th lowest bit) indicating being in buffed attack 
     * range (35) of the enemy HQ with 2 to 4 enemy towers.
     * @param value The value to replace.
     * @return The new value to use.
     */
    public static int encodeInEnemyHQBuffedRange(int value) {
        return (value & ~enemyHQBuffedRangeBitMask) | enemyHQBuffedRangeBitMask;
    }
    
    /**
     * Turns on the allocated bit (6th lowest bit) indicating being in splashable 
     * region of the enemy HQ with 5 or more enemy towers.
     * @param value The value to replace.
     * @return The new value to use.
     */
    public static int encodeInEnemyHQSplashRegion(int value) {
        return (value & ~enemyHQSplashRegionBitMask) | enemyHQSplashRegionBitMask;
    }
    
    /**
     * Turns on the allocated bit ((idx+7)th lowest bit) indicating being in the 
     * (idx)th enemy tower range.
     * @param value The value to replace.
     * @param towerIndex The 0-based index of the tower.
     * @return The new value to use.
     */
    public static int encodeInEnemyTowerRange(int value, int towerIndex) {
        int enemyTowerBitMask = enemyTowerBaseBitMask << towerIndex;
        return (value & ~enemyTowerBitMask) | enemyTowerBitMask;
    }
    
    
    /**
     * Reads the pathability state information encoded in a integer.
     * @param value The value to read from.
     * @return The ordinal of the pathability state.
     */
    public static int decodePathStateOrdinal(int value) {
        return (value & pathStateBitMask);
    }
    
    /**
     * Reads the pathability state information encoded in a integer.
     * @param value The value to read from.
     * @return The ordinal of the pathability state.
     */
    public static PathState decodePathState(int value) {
        return pathStates[value & pathStateBitMask];
    }
    
    /**
     * Checks if allocated bit (4th lowest bit) indicating being in base attack range 
     * (24) of the enemy HQ with 1 or less enemy towers is turned on.
     * @param value The value to read from.
     * @return True if in range.
     */
    public static boolean decodeInEnemyHQBaseRange(int value) {
        return ((value & enemyHQBaseRangeBitMask) == enemyHQBaseRangeBitMask);
    }
    
    /**
     * Checks if the allocated bit (5th lowest bit) indicating being in buffed attack 
     * range (35) of the enemy HQ with 2 to 4 enemy towers is turned on.
     * @param value The value to read from.
     * @return True if in range.
     */
    public static boolean decodeInEnemyHQBuffedRange(int value) {
        return ((value & enemyHQBuffedRangeBitMask) == enemyHQBuffedRangeBitMask);
    }
    
    /**
     * Checks if the allocated bit (6th lowest bit) indicating being in splashable 
     * region of the enemy HQ with 5 or more enemy towers is turned on.
     * @param value The value to read from.
     * @return True if in region.
     */
    public static boolean decodeInEnemyHQSplashRegion(int value) {
        return ((value & enemyHQSplashRegionBitMask) == enemyHQSplashRegionBitMask);
    }
    
    /**
     * Checks if the allocated bit ((idx+7)th lowest bit) indicating being in the 
     * (idx)th enemy tower range is turned on.
     * @param value The value to read from.
     * @param towerIndex The 0-based index of the tower.
     * @return True if in range.
     */
    public static boolean decodeInEnemyTowerRange(int value, int towerIndex) {
        int enemyTowerBitMask = enemyTowerBaseBitMask << towerIndex;
        return ((value & enemyTowerBitMask) == enemyTowerBitMask);
    }
    
    
    
    //Conversion methods ======================================================
    
    /**
     * Converts X coordinate of a MapLocation to the second index of the internal map 
     * int array.
     * @param locX 
     * @return 2nd internal map index
     */
    public static int locationToMapXIndex(int locX) {
        //Same as return (3*ALLOCATED_WIDTH/2+locX-mapx0)%ALLOCATED_WIDTH;
        return (195+locX-mapx0)%130;
    }
    
    /**
     * Converts Y coordinate of a MapLocation to the first index of the internal map 
     * int array.
     * @param locY 
     * @return 1st internal map index
     */
    public static int locationToMapYIndex(int locY) {
        //Same as return (3*ALLOCATED_HEIGHT/2+locY-mapy0)%ALLOCATED_HEIGHT;
        return (195+locY-mapy0)%130;
    }
    
    /**
     * Converts X coordinate of the corresponding reflected position of a MapLocation 
     * to the second index of the internal map int array
     * @param locX 
     * @return 2nd internal map index of reflected position
     */
    public static int locationToReflectedMapXIndex(int locX) {
        //Same as return (3*ALLOCATED_WIDTH/2+Common.HQLocation.x+Common.enemyHQLocation.x-locX-mapx0)%ALLOCATED_WIDTH;
        return (195+Common.HQLocation.x+Common.enemyHQLocation.x-locX-mapx0)%130;
    }
    
    /**
     * Converts Y coordinate of the corresponding reflected position of a MapLocation 
     * to the first index of the internal map int array
     * @param locY 
     * @return 1st internal map index of reflected position
     */
    public static int locationToReflectedMapYIndex(int locY) {
        //Same as return (3*ALLOCATED_HEIGHT/2+Common.HQLocation.y+Common.enemyHQLocation.y-locY-mapy0)%ALLOCATED_HEIGHT;
        return (195+Common.HQLocation.y+Common.enemyHQLocation.y-locY-mapy0)%130;
    }
    
    /**
     * Converts the x and y index of the internal map int array to the corresponding 
     * index on the radio map int array.
     * @param 
     */
    public static int mapIndexToChannel(int xidx, int yidx) {
        //Same as return xidx*ALLOCATED_HEIGHT+yidx+Channels.MAP_DATA;
        return xidx*130+yidx+Channels.MAP_DATA;
    }
    
    
    //Basic accessing methods =================================================
    
    /**
     * Sets value in internal map (deprecated, use setInternalMap(int locX, int 
     * locY, int value) instead for better bytecode efficiency)
     * @param loc MapLocation to set value.
     * @param value Value to be set.
     */
    public static void setInternalMap(MapLocation loc, int value) {
        int xidx = locationToMapXIndex(loc.x);
        int yidx = locationToMapYIndex(loc.y);
        map[yidx][xidx] = value;
        if(symmetry%2 == 1) xidx = locationToReflectedMapXIndex(loc.x);
        if((symmetry/2)%2 == 1) yidx = locationToReflectedMapYIndex(loc.y);
        if(symmetry%4 != 0) {
            int pathStateOrdinal = decodePathStateOrdinal(value);
            switch(pathStateOrdinal) {
                case 1/*NORMAL*/:
                case 2/*VOID*/:
                case 5/*OFF_MAP*/:
                    //Copy pathability state to symmetric position
                    int value2 = map[yidx][xidx];
                    value2 = encodePathState(value2,pathStateOrdinal);
                    map[yidx][xidx] = value2;
            }
        }
    }
    
    /**
     * Sets value in internal map
     * @param locX X coordinate of MapLocation to set value.
     * @param locY Y coordinate of MapLocation to set value.
     * @param value Value to be set.
     */
    public static void setInternalMap(int locX, int locY, int value) {
        int xidx = locationToMapXIndex(locX);
        int yidx = locationToMapYIndex(locY);
        map[yidx][xidx] = value;
        if(symmetry%2 == 1) xidx = locationToReflectedMapXIndex(locX);
        if((symmetry/2)%2 == 1) yidx = locationToReflectedMapYIndex(locY);
        if(symmetry%4 != 0) {
            int pathStateOrdinal = decodePathStateOrdinal(value);
            switch(pathStateOrdinal) {
                case 1/*NORMAL*/:
                case 2/*VOID*/:
                case 5/*OFF_MAP*/:
                    //Copy pathability state to symmetric position
                    int value2 = map[yidx][xidx];
                    value2 = encodePathState(value2,pathStateOrdinal);
                    map[yidx][xidx] = value2;
            }
        }
    }
    
    
    /**
     * Sets value in internal map without using the symmetry (deprecated, use 
     * setInternalMapWithoutSymmetry(int locX, int locY, int value) instead for 
     * better bytecode efficiency)
     * @param loc MapLocation to set value.
     * @param value Value to be set.
     */
    public static void setInternalMapWithoutSymmetry(MapLocation loc, int value) {
        int xidx = locationToMapXIndex(loc.x);
        int yidx = locationToMapYIndex(loc.y);
        map[yidx][xidx] = value;
    }
    
    /**
     * Sets value in internal map without using the symmetry.
     * @param locX X coordinate of MapLocation to set value.
     * @param locY Y coordinate of MapLocation to set value.
     * @param value Value to be set.
     */
    public static void setInternalMapWithoutSymmetry(int locX, int locY, int value) {
        int xidx = locationToMapXIndex(locX);
        int yidx = locationToMapYIndex(locY);
        map[yidx][xidx] = value;
    }

    /**
     * Gets value in internal map (deprecated, use getInternalMap_(int locX, int locY) 
     * instead for better bytecode efficiency). Underscore is to differentiate from 
     * Lynn's version which takes the map index directly as arguments.
     * @param loc MapLocation to get value.
     * @return map value
     */
    public static int getInternalMap(MapLocation loc) {
        int xidx = locationToMapXIndex(loc.x);
        int yidx = locationToMapYIndex(loc.y);
        return map[yidx][xidx];
    }
    
    /**
     * Gets value in internal map
     * @param locX X coordinate of MapLocation to get value.
     * @param locY Y coordinate of MapLocation to get value.
     * @return map value
     */
    public static int getInternalMap_(int locX, int locY) {
        int xidx = locationToMapXIndex(locX);
        int yidx = locationToMapYIndex(locY);
        return map[yidx][xidx];
    }

    /**
     * Gets value in internal map (Lynn's version which accepts int array index 
     * arguments rather than MapLocation coordinates).
     * @param xidx x index of internal map.
     * @param yidx y index of internal map.
     * @return value in internal map at input location.
     */
    public static int getInternalMap(int xidx, int yidx) {
        return map[yidx][xidx];
    }

    /**
     * Resets internal map.
     */
    public static void resetInternalMap() {
        map = new int[ALLOCATED_WIDTH][ALLOCATED_HEIGHT];
    }

    /**
     * Sets value in radio map
     * @param locX X coordinate of MapLocation to set value.
     * @param locY Y coordinate of MapLocation to set value.
     * @param value Value to be set.
     * @throws GameActionException
     */
    public static void setRadioMap(int locX, int locY, int value) throws GameActionException {
        int xidx = locationToMapXIndex(locX);
        int yidx = locationToMapYIndex(locY);
        RobotPlayer.rc.broadcast(mapIndexToChannel(xidx,yidx), value);
        if(symmetry%2 == 1) xidx = locationToReflectedMapXIndex(locX);
        if((symmetry/2)%2 == 1) yidx = locationToReflectedMapYIndex(locY);
        if(symmetry%4 != 0) {
            int pathStateOrdinal = decodePathStateOrdinal(value);
            switch(pathStateOrdinal) {
                case 1/*NORMAL*/:
                case 2/*VOID*/:
                case 5/*OFF_MAP*/:
                    //Copy pathability state to symmetric position
                    int channel = mapIndexToChannel(xidx,yidx);
                    int value2 = RobotPlayer.rc.readBroadcast(channel);
                    if(decodePathStateOrdinal(value2) != pathStateOrdinal) {
                        //Path state is not updated
                        value2 = encodePathState(value2,pathStateOrdinal);
                        RobotPlayer.rc.broadcast(channel, value2);
                    }
            }
        }
    }
    
    
    /**
     * Gets value in radio map
     * @param locX X coordinate of MapLocation to get value.
     * @param locY Y coordinate of MapLocation to get value.
     * @return map value
     * @throws GameActionException
     */
    public static int getRadioMap(int locX, int locY) throws GameActionException {
        int xidx = locationToMapXIndex(locX);
        int yidx = locationToMapYIndex(locY);
        return RobotPlayer.rc.readBroadcast(mapIndexToChannel(xidx,yidx));
    }
    
    
    /**
     * Set both internal and radio maps
     * @param locX X coordinate of MapLocation to set value.
     * @param locY Y coordinate of MapLocation to set value.
     * @param value Value to be set
     * @throws GameActionException
     */
    public static void setMaps(int locX, int locY, int value) throws GameActionException {
        int xidx = locationToMapXIndex(locX);
        int yidx = locationToMapYIndex(locY);
        map[yidx][xidx] = value;
        RobotPlayer.rc.broadcast(mapIndexToChannel(xidx,yidx), value);
        if(symmetry%2 == 1) xidx = locationToReflectedMapXIndex(locX);
        if((symmetry/2)%2 == 1) yidx = locationToReflectedMapYIndex(locY);
        if(symmetry%4 != 0) {
            int pathStateOrdinal = decodePathStateOrdinal(value);
            switch(pathStateOrdinal) {
                case 1/*NORMAL*/:
                case 2/*VOID*/:
                case 5/*OFF_MAP*/:
                    //Copy pathability state to symmetric position
                    int channel = mapIndexToChannel(xidx,yidx);
                    int value2 = RobotPlayer.rc.readBroadcast(channel);
                    if(decodePathStateOrdinal(value2) != pathStateOrdinal) {
                        //Path state is not updated
                        //Assume radio map is more up-to-date than internal map
                        value2 = encodePathState(value2,pathStateOrdinal);
                        map[yidx][xidx] = value2;
                        RobotPlayer.rc.broadcast(channel, value2);
                    }
            }
        }
    }
    
    
    //Enemy attack range control methods ======================================
    
    /**
     * Gets the mob level containing information about which enemy attack regions can 
     * be traversed.
     * @return The mob level.
     */
    public static int getMobLevel() throws GameActionException {
        return RobotPlayer.rc.readBroadcast(Channels.MOB_LEVEL);
    }
    
    /**
     * Sets the mob level containing information about which enemy attack regions can 
     * be traversed to a specified value.
     * @param The new mob level.
     */
    public static void setMobLevel(int value) throws GameActionException {
        RobotPlayer.rc.broadcast(Channels.MOB_LEVEL,value);
    }
    
    /**
     * Sets the mob level to make the base attack range of the enemy HQ traversable.
     */
    public static void turnOffEnemyHQBaseRange() throws GameActionException {
        int value = RobotPlayer.rc.readBroadcast(Channels.MOB_LEVEL);
        int value2 = (value | enemyHQBaseRangeBitMask);
        if(value != value2) {
            RobotPlayer.rc.broadcast(Channels.MOB_LEVEL,value2);
        }
    }
    
    /**
     * Sets the mob level to make the buffed attack range of the enemy HQ traversable.
     */
    public static void turnOffEnemyHQBuffedRange() throws GameActionException {
        int value = RobotPlayer.rc.readBroadcast(Channels.MOB_LEVEL);
        int value2 = (value | enemyHQBuffedRangeBitMask);
        if(value != value2) {
            RobotPlayer.rc.broadcast(Channels.MOB_LEVEL,value2);
        }
    }
    
    /**
     * Sets the mob level to make the splash region of the enemy HQ traversable.
     */
    public static void turnOffEnemyHQSplashRegion() throws GameActionException {
        int value = RobotPlayer.rc.readBroadcast(Channels.MOB_LEVEL);
        int value2 = (value | enemyHQSplashRegionBitMask);
        if(value != value2) {
            RobotPlayer.rc.broadcast(Channels.MOB_LEVEL,value2);
        }
    }
    
    /**
     * Sets the mob level to make the attack range of the specified enemy tower 
     * traversable.
     * @param towerIndex The original 0-based index of the tower.
     */
    public static void turnOffEnemyTowerRange(int towerIndex) throws GameActionException {
        int value = RobotPlayer.rc.readBroadcast(Channels.MOB_LEVEL);
        int value2 = (value | (enemyTowerBaseBitMask << towerIndex));
        if(value != value2) {
            RobotPlayer.rc.broadcast(Channels.MOB_LEVEL,value2);
        }
    }
    
    
    /**
     * Checks the mob level if the base attack range of the enemy HQ has been made 
     * traversable.
     * @return True if traversable.
     */
    public static boolean isEnemyHQBaseRangeTurnedOff() {
        return ((Common.mobLevel & enemyHQBaseRangeBitMask) == enemyHQBaseRangeBitMask);
    }
    
    /**
     * Checks the mob level if the buffed attack range of the enemy HQ has been made 
     * traversable.
     * @return True if traversable.
     */
    public static boolean isEnemyHQBuffedRangeTurnedOff() {
        return ((Common.mobLevel & enemyHQBuffedRangeBitMask) == enemyHQBuffedRangeBitMask);
    }
    
    /**
     * Checks the mob level if the splash region of the enemy HQ has been made  
     * traversable.
     * @return True if traversable.
     */
    public static boolean isEnemyHQSplashRegionTurnedOff() {
        return ((Common.mobLevel & enemyHQSplashRegionBitMask) == enemyHQSplashRegionBitMask);
    }
    
    /**
     * Checks the mob level if the attack range of the specified enemy tower has been 
     * made traversable.
     * @param towerIndex The index of the tower.
     * @return True if traversable.
     */
    public static boolean isEnemyTowerRangeTurnedOff(int towerIndex) {
        int enemyTowerBitMask = enemyTowerBaseBitMask << towerIndex;
        return ((Common.mobLevel & enemyTowerBitMask) == enemyTowerBitMask);
    }
    
    
    //Other methods ===========================================================
    
    /**
     * Checks pathability of a location using in order: internal map, then radio map, 
     * but no direct sensing to save bytecode. Updates accordingly. Need to be called 
     * with isPathable() or canMove() first since it only queries map values. Use 
     * only in movePossible().
     * @param loc Location to query
     * @return True if map values alone do not indicate movement restrictions
     */
    public static boolean checkNotBlocked(MapLocation loc) throws GameActionException {
        //In interest of bytecode conservation, the abstraction for methods in 
        //Map.java will be broken in this method, since this is a core method used 
        //rather frequently.
        
        //Check internal map pathability first
        //The following is "int value = getInternalMap_(loc.x,loc.y);"
        int xidx = locationToMapXIndex(loc.x);
        int yidx = locationToMapYIndex(loc.y);
        int value = map[yidx][xidx];
        
        if(value != 0) {
            //Internal map enemy attack region information has definitely been updated
            //even if pathability information has not
            //The following check does essentially
            /*
            if(decodeInEnemyHQBaseRange(value) 
                && !isEnemyHQBaseRangeTurnedOff()) {
                    //In enemy HQ base range which is not turned off
                    return false;
            } else if(decodeInEnemyHQBuffedRange(value) 
                && !isEnemyHQBuffedRangeTurnedOff()) {
                    //In enemy HQ buffed range which is not turned off
                    return false;
            } else if(decodeInEnemyHQSplashRegion(value) 
                && !isEnemyHQSplashRegionTurnedOff()) {
                    //In enemy HQ buffed range which is not turned off
                    return false;
            }
            for(int i=0; i<Common.enemyTowers.length; i++) {
                int enemyTowerBitMask = enemyTowerBaseBitMask << i;
                if(decodeInEnemyTowerRange(value,i) && !isEnemyTowerRangeTurnedOff(i)) {
                    //In ith enemy tower's range which is not turned off
                    return false;
                }
            }//*/
            if(((value & ~pathStateBitMask) & ~Common.mobLevel) != 0) {
                return false;
            }
        }
        
        //The following contains a check if decodePathStateOrdinal(value) is zero
        if(value == 0 || (value & pathStateBitMask) == 0) {
            //Internal PathState is UNKNOWN
            //Assume radio map is more up-to-date, can overwrite internal map
            //Check radio map
            //The following is "int value = getRadioMap(loc.x,loc.y);"
            value = RobotPlayer.rc.readBroadcast(mapIndexToChannel(xidx,yidx));
            if(value != 0) {
                //Update internal map
                //radio is more up to date so just copy into internal map as well
                setInternalMap(loc.x,loc.y,value);
                //Can check if in enemy attack region again
                //The following check is the same as the comment earlier
                if(((value & ~pathStateBitMask) & ~Common.mobLevel) != 0) {
                    return false;
                }
            }
            //No sensing in order to save bytecodes
        }
        
        return true;
    }
    
    
    /**
     * Checks and updates in order: internal map, then radio map, then direct sensing.
     * @param loc Location to query
     */
    public static void updateOrSense(MapLocation loc) throws GameActionException {
        //In interest of bytecode conservation, the abstraction for methods in 
        //Map.java will be broken in this method, since this is a core method used 
        //rather frequently.
        
        //Check internal map first
        //The following is "int value = getInternalMap_(loc.x,loc.y);"
        //and "int pathStateOrdinal = decodePathStateOrdinal(value);"
        int xidx = locationToMapXIndex(loc.x);
        int yidx = locationToMapYIndex(loc.y);
        int value = map[yidx][xidx];
        int pathStateOrdinal = (value & pathStateBitMask);
        
        if(pathStateOrdinal == 0) {
            //Internal PathState is UNKNOWN
            //Check radio map pathability next
            //The following is "int valueRadio = getRadioMap(loc.x,loc.y);"
            value = RobotPlayer.rc.readBroadcast(mapIndexToChannel(xidx,yidx));
            
            if(value != 0) {
                //The following is "int pathStateOrdinal = decodePathStateOrdinal(value);"
                pathStateOrdinal = (value & pathStateBitMask);
                if(pathStateOrdinal == 0) {
                    //Radio PathState is UNKNOWN
                    //Sense directly for terrain tile
                    switch (RobotPlayer.rc.senseTerrainTile(loc)) {
                        case VOID: pathStateOrdinal = 2/*VOID*/; break;
                        case NORMAL: pathStateOrdinal = 1/*NORMAL*/; break;
                        case OFF_MAP: pathStateOrdinal = 5/*OFF_MAP*/; break;
                        case UNKNOWN: pathStateOrdinal = 0/*UNKNOWN*/; break;
                        default: break;
                    }
                    //The following is
                    //"value = encodePathState(value,pathStateOrdinal);"
                    value = (value & ~pathStateBitMask) | pathStateOrdinal;
                    
                    //radio is more up to date so just copy into internal map as well
                    setMaps(loc.x,loc.y,value);
                } else {
                    //Radio PathState is set
                    //Update internal map
                    //radio is more up to date so just copy into internal map as well
                    setInternalMap(loc.x,loc.y,value);
                }
            }
        }
    }
    
    
    /**
     * Initialize radio map with enemy HQ and tower locations, attack regions, etc
     * HQ must run this before symmetry is set (not crucial, just that it will be 
     * faster).
     */
    public static void initRadioMap() throws GameActionException {
        //Set HQ
        MapLocation HQLocation = Common.HQLocation;
        setRadioMap(HQLocation.x,HQLocation.y,encodePathState(0,PathState.HQ));
        //System.out.println(""+Clock.getBytecodesLeft());
        
        //Set enemy towers
        MapLocation[] enemyTowers = Common.enemyTowers;
        for (int i=0; i<enemyTowers.length; i++) {
            MapLocation[] inRange = MapLocation.getAllMapLocationsWithinRadiusSq
                (enemyTowers[i],RobotType.TOWER.attackRadiusSquared);
            for (MapLocation loc: inRange) {
                int value = getRadioMap(loc.x,loc.y);
                value = encodeInEnemyTowerRange(value,i);
                setRadioMap(loc.x,loc.y,value);
            }
            //System.out.println(""+Clock.getBytecodesLeft());
        }
        
        //Set enemy HQ
        MapLocation enemyHQLocation = Common.enemyHQLocation;
        MapLocation[] inRange = MapLocation.getAllMapLocationsWithinRadiusSq
            (enemyHQLocation,52);
        for(MapLocation loc: inRange) {
            int distance = enemyHQLocation.distanceSquaredTo(loc);
            int value = getRadioMap(loc.x,loc.y);
            if(distance == 0) {
                //Set pathability to ENEMY_HQ
                value = encodePathState(value,PathState.ENEMY_HQ);
            }
            if(distance <= RobotType.HQ.attackRadiusSquared) {
                //in enemy HQ base attack range
                value = encodeInEnemyHQBaseRange(value);
            }
            if(distance <= GameConstants.HQ_BUFFED_ATTACK_RADIUS_SQUARED 
                && enemyTowers.length >= 2) {
                    //in enemy HQ buffed attack range
                    value = encodeInEnemyHQBuffedRange(value);
            }
            if(Common.isInSplashRegion(loc,enemyHQLocation) 
                && enemyTowers.length >= 5) {
                    //in enemy HQ base attack range
                    value = encodeInEnemyHQSplashRegion(value);
            }
            setRadioMap(loc.x,loc.y,value);
        }
        
    }
    
    
    //Display =================================================================
    
    /**
     * Converts the encoded information in a map value into a representative symbol.
     * @param value The value to convert.
     * @return The corresponding symbol.
     */
    private static String getSymbol(int value) {
        switch(decodePathState(value)) {
            case UNKNOWN: return "?"; //unknown
            case VOID: return "."; //void
            case ENEMY_HQ: return "X"; //enemy HQ
            case HQ: return "O"; //my HQ
            case OFF_MAP: return "#"; //out of map
            case NORMAL: //normal
                if(decodeInEnemyHQBaseRange(value)) {
                    return "*";
                } else if(decodeInEnemyHQBuffedRange(value)) {
                    return "+";
                } else if(decodeInEnemyHQSplashRegion(value)) {
                    return "!";
                } else if(decodeInEnemyTowerRange(value,0)) {
                    return "0";
                } else if(decodeInEnemyTowerRange(value,1)) {
                    return "1";
                } else if(decodeInEnemyTowerRange(value,2)) {
                    return "2";
                } else if(decodeInEnemyTowerRange(value,3)) {
                    return "3";
                } else if(decodeInEnemyTowerRange(value,4)) {
                    return "4";
                } else if(decodeInEnemyTowerRange(value,5)) {
                    return "5";
                } else return " ";
        }
        return "?";
    }
    
    /**
     * Print internal map to console.
     */
    public static void printInternal() {
        for (int[] line: map) {
            for (int value: line) {
                System.out.print(getSymbol(value));
            }
            System.out.println("");
        }
    }
    
    /**
     * Print radio map to console.
     */
    public static void printRadio() throws GameActionException {
        for (int yidx=0; yidx<ALLOCATED_HEIGHT; yidx++) {
            for (int xidx=0; xidx<ALLOCATED_WIDTH; xidx++) {
                int value = RobotPlayer.rc.readBroadcast(mapIndexToChannel(xidx,yidx));
                System.out.print(getSymbol(value));
            }
            System.out.println("");
        }
    }
}