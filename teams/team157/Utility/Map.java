package team157.Utility;

import java.util.Random;
import battlecode.common.*;

public class Map {
    
    //Global variables ========================================================
    
    /**
     * mapx0,mapy0 are the MapLocation coordinates of the HQ-to-HQ midpoint,
     */
    public static int mapx0, mapy0;

    public static final int rotationSymmetry = 3;

    /**
     * symmetry represents the symmetry type of the map, in its last 2 bits for y and 
     * x reflections, so 0: unknown,  1: x-reflection,  2: y-reflection, 3: rotational
     */
    public static int symmetry=0;
    
    /**
     * Allocated size of the internal and radio maps. Since sensing region can be as 
     * much as 5 units away, we add 5 to prevent overflowing to the other side of the 
     * map.
     */
    public static final int ALLOCATED_WIDTH = GameConstants.MAP_MAX_WIDTH+5,
                            ALLOCATED_HEIGHT = GameConstants.MAP_MAX_HEIGHT+5;
    
    /**
     * Internal map is toroidal and approximately centered at midpoint of HQs.
     * Map representation modulo 6:
     *  0: unknown,  1: normal,  2: enemy HQ,  3: HQ,  4: void,  5: out of map
     *
     *  (ordinal values for terrain tiles: 2: unknown, 1: void, 3: offmap, 0: normal)
     */
    public static int[][] map = new int[ALLOCATED_HEIGHT][ALLOCATED_WIDTH];
    
    
    
    //Conversion methods ======================================================
    
    /**
     * Converts X coordinate of a MapLocation to the second index of the internal map 
     * int array.
     * @param locX 
     * @return 2nd internal map index
     */
    public static int locationToMapXIndex(int locX) {
        //Same as return (3*ALLOCATED_WIDTH/2+locX-mapx0)%ALLOCATED_WIDTH;
        return (189+locX-mapx0)%126;
    }
    
    /**
     * Converts Y coordinate of a MapLocation to the first index of the internal map 
     * int array.
     * @param locY 
     * @return 1st internal map index
     */
    public static int locationToMapYIndex(int locY) {
        //Same as return (3*ALLOCATED_HEIGHT/2+locY-mapy0)%ALLOCATED_HEIGHT;
        return (189+locY-mapy0)%126;
    }
    
    /**
     * Converts X coordinate of the corresponding reflected position of a MapLocation 
     * to the second index of the internal map int array
     * @param locX 
     * @return 2nd internal map index of reflected position
     */
    public static int locationToReflectedMapXIndex(int locX) {
        //Same as return (3*ALLOCATED_WIDTH/2+RobotPlayer.HQLocation.x+RobotPlayer.enemyHQLocation.x-locX-mapx0)%ALLOCATED_WIDTH;
        return (189+RobotPlayer.HQLocation.x+RobotPlayer.enemyHQLocation.x-locX-mapx0)%126;
    }
    
    /**
     * Converts Y coordinate of the corresponding reflected position of a MapLocation 
     * to the first index of the internal map int array
     * @param locY 
     * @return 1st internal map index of reflected position
     */
    public static int locationToReflectedMapYIndex(int locY) {
        //Same as return (3*ALLOCATED_HEIGHT/2+RobotPlayer.HQLocation.y+RobotPlayer.enemyHQLocation.y-locY-mapy0)%ALLOCATED_HEIGHT;
        return (189+RobotPlayer.HQLocation.y+RobotPlayer.enemyHQLocation.y-locY-mapy0)%126;
    }
    
    /**
     * Converts the x and y index of the internal map int array to the corresponding 
     * index on the radio map int array.
     * @param 
     */
    public static int mapIndexToChannel(int xidx, int yidx) {
        //Same as return xidx*ALLOCATED_HEIGHT+yidx+Channels.MAP_DATA;
        return xidx*126+yidx+Channels.MAP_DATA;
    }
    
    
    //Accessing methods =======================================================
    
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
        if(symmetry%4 != 0) map[yidx][xidx] = value;
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
        if(symmetry%4 != 0) map[yidx][xidx] = value;
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
        if(symmetry%4 != 0) RobotPlayer.rc.broadcast(mapIndexToChannel(xidx,yidx), value);
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
            map[yidx][xidx] = value;
            RobotPlayer.rc.broadcast(mapIndexToChannel(xidx,yidx), value);
        }
    }
    
    
    //Display =================================================================
    
    /**
     * Print internal map to console.
     */
    public static void debug_printInternalMap() {
        for (int[] line: map) {
            for (int i: line) {
                System.out.print(i); 
                /**
                switch(i) {
                    case 0: System.out.print("?"); break; //unknown
                    case 1: System.out.print(" "); break; //normal
                    case 2: System.out.print("X"); break; //enemy HQ
                    case 3: System.out.print("O"); break; //HQ
                    case 4: System.out.print("."); break; //void
                    case 5: System.out.print("#"); break; //out of map
                }
                **/
            }
            System.out.println("");
        }
    }
    
    /**
     * Print radio map to console.
     */
    public static void debug_printRadioMap() throws GameActionException {
        for (int yidx=0; yidx<ALLOCATED_HEIGHT; yidx++) {
            for (int xidx=0; xidx<ALLOCATED_WIDTH; xidx++) {
                switch(RobotPlayer.rc.readBroadcast(mapIndexToChannel(xidx,yidx))) {
                    case 0: System.out.print("?"); break; //unknown
                    case 1: System.out.print(" "); break; //normal
                    case 2: System.out.print("X"); break; //enemy HQ
                    case 3: System.out.print("O"); break; //HQ
                    case 4: System.out.print("."); break; //void
                    case 5: System.out.print("#"); break; //out of map
                }
            }
            System.out.println("");
        }
    }
    
    
    
}