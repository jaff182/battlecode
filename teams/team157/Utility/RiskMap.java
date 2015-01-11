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

    private ArrayList<MapLocation> buildings;
    private ArrayList<MapLocation> units;
    private ArrayList<Double> risks;
    private ArrayList<Integer> times;
    private float radius = 5;

    public RiskMap()
    {
        this.buildings = new ArrayList<MapLocation>();
        this.units = new ArrayList<MapLocation>();
        this.risks = new ArrayList<Double>();
        this.times = new ArrayList<Integer>();
    }

    public void addBuilding(MapLocation location)
    {
        this.buildings.add(location);
    }

    public void addUnit(MapLocation location)
    {
        this.units.add(location);
        this.risks.add(1.0);
    }

    public void removeUnit(MapLocation location)
    {
        this.units.remove(location);
    }

    public void removeBuilding(MapLocation location)
    {
        this.buildings.remove(location);
    }

    public double query(MapLocation loc)
    {
        // scanning for enemy building within the radius
        for (Iterator<MapLocation> i = buildings.iterator(); i.hasNext(); ) {
            MapLocation building = i.next();
            if (Measure.distance(building, loc) > radius) {
                return 1;
            }
        }

        // scanning for enemy unit within the radius
        for (Iterator<MapLocation> i = units.iterator(); i.hasNext(); ) {
                   MapLocation unit = i.next();
                   if (Measure.distance(unit, loc) > radius) {
                       return risks.get(units.indexOf(unit));
                   }
               }
        return 0;
    }
}
