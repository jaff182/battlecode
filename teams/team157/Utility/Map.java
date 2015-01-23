package team157.Utility;

import java.util.Random;

import team157.RobotPlayer;
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
        return (value/8)*8 + pathState.ordinal();
    }
    
    /**
     * Encodes the pathability state information into the allocated bits (lowest 3 
     * bits) of an integer.
     * @param value The value to replace.
     * @param pathStateOrdinal The ordinal of the pathability state to write.
     * @return The new value to use.
     */
    public static int encodePathState(int value, int pathStateOrdinal) {
        return (value/8)*8 + pathStateOrdinal;
    }
    
    /**
     * Turns on the allocated bit (4th lowest bit) indicating being in base attack 
     * range (24) of the enemy HQ with 1 or less enemy towers.
     * @param value The value to replace.
     * @return The new value to use.
     */
    public static int encodeInEnemyHQBaseRange(int value) {
        return value%8 + (value/16)*16 + 8;
    }
    
    /**
     * Turns on the allocated bit (5th lowest bit) indicating being in buffed attack 
     * range (35) of the enemy HQ with 2 to 4 enemy towers.
     * @param value The value to replace.
     * @return The new value to use.
     */
    public static int encodeInEnemyHQBuffedRange(int value) {
        return value%16 + (value/32)*32 + 16;
    }
    
    /**
     * Turns on the allocated bit (6th lowest bit) indicating being in splashable 
     * region of the enemy HQ with 5 or more enemy towers.
     * @param value The value to replace.
     * @return The new value to use.
     */
    public static int encodeInEnemyHQSplashRegion(int value) {
        return value%32 + (value/64)*64 + 32;
    }
    
    /**
     * Turns on the allocated bit ((idx+7)th lowest bit) indicating being in the 
     * (idx)th enemy tower range.
     * @param value The value to replace.
     * @param towerIndex The 0-based index of the tower.
     * @return The new value to use.
     */
    public static int encodeInEnemyTowerRange(int value, int towerIndex) {
        int lowBits = 64;
        for(int i=0; i<towerIndex; i++) {
            lowBits *= 2;
        }
        return value%lowBits + (value/(2*lowBits))*2*lowBits + lowBits;
    }
    
    
    /**
     * Reads the pathability state information encoded in a integer.
     * @param value The value to read from.
     * @return The ordinal of the pathability state.
     */
    public static int decodePathStateOrdinal(int value) {
        return value%8;
    }
    
    /**
     * Reads the pathability state information encoded in a integer.
     * @param value The value to read from.
     * @return The ordinal of the pathability state.
     */
    public static PathState decodePathState(int value) {
        return pathStates[value%8];
    }
    
    /**
     * Checks if allocated bit (4th lowest bit) indicating being in base attack range 
     * (24) of the enemy HQ with 1 or less enemy towers is turned on.
     * @param value The value to read from.
     * @return True if in range.
     */
    public static boolean decodeInEnemyHQBaseRange(int value) {
        return ((value%16)/8 == 1);
    }
    
    /**
     * Checks if the allocated bit (5th lowest bit) indicating being in buffed attack 
     * range (35) of the enemy HQ with 2 to 4 enemy towers is turned on.
     * @param value The value to read from.
     * @return True if in range.
     */
    public static boolean decodeInEnemyHQBuffedRange(int value) {
        return ((value%32)/16 == 1);
    }
    
    /**
     * Checks if the allocated bit (6th lowest bit) indicating being in splashable 
     * region of the enemy HQ with 5 or more enemy towers is turned on.
     * @param value The value to read from.
     * @return True if in region.
     */
    public static boolean decodeInEnemyHQSplashRegion(int value) {
        return ((value%64)/32 == 1);
    }
    
    /**
     * Checks if the allocated bit ((idx+7)th lowest bit) indicating being in the 
     * (idx)th enemy tower range is turned on.
     * @param value The value to read from.
     * @param towerIndex The 0-based index of the tower.
     * @return True if in range.
     */
    public static boolean decodeInEnemyTowerRange(int value, int towerIndex) {
        int lowBits = 64;
        for(int i=0; i<towerIndex; i++) {
            lowBits *= 2;
        }
        return ((value%(2*lowBits))/lowBits == 1);
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
        //Same as return (3*ALLOCATED_WIDTH/2+RobotPlayer.HQLocation.x+RobotPlayer.enemyHQLocation.x-locX-mapx0)%ALLOCATED_WIDTH;
        return (195+RobotPlayer.HQLocation.x+RobotPlayer.enemyHQLocation.x-locX-mapx0)%130;
    }
    
    /**
     * Converts Y coordinate of the corresponding reflected position of a MapLocation 
     * to the first index of the internal map int array
     * @param locY 
     * @return 1st internal map index of reflected position
     */
    public static int locationToReflectedMapYIndex(int locY) {
        //Same as return (3*ALLOCATED_HEIGHT/2+RobotPlayer.HQLocation.y+RobotPlayer.enemyHQLocation.y-locY-mapy0)%ALLOCATED_HEIGHT;
        return (195+RobotPlayer.HQLocation.y+RobotPlayer.enemyHQLocation.y-locY-mapy0)%130;
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
            PathState pathState = pathStates[decodePathStateOrdinal(value)];
            switch(pathState) {
                case NORMAL:
                case VOID:
                case OFF_MAP:
                    //Copy pathability state to symmetric position
                    int value2 = map[yidx][xidx];
                    value2 = encodePathState(value2,pathState);
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
            PathState pathState = pathStates[decodePathStateOrdinal(value)];
            switch(pathState) {
                case NORMAL:
                case VOID:
                case OFF_MAP:
                    //Copy pathability state to symmetric position
                    int value2 = map[yidx][xidx];
                    value2 = encodePathState(value2,pathState);
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
            //Copy pathability state to symmetric position
            int value2 = RobotPlayer.rc.readBroadcast(mapIndexToChannel(xidx,yidx));
            value2 = encodePathState(value2,decodePathStateOrdinal(value));
            RobotPlayer.rc.broadcast(mapIndexToChannel(xidx,yidx), value2);
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
            //Copy pathability state to symmetric position
            int pathStateOrdinal = decodePathStateOrdinal(value);
            int value2 = map[yidx][xidx];
            value2 = encodePathState(value2,pathStateOrdinal);
            map[yidx][xidx] = value2;
            value2 = RobotPlayer.rc.readBroadcast(mapIndexToChannel(xidx,yidx));
            value2 = encodePathState(value2,pathStateOrdinal);
            RobotPlayer.rc.broadcast(mapIndexToChannel(xidx,yidx), value2);
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
                } else if(decodeInEnemyHQBaseRange(value)) {
                    return "+";
                } else if(decodeInEnemyHQBuffedRange(value)) {
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