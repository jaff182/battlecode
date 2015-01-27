package team157.Utility;

import battlecode.common.*;
import team157.Common;
import team157.RobotPlayer;
import team157.Channels;

public class Supply {
    
    //Global variables ========================================================
    
    /**
     * The minimum number of rounds worth of supply to keep for oneself when deciding 
     * to distributing supply.
     */
    private static final int MIN_ROUNDS_TO_KEEP = 10;
    
    
    //Supply transferring methods =============================================
    
    /**
     * Distribute supply among neighboring units, according to health/supplyUpkeep. 
     * Primarily used by a temporary holder of supply, eg beaver/soldier.
     * @param multiplier Double array of multipliers to supply capacity per unit health of each RobotType.
     * @throws GameActionException
     */
    public static void distribute(double[] multiplier) throws GameActionException {
        if(Clock.getBytecodesLeft() > 1000 
            && RobotPlayer.rc.getSupplyLevel() > MIN_ROUNDS_TO_KEEP*Common.myType.supplyUpkeep) {
                //Sense nearby friendly robots
                RobotInfo[] friends = RobotPlayer.rc.senseNearbyRobots(GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED,Common.myTeam);
                if(friends.length > 0) {
                    //Initiate
                    int targetidx = -1;
                    double myCapacity = RobotPlayer.rc.getHealth()*Common.myType.supplyUpkeep*multiplier[RobotPlayer.rc.getType().ordinal()];
                    double totalSupply = RobotPlayer.rc.getSupplyLevel();
                    double totalCapacity = myCapacity;
                    double minSupplyRatio = Integer.MAX_VALUE; //some big number
                    
                    //Iterate through friends for as long as bytecodes allow
                    for(int i=0; i<friends.length; i++) {
                        //Keep track of total values to find mean later
                        totalSupply += friends[i].supplyLevel;
                        double friendCapacity = friends[i].health*friends[i].type.supplyUpkeep*multiplier[friends[i].type.ordinal()];
                        totalCapacity += friendCapacity;
                        
                        //Find robot with lowest supply per capacity and positive capacity
                        if(friendCapacity > 0) {
                            double supplyRatio = friends[i].supplyLevel/friendCapacity;
                            if(supplyRatio < minSupplyRatio) {
                                minSupplyRatio = supplyRatio;
                                targetidx = i;
                            }
                        }
                        
                        //Stop checking if insufficient bytecode left
                        if(Clock.getBytecodesLeft() < 600) break;
                    }
                    
                    //Transfer any excess supply
                    double myMeanSupply = Math.max(totalSupply/totalCapacity*myCapacity,MIN_ROUNDS_TO_KEEP*Common.myType.supplyUpkeep);
                    if(targetidx != -1 && RobotPlayer.rc.getSupplyLevel() > myMeanSupply) {
                        MapLocation loc = friends[targetidx].location;
                        //Transfer all supply above my mean level amount
                        double transferAmount = (RobotPlayer.rc.getSupplyLevel()-myMeanSupply);
                        RobotPlayer.rc.transferSupplies((int)transferAmount,loc);
                    }
                }
        }
    }
    
    /**
     * Dispense supply to neighboring units according to health/supplyUpkeep. 
     * Primarily used by a large source/storage of supply, eq HQ/building.
     * @param multiplier Double array of multipliers to supply capacity per unit health of each RobotType.
     * @throws GameActionException
     */
    public static void dispense(double[] multiplier) throws GameActionException {
        if(Clock.getBytecodesLeft() > 800
            && RobotPlayer.rc.getSupplyLevel() > MIN_ROUNDS_TO_KEEP*Common.myType.supplyUpkeep) {
                //Sense nearby friendly robots
                RobotInfo[] friends = RobotPlayer.rc.senseNearbyRobots(GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED,Common.myTeam);
                int targetidx = -1;
                double totalSupply = 0, totalCapacity = 0;
                double targetSupplyRatio = 1000, minSupplyRatio = targetSupplyRatio;
                //targetsupplyRatio arbitrarily set to 1000 for now
                
                for(int i=0; i<friends.length; i++) {
                    //Keep track of total values to find mean later
                    totalSupply += friends[i].supplyLevel;
                    double friendCapacity = friends[i].health*multiplier[friends[i].type.ordinal()];
                    totalCapacity += friendCapacity;
                    
                    //Find robot with lowest supply per capacity
                    if(friendCapacity > 0) {
                        double supplyRatio = friends[i].supplyLevel/friendCapacity;
                        if(supplyRatio < minSupplyRatio) {
                            minSupplyRatio = supplyRatio;
                            targetidx = i;
                        }
                    }
                    
                    //Stop checking if insufficient bytecode left
                    if(Clock.getBytecodesLeft() < 600) break;
                }
                
                //Replenish supply of target
                double targetTotalSupply = totalCapacity*targetSupplyRatio;
                if(targetidx != -1 && totalSupply < targetTotalSupply) {
                    MapLocation loc = friends[targetidx].location;
                    RobotPlayer.rc.transferSupplies((int)(targetTotalSupply-totalSupply),loc);
                }
        }
    }
    
    /**
     * Dump most of supply to neighboring units according to health/supplyUpkeep. 
     * Primarily used by a mobile supplier with a large amount of supply, eq drone.
     * Relies on other units' distribution to spread the supply.
     * @param loc Location to dump supply to
     * @param returnTripTime Number of rounds it takes to get back to HQ
     * @throws GameActionException
     */
    public static void dump(MapLocation loc,int returnTripTime) throws GameActionException {
        if(Clock.getBytecodesLeft() > 600) {
            int transferAmount = (int)(RobotPlayer.rc.getSupplyLevel() - Common.myType.supplyUpkeep*returnTripTime);
            if(transferAmount > 0) {
                RobotPlayer.rc.transferSupplies(transferAmount,loc);
            }
        }
    }
    
    
    //Requesting methods ======================================================
    
    /**
     * Channels to store supply drone timestamps. Add more channels here for more 
     * supply channels. Must be the same length as CHANNELS.
     */
    public static final int[] DRONE_CHANNELS = {Channels.SUPPLY_REQUESTS,Channels.SUPPLY_REQUESTS+2};
    
    /**
     * Channels to store requests. Add more channels here for more supply channels. 
     * Must be the same length as DRONE_CHANNELS.
     */
    public static final int[] CHANNELS = {Channels.SUPPLY_REQUESTS+1,Channels.SUPPLY_REQUESTS+3};
    
    
    /**
     * Checks if a supply drone exists for all supply system channels.
     * @return The index of the first unfulfilled supply drone task. -1 if all supply 
     *         channels have a supply drone.
     */
    public static int droneExists() throws GameActionException {
        for(int i=0; i<DRONE_CHANNELS.length; i++) {
            if(RobotPlayer.rc.readBroadcast(DRONE_CHANNELS[i]) < Clock.getRoundNum()-10) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Upload a supply request onto the radio. All units to be supplied call this 
     * if they are the more important to be supplied than the last requester.
     * @param index The channel index to send to.
     */
    public static void request(int index) throws GameActionException {
        int id = RobotPlayer.rc.getID();
        int supplyLevel = Math.min((int)(RobotPlayer.rc.getSupplyLevel()),999);
        int priority = supplyPriorities[Common.myType.ordinal()];
        RobotPlayer.rc.broadcast(CHANNELS[index],id*32000+supplyLevel*32+priority);
    }
    
    /**
     * Decodes the ID of the request
     * @param value The value stored on the radio.
     * @return The ID of the robot making the request.
     */
    public static int decodeRequestID(int value) {
        return value/32000;
    }
    
    /**
     * Decodes the supply level of the request
     * @param value The value stored on the radio.
     * @return The supply level of the robot making the request.
     */
    public static int decodeRequestLevel(int value) {
        return (value/32)%1000;
    }
    
    /**
     * Decodes the priority rating of the request
     * @param value The value stored on the radio.
     * @return The ID of the robot making the request.
     */
    public static int decodeRequestPriority(int value) {
        return value%32;
    }
    
    /**
     * Gets the information stored in the supply requests system.
     * @param index The channel index to obtain information from.
     */
    public static int getRequestInfo(int index) throws GameActionException {
        return RobotPlayer.rc.readBroadcast(CHANNELS[index]);
    }
    
    /**
     * Resets the supply request system for the specified channel.
     * @param index The channel index to reset
     */
    public static void resetRequestInfo(int index) throws GameActionException {
        RobotPlayer.rc.broadcast(CHANNELS[index],0);
    }
    
    /**
     * Resets the supply request system.
     */
    public static void resetRequestInfo() throws GameActionException {
        for(int i=0; i<CHANNELS.length; i++) {
            RobotPlayer.rc.broadcast(CHANNELS[i],0);
        }
    }
    
    /**
     * Values to indicate which robot type gets supplied first. Higher means more     
     * important to supply. Values should not exceed 31.
     */
    private static final int[] supplyPriorities = {
        0/*0:HQ*/,          0/*1:TOWER*/,       0/*2:SUPPLYDPT*/,   0/*3:TECHINST*/,
        0/*4:BARRACKS*/,    0/*5:HELIPAD*/,     0/*6:TRNGFIELD*/,   0/*7:TANKFCTRY*/,
        0/*8:MINERFCTRY*/,  0/*9:HNDWSHSTN*/,   0/*10:AEROLAB*/,    0/*11:BEAVER*/,
        0/*12:COMPUTER*/,   18/*13:SOLDIER*/,   18/*14:BASHER*/,    16/*15:MINER*/,
        0/*16:DRONE*/,      19/*17:TANK*/,      20/*18:COMMANDER*/, 21/*19:LAUNCHER*/,
        0/*20:MISSILE*/
    };
    
    /**
     * All units call this to send a supply request every few rounds when supply is 
     * low.
     */
    public static void callForSupply() throws GameActionException {
        if(Clock.getRoundNum()%5 == 0) {
            int myPriority = supplyPriorities[Common.myType.ordinal()];
            if(myPriority > 0) {
                double mySupply = RobotPlayer.rc.getSupplyLevel();
                if(mySupply < MIN_ROUNDS_TO_KEEP*Common.myType.supplyUpkeep) {
                    for(int i=0; i<CHANNELS.length; i++) {
                        //Decode request information
                        int value = getRequestInfo(i);
                        int maxPriority = decodeRequestPriority(value);
                        int minSupplyLevel = decodeRequestLevel(value);
                        if((myPriority > maxPriority) 
                            || (myPriority == maxPriority && mySupply < minSupplyLevel)) {
                                //Have higher priority than previous requester
                                //Shift later channels down
                                for(int j=CHANNELS.length-2; j>=i; j--) {
                                    value = getRequestInfo(j);
                                    RobotPlayer.rc.broadcast(CHANNELS[j+1],value);
                                }
                                request(i);
                                return;
                        }
                    }
                }
            }
        }
    }
    
    
    
}