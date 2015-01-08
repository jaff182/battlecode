package Utility;

/**
 *
 */
public class MiningFunctions {

    // TODO: for efficiency's sake, we might maintain a list of ore values & locations
    public static Point simpleRatio(Point currentLocation, int[][] grid) {
        float maxRatio = 0;
        Point maxPoint = new Point(0, 0);

        int numRows = grid.length;
        int numCols = grid[0].length;

        for (int i = 0; i < numRows; i++)
            for (int j = 0; j < numCols; j++)
            {
                if ((float)grid[i][j]/currentLocation.distance(i, j) > maxRatio)
                {
                    maxRatio = (float)grid[i][j]/currentLocation.distance(i, j);
                    maxPoint = new Point(i, j);
                }
            }
        return maxPoint;
    }

    public static Point simpleRatio(Point currentLocation, Point[] oreLocations, int[] oreValues){
        float maxRatio = 0;
        Point maxPoint = new Point(0, 0);

        int n = oreLocations.length;

        for (int i = 0; i < n; i++) {
            if ((float)oreValues[i]/currentLocation.distance(oreLocations[i]) > maxRatio)
            {
                maxRatio = (float)oreValues[i]/currentLocation.distance(oreLocations[i]);
                maxPoint = oreLocations[i];
            }
        }

        return maxPoint;
    }
}
