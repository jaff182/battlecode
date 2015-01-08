package team157;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import battlecode.common.*;

public class RobotPlayerTest {
    
    @Test
    public void RobotTypeOrdinalUnchanged() {
        assertEquals(RobotPlayer.robotTypes[0],RobotType.HQ);
        
    }
    
}