package team157;

import battlecode.common.*;

public class Launcher extends MovableUnit {
    
    //General methods =========================================================

    public static int missileCount = 0;

    public static final int defaultMissileCount = 5;

    public static void start() throws GameActionException {
        init();
        while(true) {
            loop();
            rc.yield(); //Yield the round
        }
    }
    
    private static void init() throws GameActionException {
        initInternalMap(); //set locations within attack radius of enemy tower or hq as unpathable
        target = enemyHQLocation;
        missileCount = 0;
    }
    
    private static void loop() throws GameActionException {
        // Code that runs in every robot (including buildings, excepting missiles)
        // TODO: add code to move launchers
        sharedLoopCode();
        updateMyLocation();
        updateTargets();

        if (missileCount > 0)
        {
            Direction dir0 = myLocation.directionTo(target);
            launchMissile(dir0);
        }
    }
    
    //Specific methods =========================================================
    
    /**
     * Spawn robot of type rbtype in direction dir0 if allowed, transfers supply
     * @param dir0 Direction to spawn at
     * @param robotType RobotType of robot to spawn
     * @throws GameActionException
     */
    public static void launchMissile(Direction dir0) throws GameActionException {
        if(rc.isCoreReady() && rc.getTeamOre() >= RobotType.MISSILE.oreCost) {
            if(rc.canSpawn(dir0,RobotType.MISSILE)) {
                // Not sure if I can do this
                rc.spawn(dir0, RobotType.MISSILE);
                missileCount--;
            }
        }
    }

    public static void stopFiring()
    {
        missileCount = 0;
    }

    public static void fire(MapLocation location, int numMissile)
    {
        target = location;
        missileCount = numMissile;
    }

    public static void fire(MapLocation location)
    {
        target = location;
        missileCount = defaultMissileCount;
    }

    public static void fireEnemyHQ()
    {
        target = enemyHQLocation;
        missileCount = defaultMissileCount;
    }

    private static void updateTargets() throws GameActionException
    {
        if (missileCount == 0)
        {
            rc.readBroadcast(Channels.MISSILE_TARGET);
        }

        target = new MapLocation(rc.readBroadcast(Channels.MISSILE_TARGET + 1),
                rc.readBroadcast(Channels.MISSILE_TARGET + 2));
    }
}