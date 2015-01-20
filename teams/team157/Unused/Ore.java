package team157.Unused;

import battlecode.common.MapLocation;

/**
 *
 */
public class Ore {

    private int value;
    private MapLocation location;

    public Ore(int value, MapLocation location)
    {
        this.value = value;
        this.location = location;
    }

    public void setValue(int value)
    {
        this.value = value;
    }

    public int getValue()
    {
        return this.value;
    }

    public MapLocation getLocation()
    {
        return this.location;
    }

    public void setLocation(MapLocation location)
    {
        this.location = location;
    }
}
