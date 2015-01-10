package Utility;
import battlecode.common.MapLocation;

/**
 *
 */
public class MiningFunctions {

    protected static int distance(int x1, int y1, int x2, int y2) {
        return Math.abs(x1 - x2) + Math.abs(y1 - y2);
    }

    protected static int distance(MapLocation loc1, MapLocation loc2)
    {
        return Math.abs(loc1.x - loc2.x) + Math.abs(loc1.y - loc2.y);
    }

    public static MapLocation simpleRatio(MapLocation currentLocation, int[][] grid) {
        float maxRatio = 0;
        MapLocation maxPoint = new MapLocation(0, 0);

        int numRows = grid.length;
        int numCols = grid[0].length;

        for (int i = 0; i < numRows; i++)
            for (int j = 0; j < numCols; j++)
            {
                if ((float)grid[i][j]/distance(currentLocation.x, currentLocation.y, i, j) > maxRatio)
                {
                    maxRatio = (float)grid[i][j]/distance(currentLocation.x, currentLocation.y, i, j);
                    maxPoint = new MapLocation(i, j);
                }
            }
        return maxPoint;
    }

    public static MapLocation simpleRatio(MapLocation currentLocation, MapLocation[] oreLocations, int[] oreValues){
        float maxRatio = 0;
        MapLocation maxPoint = new MapLocation(0, 0);

        int n = oreLocations.length;

        for (int i = 0; i < n; i++) {
            if ((float)oreValues[i]/distance(currentLocation, oreLocations[i]) > maxRatio)
            {
                maxRatio = (float)oreValues[i]/distance(currentLocation, oreLocations[i]);
                maxPoint = oreLocations[i];
            }
        }
        return maxPoint;
    }
}
