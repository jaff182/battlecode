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
     * Starting channel for effective miner system (counting and announcing proportion)
     * 3 channels needed (17094 to 17096)
     */
    public static final int MINER_EFFECTIVENESS = 17094;
    
    /**
     * Starting channel for tank defense unit count system
     * 19 channels needed (17097 to 17115)
     */
    public static final int TANK_DEFENSE_COUNT = 17097;

    /**
     * Starting channel for missile target
     * 10 channels needed (17120 to 17129)
     */
    public static final int MISSILE_TARGET = 17120;
    
    /**
     * Channel for global mob level signal. Encodes which enemy HQ/tower attack 
     * regions may be traversed.
     */
    public static final int MOB_LEVEL = 17121;
    
    /**
     * Starting channel for storing the coordinates of initially existing enemy towers.
     * 12 channels needed (17130 to 17141);
     */
    public static final int ENEMY_TOWER_LOCATIONS = 17130;
    
    public static final int DOES_SUPPLY_DRONE_EXIST = 17145;
}
