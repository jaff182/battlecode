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
            int channel = mapIndexToChannel(xidx,yidx);
            value2 = RobotPlayer.rc.readBroadcast(channel);
            value2 = encodePathState(value2,pathStateOrdinal);
            RobotPlayer.rc.broadcast(channel, value2);
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
        int value = getMobLevel();
        value = encodeInEnemyHQBaseRange(value);
        setMobLevel(value);
    }
    
    /**
     * Sets the mob level to make the buffed attack range of the enemy HQ traversable.
     */
    public static void turnOffEnemyHQBuffedRange() throws GameActionException {
        int value = getMobLevel();
        value = encodeInEnemyHQBuffedRange(value);
        setMobLevel(value);
    }
    
    /**
     * Sets the mob level to make the splash region of the enemy HQ traversable.
     */
    public static void turnOffEnemyHQSplashRegion() throws GameActionException {
        int value = getMobLevel();
        value = encodeInEnemyHQSplashRegion(value);
        setMobLevel(value);
    }
    
    /**
     * Sets the mob level to make the attack range of the specified enemy tower 
     * traversable.
     */
    public static void turnOffEnemyTowerRange(int towerIndex) throws GameActionException {
        int value = getMobLevel();
        value = encodeInEnemyTowerRange(value,towerIndex);
        setMobLevel(value);
    }
    
    
    /**
     * Checks the mob level if the base attack range of the enemy HQ has been made 
     * traversable.
     * @return True if traversable.
     */
    public static boolean isEnemyHQBaseRangeTurnedOff() {
        return decodeInEnemyHQBaseRange(Common.mobLevel);
    }
    
    /**
     * Checks the mob level if the buffed attack range of the enemy HQ has been made 
     * traversable.
     * @return True if traversable.
     */
    public static boolean isEnemyHQBuffedRangeTurnedOff() {
        return decodeInEnemyHQBuffedRange(Common.mobLevel);
    }
    
    /**
     * Checks the mob level if the splash region of the enemy HQ has been made  
     * traversable.
     * @return True if traversable.
     */
    public static boolean isEnemyHQSplashRegionTurnedOff() {
        return decodeInEnemyHQSplashRegion(Common.mobLevel);
    }
    
    /**
     * Checks the mob level if the attack range of the specified enemy tower has been 
     * made traversable.
     * @param towerIndex The index of the tower.
     * @return True if traversable.
     */
    public static boolean isEnemyTowerRangeTurnedOff(int towerIndex) {
        return decodeInEnemyTowerRange(Common.mobLevel,towerIndex);
    }
    
    
    //Other methods ===========================================================
    
    /**
     * Checks pathability of a location using in order: internal map, then radio map, 
     * then direct sensing. Updates accordingly. Need to be called with isPathable() 
     * or canMove() since it only queries map values.
     * @param loc Location to query
     * @return True if map values alone do not indicate movement restrictions
     */
    public static boolean checkPathable(MapLocation loc) throws GameActionException {
        //Get maximum pathability state ordinal
        int maxPathableOrdinal = 1; //NORMAL only
        if(RobotPlayer.myType == RobotType.DRONE) maxPathableOrdinal = 2; //VOID too
        //Check internal map pathability first
        int value = getInternalMap_(loc.x,loc.y);
        int pathStateOrdinal = decodePathStateOrdinal(value);
        if(pathStateOrdinal > maxPathableOrdinal) {
            //Internal PathState is (VOID,) ENEMY_HQ, HQ, OFF_MAP
            return false;
        }
        if(pathStateOrdinal == 0) {
            //Internal PathState is UNKNOWN
            //Check radio map pathability next
            int valueRadio = getRadioMap(loc.x,loc.y);
            int pathStateOrdinalRadio = decodePathStateOrdinal(valueRadio);
            if(pathStateOrdinalRadio != 0) {
                //Update internal map
                //radio is more up to date so just copy into internal map as well
                setInternalMap(loc.x,loc.y,valueRadio);
                if(pathStateOrdinalRadio > maxPathableOrdinal) {
                    //Radio PathState is (VOID,) ENEMY_HQ, HQ, OFF_MAP
                    return false;
                }
            } else {
                //Radio PathState is UNKNOWN
                //Sense directly for terrain tile
                switch (RobotPlayer.rc.senseTerrainTile(loc)) {
                    case VOID:
                        pathStateOrdinal = PathState.VOID.ordinal(); break;
                    case NORMAL:
                        pathStateOrdinal = PathState.NORMAL.ordinal(); break;
                    case OFF_MAP:
                        pathStateOrdinal = PathState.OFF_MAP.ordinal(); break;
                    case UNKNOWN:
                        pathStateOrdinal = PathState.UNKNOWN.ordinal(); break;
                    default: break;
                }
                valueRadio = Map.encodePathState(valueRadio,pathStateOrdinal);
                //radio is more up to date so just copy into internal map as well
                setMaps(loc.x,loc.y,valueRadio);
                //Check PathState again
                if(pathStateOrdinal > maxPathableOrdinal) return false;
            }
        }
        
        //At this point internal map should already be consistent with radio map
        //as long as all terrain tile checks also update the radio map.
        //Now check whether in attack regions of enemy HQ or towers
        
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
        }
        
        return true;
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
        System.out.println(""+Clock.getBytecodesLeft());
        
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
            System.out.println(""+Clock.getBytecodesLeft());
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