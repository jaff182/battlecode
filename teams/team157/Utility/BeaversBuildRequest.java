package team157.Utility;

import battlecode.common.GameActionException;
import battlecode.common.RobotType;
import team157.Channels;
import team157.RobotPlayer;

public class BeaversBuildRequest {
    /**
     * Allows you to tell beavers to build things.
     * 
     * Causes a random beaver (not the first to run) to build something.
     * 
     * Returns in a single turn (whether its successful). If a beaver dies, this function may fail.
     */
    
    /**
     * Channel allocation is as such:
     * 
     * BASE_CHANNEL  : building ordinal type, or MAX_INT if no building is to be built.
     * BASE_CHANNEL+1: selected beaver number to build the building we want
     * BASE_CHANNEL+2: number of this beaver, goes from 15(beaver number) to 1 as code runs
     * 
     */
    public static int BASE_CHANNEL = Channels.BEAVER_BUILD_REQUEST;
    
    /**
     * HQ must run this at start of game once, in it's init code.
     * 
     * @throws GameActionException
     */
    public static void HQinit() throws GameActionException {
        RobotPlayer.rc.broadcast(BASE_CHANNEL, Integer.MAX_VALUE);
    }
    
    /**
     * Every beaver should call this to check if they have to build a building.
     * 
     * @return the building type if they have to, null otherwise.
     * @throws GameActionException
     */
    public static RobotType doIHaveToBuildABuilding() throws GameActionException {
        int robotType = RobotPlayer.rc.readBroadcast(BASE_CHANNEL);
        final int thisBeaverNumber = RobotPlayer.rc.readBroadcast(BASE_CHANNEL+1)-1;
        RobotPlayer.rc.broadcast(BASE_CHANNEL+1, thisBeaverNumber);
            if (robotType != Integer.MAX_VALUE) {// Maybe..
                final int beaverNumberSelected = RobotPlayer.rc.readBroadcast(BASE_CHANNEL+2);
//                System.out.println("Look! A request for beaver " + beaverNumberSelected +", and i'm " + thisBeaverNumber);

                if (beaverNumberSelected == thisBeaverNumber) {
                    return RobotPlayer.robotTypes[robotType]; // Yup I do
                }
            }
        return null; // Nope
    }
    
    /**
     * Respond to doIHaveToBuildABuilding().
     * @throws GameActionException
     */
    public static void yesIWillBuildABuilding() throws GameActionException {
        RobotPlayer.rc.broadcast(BASE_CHANNEL, Integer.MAX_VALUE); // Turn off building checks
    }
    
    /**
     * Tells a beaver to build a building. Call this once per turn.
     * 
     * @param buildingType
     *            the building we want to build
     * @param totalNumberOfBeavers
     *            the total number of beavers present
     * @param numberOfBeaverToBuildBuilding
     *            the beaver we want to build a building. must be a number
     *            between 0 to totalBeavers exclusive.
     * @throws GameActionException
     */
    public static void pleaseBuildABuilding(RobotType buildingType,
            int totalNumberOfBeavers, int numberOfBeaverToBuildBuilding)
            throws GameActionException {
//        System.out.println("Look! A request for beaver " + numberOfBeaverToBuildBuilding);
        RobotPlayer.rc.broadcast(BASE_CHANNEL, buildingType.ordinal());
        RobotPlayer.rc.broadcast(BASE_CHANNEL + 1, totalNumberOfBeavers);
        RobotPlayer.rc.broadcast(BASE_CHANNEL + 2,
                numberOfBeaverToBuildBuilding);

    }
    
    /**
     * Tell you whether your message has been received by the target beaver.
     * 
     * Does not guarantee the beaver will finish building the building!
     * 
     * Call the immediate turn after you call pleaseBuildABuilding(). Calls
     * after/before will return invalid results.
     * 
     * You must not call pleaseBuildABuilding in the same turn BEFORE this.
     * 
     * @return true if build request received, false otherwise
     * @throws GameActionException 
     */
    public static boolean wasMyBuildMessageReceived() throws GameActionException {
        return RobotPlayer.rc.readBroadcast(BASE_CHANNEL) == Integer.MAX_VALUE;
    }
}
