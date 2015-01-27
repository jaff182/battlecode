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
     * BuildOrder.MAX_LENGTH+1 = 200 channels needed (16901 to 17100)
     */
    public static final int BUILD_ORDER_BASE = 16901;
    
    /**
     * Starting channel for HQ's declaration of units required. Structures try to 
     * satisfy the numbers stated here.
     * 19 channels needed (17101 to 17119)
     */
     public static final int UNIT_ORDER_BASE = 17101;
    
    /**
     * Channel storing number of units produced since start of game by you (including
     * towers, HQ)
     */
    public static final int SEQ_UNIT_NUMBER = 17121;
    
    /**
     * Starting channel storing number of robots of each type that exist now
     * 48 channels needed (17122 to 17169)
     */
    public static final int UNIT_COUNT_BASE = 17122;
    
    
    public static final int LAST_ATTACKED_COORDINATES = 17170;
    
    /**
     * Starting channel for effective miner system (counting and announcing proportion)
     * 3 channels needed (17194 to 17196)
     */
    public static final int MINER_EFFECTIVENESS = 17194;

    /**
     * Starting channel for missile target
     * 10 channels needed (17220 to 17229)
     */
    public static final int MISSILE_TARGET = 17220;
    
    /**
     * Channel for global mob level signal. Encodes which enemy HQ/tower attack 
     * regions may be traversed.
     */
    public static final int MOB_LEVEL = 17221;
    
    /**
     * Starting channel for storing the coordinates of initially existing enemy towers.
     * 12 channels needed (17230 to 17241);
     */
    public static final int ENEMY_TOWER_LOCATIONS = 17230;
    
    /**
     * Starting channel for supply request and supply drone system.
     * 4 channels needed (17250 to 17253)
     */
    public static final int SUPPLY_REQUESTS = 17250;
}
