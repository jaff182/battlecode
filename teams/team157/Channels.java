package team157;

public class Channels {
    
    /**
     * Channel for map symmetry information
     */
    public static final int MAP_SYMMETRY = 0;
    
    /**
     * Starting channel for internal map
     * Map.ALLOCATED_WIDTH*Map.ALLOCATED_HEIGHT = 130^2 = 16900 channels needed
     * (1 to 16900)
     */
    public static final int MAP_DATA = 1;
    
    /**
     * Starting channel for HQ's declaration of structures required. Beavers try to 
     * satisfy the build order stated here.
     * BuildOrder.MAX_LENGTH+1 = 100 channels needed (16901 to 17000)
     */
    public static final int BUILD_ORDER_BASE = 16901;
    
    /**
     * Starting channel for HQ's declaration of units required. Structures try to 
     * satisfy the numbers stated here.
     * 19 channels needed (17001 to 17019)
     */
     public static final int UNIT_ORDER_BASE = 17001;
    
    /**
     * Channel storing number of units produced since start of game by you (including
     * towers, HQ)
     */
    public static final int SEQ_UNIT_NUMBER = 17021;
    
    /**
     * Starting channel storing number of robots of each type that exist now
     * 48 channels needed (17022 to 17069)
     */
    public static final int UNIT_COUNT_BASE = 17022;
    
    
    public static final int LAST_ATTACKED_COORDINATES = 17070;
    
    /**
     * Starting channel for beaver build request system
     * 3 channels needed (17091 to 17093)
     */
    public static final int BEAVER_BUILD_REQUEST = 17091;
    
    /**
     * Starting channel for effective miner count system
     * 2 channels needed (17095 to 17096)
     */
    public static final int EFFECTIVE_MINERS_COUNT = 17095;
    
    /**
     * Starting channel for tank defense unit count system
     * 19 channels needed
     */
    public static final int TANK_DEFENSE_COUNT = 17097;

    /**
     * Starting channel for missile target
     * 10 channels needed
     */
    public static final int MISSILE_TARGET = 17120;
}
