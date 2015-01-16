package team157.Utility;

public class SoldierGroup {
    
    public final int X_COORDINATE_CHANNEL = 30000;
    public final int Y_COORDINATE_CHANNEL = 30001;
    public final int MOVE_TYPE_CHANNEL = 30002;
    
    public static void setNextLocation(int x, int y, MoveType moveType) {
    }
    
    public static enum MoveType {
        ATTACK, // Attack move to an area as a group.
        RALLY, // Build up forces in an area
        RETREAT // Move, but do not attack
        }


    public static int x;
    public static int y;
    public static MoveType moveType;
    
    /**
     * Write to x, y, moveType with the current instructions.
     */
    public static void getNextLocation() {
        
    }
    
}
