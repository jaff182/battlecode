package team157.Utility;

import battlecode.common.MapLocation;

/**
 *
 */
public class Measure {

	public static int distance(int x1, int y1, int x2, int y2) {
		return Math.abs(x1 - x2) + Math.abs(y1 - y2);
	}

	public static int distance(MapLocation loc1, MapLocation loc2)
	{
		return Math.abs(loc1.x - loc2.x) + Math.abs(loc1.y - loc2.y);
	}
}
