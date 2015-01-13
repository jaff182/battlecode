package team157.Utility;

import battlecode.common.RobotType;

/**
 *
 */
public class BuildingQueue {

    private RobotType[] buildOrder;
    private RobotType defaultBuilding;
    private int numBuilding;

    public BuildingQueue(RobotType[] buildOrder, RobotType defaultBuilding)
    {
        this.buildOrder = buildOrder;
        this.numBuilding = 0;
        this.defaultBuilding = defaultBuilding;
    }

    public RobotType getNextBuilding()
    {
        if (numBuilding < buildOrder.length)
        {
            return buildOrder[numBuilding];
        }
        // We want to build as many supply depots as possible, whenever we have funds
        return defaultBuilding;
    }

    public void updateBuildingNumber()
    {
        numBuilding += 1;
    }
}
