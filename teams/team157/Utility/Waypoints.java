package team157.Utility;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import team157.Utility.*;
import team157.Common;

public class Waypoints {
    /**
     * Waypoints that follow the order specified:
     * 
     * 
     *  1.the last 3 locations our units have been attacked in  
     *  2.A points that should be the best ore concentration (one of two)
     *  3.enemy hq
     *  
     *  Waypoints are stored in the radio map for retrieval by everyone.
     *  
     *  Always call refreshLocalCache() once per turn before accessing waypoints[]
     */
    
    public final static int MAX_NUMBER_OF_WAYPOINTS = 10;
    
    public final static int LAST_ATTACKED_LOCATIONS_USED = 3;

    
    public static MapLocation waypoints[] = new MapLocation[MAX_NUMBER_OF_WAYPOINTS];
    public static int numberOfWaypoints = 0;
    
    public static MapLocation bestEnemyOreLocation = null;
    
    /**
     * Radio map storage pattern: <br>
     * <br>
     * BASE_CHANNEL             ---------   <br>
     * . <br>
     * . <br>
     * MAX_NUMBER_OF_GROUPS     ---------   <br>
     * 
     */
    final static int BASE_CHANNEL = 40000;

    static int numberOfEventsThatHaveOccurredWhenLastUpdated = -1;
    
    /**
     * CALL BEFORE ACCESSING waypoints EACH TURN!!!
     * 
     * @throws GameActionException
     */
    public static void refreshLocalCache() throws GameActionException {
        int numberOfEventsThatHaveOccurred = LastAttackedLocationsReport.getNumberOfEventsThatHaveOccurred();

        if (numberOfEventsThatHaveOccurred != numberOfEventsThatHaveOccurredWhenLastUpdated) {
            // Dirty!
            numberOfEventsThatHaveOccurredWhenLastUpdated = numberOfEventsThatHaveOccurred;

            int i = 0;

            // 1 in 5 drones go to HQ
            // TODO: I assume that the var i is accessed statically by all robots, because else it does not make sense
            // as i = 0 all the time.
            if (Common.rand.nextInt(5) == 0) {
                waypoints[i] = Common.HQLocation;
                ++i;
            }

            // TODO: should we wait until all robots have finished this step? do we have to worry about race condition?

            // build new waypoints array
            for (; i < LAST_ATTACKED_LOCATIONS_USED; ++i) {
                int x = LastAttackedLocationsReport.getLastAttackXCoordinate(i);
                if (x != Integer.MAX_VALUE)
                    waypoints[i] = new MapLocation(x,
                            LastAttackedLocationsReport
                                    .getLastAttackYCoordinate(i));
                else
                    break;
            }
            if (bestEnemyOreLocation != null) {
                waypoints[i] = bestEnemyOreLocation;
                ++i;
            }
            waypoints[i] = Common.enemyHQLocation;
            numberOfWaypoints = i+1;
        }
    }
    
    public static void print() {
        System.out.println("Waypoints (" + numberOfWaypoints + " of them):");
        for (int i=0; i<numberOfWaypoints; i++) {
            System.out.println(waypoints[i]);
        }
    }
    
}
