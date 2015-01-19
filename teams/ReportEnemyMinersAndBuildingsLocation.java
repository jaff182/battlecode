import battlecode.common.RobotInfo;


public class ReportEnemyMinersAndBuildingsLocation {
/**
 * Used to report unguarded miners, beavers and buildings.
 * 
 * 4 statistics are stored in the radio:<br>
 * 1) Time the last report is made<br>
 * 2) The x-location of the center of mass of units seen<br>
 * 3) The y-location of the center of mass of units seen<br>
 * 4) The total cost of the enemy units seen<br>
 * 
 */
    
    // Standard channel allocation ===================================
    public static final int BASE_CHANNEL = 30000;
    public static final int ROUND_NUMBER_REPORT_MADE_CHANNEL = BASE_CHANNEL;
    public static final int LOCATION_REPORTED_X_CHANNEL = BASE_CHANNEL+1;
    public static final int LOCATION_REPORTED_Y_CHANNEL = BASE_CHANNEL+2;
    public static final int COST_CHANNEL = BASE_CHANNEL+3;

    // Parameters (tweak this) ========================================
    
    /**
     * Number of rounds before the previous record can be replaced with a better
     * one (regardless of the cost parameter)
     */
    public static final int REPORT_EXPIRY_ROUNDS = 5;
    
    /**
     * Checks whether the given enemies satisfies the criterion for a report,
     * and then goes ahead and reports it if so.
     * 
     * The reporting criterion are namely,<br>
     * 1) No enemies other than buildings (including HQ, tower), miners, and
     * beavers seen<br>
     * 2) The cost of the units seen are higher than that previously seen (or
     * the previous report has expired)<br>
     * 
     * @param enemies
     */
    public static void checkAndReportAreaInSight(RobotInfo[] enemies) {
        return;
    }

}
