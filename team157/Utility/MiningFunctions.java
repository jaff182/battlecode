package Utility;

/**
 *
 */
public class MiningFunctions {

    public static Point simpleRatio(Point currentLocation, int[][] grid)
    {
        float minRatio = 0;
        Point minPoint = new Point(0, 0);

        int numRows = grid.length;
        int numCols = grid[0].length;

        for (int i = 0; i < numRows; i++)
            for (int j = 0; j < numCols; j++)
            {
                if ((float)grid[i][j]/currentLocation.distance(i, j) > minRatio)
                {
                    minRatio = (float)grid[i][j]/currentLocation.distance(i, j);
                    minPoint = new Point(i, j);
                }
            }
        return minPoint;
    }
}
