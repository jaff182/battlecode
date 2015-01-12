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
    
    @Test
    public void RobotTypeOrdinalUnchanged() {
        assertEquals(RobotPlayer.robotTypes[0],RobotType.HQ);
        assertEquals(RobotPlayer.robotTypes[1],RobotType.TOWER);
        assertEquals(RobotPlayer.robotTypes[2],RobotType.SUPPLYDEPOT);
        assertEquals(RobotPlayer.robotTypes[3],RobotType.TECHNOLOGYINSTITUTE);
        assertEquals(RobotPlayer.robotTypes[4],RobotType.BARRACKS);
        assertEquals(RobotPlayer.robotTypes[5],RobotType.HELIPAD);
        assertEquals(RobotPlayer.robotTypes[6],RobotType.TRAININGFIELD);
        assertEquals(RobotPlayer.robotTypes[7],RobotType.TANKFACTORY);
        assertEquals(RobotPlayer.robotTypes[8],RobotType.MINERFACTORY);
        assertEquals(RobotPlayer.robotTypes[9],RobotType.HANDWASHSTATION);
        assertEquals(RobotPlayer.robotTypes[10],RobotType.AEROSPACELAB);
        assertEquals(RobotPlayer.robotTypes[11],RobotType.BEAVER);
        assertEquals(RobotPlayer.robotTypes[12],RobotType.COMPUTER);
        assertEquals(RobotPlayer.robotTypes[13],RobotType.SOLDIER);
        assertEquals(RobotPlayer.robotTypes[14],RobotType.BASHER);
        assertEquals(RobotPlayer.robotTypes[15],RobotType.MINER);
        assertEquals(RobotPlayer.robotTypes[16],RobotType.DRONE);
        assertEquals(RobotPlayer.robotTypes[17],RobotType.TANK);
        assertEquals(RobotPlayer.robotTypes[18],RobotType.COMMANDER);
        assertEquals(RobotPlayer.robotTypes[19],RobotType.LAUNCHER);
        assertEquals(RobotPlayer.robotTypes[20],RobotType.MISSILE);
        System.out.println("RobotTypeOrdinalUnchanged test passed.");
    }
    

}
