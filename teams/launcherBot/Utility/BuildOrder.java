package launcherBot.Utility;

import launcherBot.*;
import battlecode.common.*;

/**
 * Allows HQ to declare desired structures and their relative priorities. Beavers use 
 * this to check if they need to build units. NOTE: Not dependency-aware, HQ must add 
 * buildings in appropriate order.
 * 
 * Build Order protocol:
 * 1) HQ adds an entry to the build order list using ArrayList-like methods.
 * 2) Beaver queries build order for a job with doIHaveToBuildABuilding, finds the 
 *    first unsatisfied entry.
 * 3) Beaver claims job with IAmTheBuilding(), starts building, then continually
 *    checks validity of build order entry and claims with IAmBuildingTheBuilding() 
 *    on behalf of the building.
 * 4) Building checks on spawn to see if it is on the build order list with 
 *    AmIOnBuildOrder(), then continues claiming with IAmTheBuilding().
 */
public class BuildOrder {
    
    /**
     * Channel allocation is as such:
     * BASE_CHANNEL: Total number of current requests
     * BASE_CHANNEL+n: The nth structure to build
     * 
     */
    private static final int BASE_CHANNEL = Channels.BUILD_ORDER_BASE;
    
    /**
     * Maximum length of build order
     */
    private static final int MAX_LENGTH = 99;
    
    /**
     * The number of rounds after which a timestamp expires and the building is 
     * considered destroyed and needs to be rebuilt.
     */
    private static final int TIMESTAMP_EXPIRY = 2;
    
    
    //Conversion methods ======================================================
    
    /**
     * Converts build order information to integer
     * @param buildingTypeOrdinal The ordinal of the building type to be built.
     * @param id The ID of the unit, or the beaver claiming the job. 0 means unclaimed.
     * @return Value to be stored in radio.
     */
    public static int encode(int buildingTypeOrdinal, int id) {
        return buildingTypeOrdinal + 21*(Clock.getRoundNum() + 2001*id);
    }
    
    /**
     * Decodes the building type to be built.
     * @param value The value stored on the radio
     * @return The ordinal of the building type to be built.
     */
    public static int decodeTypeOrdinal(int value) {
        return value%21;
    }
    
    /**
     * Decodes the round number that the value was updated.
     * @param value The value stored on the radio
     * @return The round number that the value was updated.
     */
    public static int decodeTimeStamp(int value) {
        return (value/21)%2001;
    }
    
    /**
     * Decodes the ID of the robot built.
     * @param value The value stored on the radio
     * @return The ID of the robot.
     */
    public static int decodeID(int value) {
        return value/42021;
    }
    
    
    //Build order methods =====================================================
    //ArrayList-like methods to access or modify the build order
    
    /**
     * Add building of a certain type to the end of the build order, up to a length of 
     * MAX_LENGTH.
     * @param buildingType Building robot type to add.
     */
    public static void add(RobotType buildingType) throws GameActionException {
        int length = Common.rc.readBroadcast(BASE_CHANNEL);
        if(length < MAX_LENGTH) {
            int channel = BASE_CHANNEL+1+length;
            int value = encode(buildingType.ordinal(),0);
            Common.rc.broadcast(channel,value); //upload value
            Common.rc.broadcast(BASE_CHANNEL,length+1); //increment length
        } else {
            //Complain
            System.out.println("Warning: Maximum build order length reached.");
        }
    }
    
    /**
     * Insert building of a certain type to a specified position in the build order
     * @param index Position to insert entry
     * @param buildingType Building robot type to insert
     */
    public static void add(int index, RobotType buildingType) throws GameActionException {
        int length = Common.rc.readBroadcast(BASE_CHANNEL);
        if(length < MAX_LENGTH) {
            if(index < length) {
                //Shift down the entries past the index
                for(int idx=length-1; idx>=index; idx--) {
                    int channelFrom = BASE_CHANNEL+1+idx;
                    int value = Common.rc.readBroadcast(channelFrom);
                    Common.rc.broadcast(channelFrom+1,value);
                }
            }
            if(index <= length) {
                int channel = BASE_CHANNEL+1+length;
                int value = encode(buildingType.ordinal(),0);
                Common.rc.broadcast(channel,value); //upload value
                Common.rc.broadcast(BASE_CHANNEL,length+1); //increment length
            }  else {
                //Complain
                System.out.println("Warning: Cannot insert build order entry beyond bounds.");
            }
        } else {
            //Complain
            System.out.println("Warning: Maximum build order length reached.");
        }
    }
    
    /**
     * Add building types from an array to the end of the build order, up to a length 
     * of MAX_LENGTH.
     * @param buildingTypes Array of building robot types to add.
     */
    public static void addAll(RobotType[] buildingTypes) throws GameActionException {
        int length = Common.rc.readBroadcast(BASE_CHANNEL);
        int idx = 0;
        while(idx < buildingTypes.length && length + idx < MAX_LENGTH) {
            int channel = BASE_CHANNEL+1+length+idx;
            int value = encode(buildingTypes[idx].ordinal(),0);
            Common.rc.broadcast(channel,value); //upload value
            idx++;
        }
        Common.rc.broadcast(BASE_CHANNEL,length+idx); //increase length
        if(length+idx > MAX_LENGTH) {
            //Complain
            System.out.println("Warning: Maximum build order length reached.");
        }
    }
    
    /**
     * Clear the build order
     */
    public static void clear() throws GameActionException {
        Common.rc.broadcast(BASE_CHANNEL,0);
    }
    
    /**
     * Gets the build order entry at a specified position.
     * @return The value at the specified position.
     */
    public static int get(int index) throws GameActionException {
        int channel = BASE_CHANNEL+1+index;
        return Common.rc.readBroadcast(channel);
    }
    
    /**
     * Remove entry at a specified position in the build order.
     * @param index Position to remove entry.
     */
    public static void remove(int index) throws GameActionException {
        int length = Common.rc.readBroadcast(BASE_CHANNEL);
        if(index >= 0) {
            if(index < length-1) {
                //Shift up the entries past the index
                for(int idx=index+1; idx<length; idx++) {
                    int channelFrom = BASE_CHANNEL+1+idx;
                    int value = Common.rc.readBroadcast(channelFrom);
                    Common.rc.broadcast(channelFrom-1,value);
                }
            }
            if(index <= length-1) {
                //Decrement length
                Common.rc.broadcast(BASE_CHANNEL,length-1);
            } else {
                //Complain
                System.out.println("Warning: Removing build order entry outside bounds.");
            }
        } else {
            //Complain
            System.out.println("Warning: Removing build order entry outside bounds.");
        }
    }
    
    /**
     * Find the first occurrence of a building of a certain type.
     * @param 
     */
    public static void remove(RobotType buildingType) throws GameActionException {
        int length = Common.rc.readBroadcast(BASE_CHANNEL);
        int typeOrdinal = buildingType.ordinal();
        //Search for building type
        int index = -1;
        for(int idx=0; idx<length; idx++) {
            int channel = BASE_CHANNEL+1+idx;
            int value = Common.rc.readBroadcast(channel);
            if(decodeTypeOrdinal(value) == typeOrdinal) {
                index = idx;
                break;
            }
        }
        if(index >= 0) {
            if(index < length-1) {
                //Shift up the entries past the index
                for(int idx=index+1; idx<length; idx++) {
                    int channelFrom = BASE_CHANNEL+1+idx;
                    int value = Common.rc.readBroadcast(channelFrom);
                    Common.rc.broadcast(channelFrom-1,value);
                }
            }
            if(index <= length-1) {
                //Decrement length
                Common.rc.broadcast(BASE_CHANNEL,length-1);
            }
        } else {
            //Complain
            System.out.println("Warning: No entry found with "+buildingType);
        }
    }
    
    /**
     * Find the first occurrence of a building of a certain type.
     * @param buildingType The building type to remove.
     * @return The index of the first occurrence of buildingType. (-1 if not found)
     */
    public static int search(RobotType buildingType) throws GameActionException {
        int length = Common.rc.readBroadcast(BASE_CHANNEL);
        int typeOrdinal = buildingType.ordinal();
        //Search for building type
        int index = -1;
        for(int idx=0; idx<length; idx++) {
            int channel = BASE_CHANNEL+1+idx;
            int value = Common.rc.readBroadcast(channel);
            if(decodeTypeOrdinal(value) == typeOrdinal) {
                return idx;
            }
        }
        //Not found
        return -1;
    }
    
    /**
     * Replaces the build order entry at a specified position with a new entry.
     * @param index Position to replace entry.
     * @param buildingType Building type to add.
     * @param id Building robot ID to add.
     */
    public static void set(int index, RobotType buildingType, int id) throws GameActionException {
        int channel = BASE_CHANNEL+1+index;
        int value = encode(buildingType.ordinal(),id);
        Common.rc.broadcast(channel,value);
    }
    
    /**
     * Replaces the build order entry at a specified position with a new entry.
     * @param index Position to replace entry.
     * @param buildingTypeOrdinal Ordinal of building type to add.
     * @param id Building robot ID to add.
     */
    public static void set(int index, int buildingTypeOrdinal, int id) throws GameActionException {
        int channel = BASE_CHANNEL+1+index;
        int value = encode(buildingTypeOrdinal,id);
        Common.rc.broadcast(channel,value);
    }
    
    /**
     * Returns the length of the build order
     * @return Length of build order.
     */
    public static int size() throws GameActionException {
        return Common.rc.readBroadcast(BASE_CHANNEL);
    }
    
    
    
    
    //Job claiming methods ====================================================
    
    /**
     * Every beaver should call this to check if they have to build a building. 
     * Iterates through the build order to check that the IDs are not 0 (job has been 
     * claimed) and the time stamps are up to date, otherwise the building might have 
     * died or not have been built.
     * @return The index of the build order they have to satisfy, -1 otherwise.
     * @throws GameActionException
     */
    public static int doIHaveToBuildABuilding() throws GameActionException {
        int length = Common.rc.readBroadcast(BASE_CHANNEL);
        for(int idx=0; idx<length; idx++) {
            int value = get(idx);
            //Check claimed job and unexpired timestamp
            if(decodeID(value) == 0 
                || decodeTimeStamp(value) < Clock.getRoundNum()-TIMESTAMP_EXPIRY) {
                    //Need to build this building
                    return idx;
            }
        }
        //Nothing to build
        return -1;
    }
    
    /**
     * Buildings' reporting of their existence in response to 
     * doIHaveToBuildABuilding(). Also beaver's initial response before the building 
     * ID is known.
     * @param index Position in the build order to respond to.
     * @throws GameActionException
     */
    public static void IAmTheBuilding(int index) throws GameActionException {
        int value = get(index);
        int buildingTypeOrdinal = decodeTypeOrdinal(value);
        set(index,buildingTypeOrdinal,Common.rc.getID());
    }
    
    /**
     * Beaver's response to doIHaveToBuildABuilding() with their building's ID.
     * @param index Position in the build order to respond to.
     * @param id Building's ID.
     * @throws GameActionException
     */
    public static void IAmBuildingTheBuilding(int index, int id) throws GameActionException {
        int value = get(index);
        int buildingTypeOrdinal = decodeTypeOrdinal(value);
        set(index,buildingTypeOrdinal,id);
    }
    
    /**
     * Buildings need to find out which build order they are supposed to satisfy.
     * @param id0 Robot's or building's ID
     * @return Position in build order building is meant to satisfy. -1 means none.
     */
    public static int AmIOnBuildOrder(int id0) throws GameActionException {
        //Search for index with matching ID
        int length = Common.rc.readBroadcast(BASE_CHANNEL);
        for(int idx=0; idx<length; idx++) {
            int value = get(idx);
            int id = decodeID(value);
            if(id == id0) return idx;
        }
        //Otherwise return none
        return -1;
    }
    
    
    //Debug methods ===========================================================
    
    /**
     * Prints the build order.
     */
    public static void print() throws GameActionException {
        int length = Common.rc.readBroadcast(BASE_CHANNEL);
        for(int idx=0; idx<length; idx++) {
            int value = get(idx);
            RobotType buildingType = RobotType.values()[decodeTypeOrdinal(value)];
            int id = decodeID(value);
            int timestamp = decodeTimeStamp(value);
            System.out.println(idx+": "+buildingType+" claimed by robot "+id+" at round "+timestamp+".");
        }
    }
}
