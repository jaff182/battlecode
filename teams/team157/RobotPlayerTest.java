package team157;

import static org.junit.Assert.*;

import org.junit.Test;

import battlecode.common.Direction;

public class RobotPlayerTest {

    // test ordinal values of Directions.
    @Test
    public void testDirectionOrdinals() {
        assertEquals(Direction.NORTH.ordinal(), 0);
        assertEquals(Direction.NORTH_EAST.ordinal(), 1);
        assertEquals(Direction.EAST.ordinal(), 2);
        assertEquals(Direction.SOUTH_EAST.ordinal(), 3);
        assertEquals(Direction.SOUTH.ordinal(), 4);
        assertEquals(Direction.SOUTH_WEST.ordinal(), 5);
        assertEquals(Direction.WEST.ordinal(), 6);
        assertEquals(Direction.NORTH_WEST.ordinal(), 7);
        assertEquals(Direction.NONE.ordinal(), 8);
        assertEquals(Direction.OMNI.ordinal(), 9);
        
        
        int sum = 0;
        for (Direction dir: Direction.values()) {
            sum += 1;
        }
        assertEquals(sum, 10);
    }
    
    

}
