package team157.Utility;

import battlecode.common.MapLocation;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * This map helps to evaluate the risk of any position
 *
 * Unit sighting: decrease over time, exponentially
 * Building sighting: remain constant
 */
public class RiskMap {

	private ArrayList<MapLocation> sightings;
	private float radius = 5;

	public RiskMap()
	{
		this.sightings = new ArrayList<MapLocation>();
	}

	public void addSighting(MapLocation location)
	{
		this.sightings.add(location);
	}

	public float query(MapLocation loc)
	{
		for (Iterator<MapLocation> i = sightings.iterator(); i.hasNext(); ) {
			MapLocation sighting = i.next();
			if (loc.distanceSquaredTo(sighting) > radius*radius) {
				return 1;
			}
		}
		return 0;
	}
}
