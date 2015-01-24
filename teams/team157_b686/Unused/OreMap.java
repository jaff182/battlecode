package team157_b686.Unused;

import battlecode.common.MapLocation;

/**
 *
 */
public class OreMap {

    private final int MaxOreSize = 50;
    private Ore[] ores;
    private int oreCount;

    public OreMap()
    {
        oreCount = 0;
        ores = new Ore[MaxOreSize];
    }

    public void addOre(MapLocation location, int value)
    {
        if (oreCount < MaxOreSize) {
            ores[oreCount] = new Ore(value, location);
            oreCount += 1;
        } else
        {
            // potentially resizing the array
        }
    }

    public void addOrUpdateOre(MapLocation location, int value)
    {
        for (int i = 0; i < oreCount; i++)
        {
            if (ores[i].getLocation().equals(location))
            {
                ores[i].setValue(value);
                return;
            }
        }
        addOre(location, value);
    }

    // TODO: for bytecote optimization might use a different location class
    public int getValue(MapLocation location)
    {
        for (int i = 0; i < oreCount; i++)
        {
            if (ores[i].getLocation().equals(location))
            {
                return ores[i].getValue();
            }
        }
        return 0;
    }

    public MapLocation getBestOreLocation(MapLocation currentLocation)
    {
        return MiningFunctions.simpleRatio(currentLocation, ores);
    }
}
