package team157;

import static org.junit.Assert.*;
import org.junit.Test;
import battlecode.common.*;

public class RobotPlayerTest {

    // find ordinal numbers of terrain tile types
    @Test
    public void testTerrainTile() {
        assertEquals(TerrainTile.NORMAL.ordinal(), 0);
        assertEquals(TerrainTile.OFF_MAP.ordinal(), 3);
        assertEquals(TerrainTile.UNKNOWN.ordinal(), 2);
        assertEquals(TerrainTile.VOID.ordinal(), 1);
    }
}
