package team157;

public class Channels {
    
    /**
     * Channel for map symmetry information
     */
    public static final int MAP_SYMMETRY = 0;
    
    /**
     * Starting channel for internal map
     * Map.ALLOCATED_WIDTH*Map.ALLOCATED_HEIGHT = 126^2 = 15876 channels needed
     */
    public static final int MAP_DATA = 1;
    
    
    /**
     * Starting channel storing number of robots of each type that exist now
     * 48 channels needed
     */
    public static final int UNIT_COUNT_BASE = 16002;
    
    /**
     * Channel storing number of units produced since start of game by you (including
     * towers, HQ)
     */
    public static final int SEQ_UNIT_NUMBER = 16001;
    
    public static final int LAST_ATTACKED_COORDINATES = 16050;
    
    /**
     * Starting channel for beaver build request system
     * 3 channels needed
     */
    public static final int BEAVER_BUILD_REQUEST = 16071;
    
    /**
     * Starting channel for effective miner count system
     * 2 channels needed
     */
    public static final int EFFECTIVE_MINERS_COUNT = 16075; // 2 channels needed

}
