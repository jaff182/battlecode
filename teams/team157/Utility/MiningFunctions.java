package team157.utility;
import battlecode.common.MapLocation;
import team157.utility.Measure;
import team157.utility.Ore;

/**
 *
 */
public class MiningFunctions {

	public static MapLocation simpleRatio(MapLocation currentLocation, int[][] grid) {
        float maxRatio = 0;
        MapLocation maxPoint = new MapLocation(0, 0);

        int numRows = grid.length;
        int numCols = grid[0].length;

        for (int i = 0; i < numRows; i++)
            for (int j = 0; j < numCols; j++)
            {
                if ((float)grid[i][j]/ Measure.distance(currentLocation.x, currentLocation.y, i, j) > maxRatio)
                {
                    maxRatio = (float)grid[i][j]/ Measure.distance(currentLocation.x, currentLocation.y, i, j);
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
            if ((float)oreValues[i]/ Measure.distance(currentLocation, oreLocations[i]) > maxRatio)
            {
                maxRatio = (float)oreValues[i]/ Measure.distance(currentLocation, oreLocations[i]);
                maxPoint = oreLocations[i];
            }
        }
        return maxPoint;
    }

    public static MapLocation simpleRatio(MapLocation currentLocation, Ore[] ores)
    {
        float maxRatio = 0;
        MapLocation maxPoint = new MapLocation(0, 0);
        int n = ores.length;
        for (Ore ore:ores) {
            if ((float)ore.getValue()/ Measure.distance(currentLocation, ore.getLocation()) > maxRatio)
            {
                maxRatio = (float)ore.getValue()/ Measure.distance(currentLocation, ore.getLocation());
                maxPoint = new MapLocation(ore.getLocation().x, ore.getLocation().y);
            }
        }
        return maxPoint;
}
}
