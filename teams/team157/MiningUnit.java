package team157;

import java.util.Random;

import battlecode.common.*;
import team157.Utility.RobotCount;

public class MiningUnit extends MovableUnit {
    
    private static Direction pathingPreviousDir = Direction.NONE;
    /**
     * Move around randomly, drifting towards higher ore.
     * @throws GameActionException
     */
    public static void wanderTowardsOre() throws GameActionException {
        if(rc.isCoreReady()) {
            double[] oreLevels = new double[8];
            double totalOre = 0;

            updateMyLocation();

            MapLocation[] sensingLoc = MapLocation.getAllMapLocationsWithinRadiusSq(myLocation, 8);
            //Sum contributions
            for(MapLocation loc : sensingLoc) {
                int dirInt = myLocation.directionTo(loc).ordinal();
                if(dirInt < 8) {
                    double ore = rc.senseOre(loc);
                    oreLevels[dirInt] += ore;
                    oreLevels[(dirInt+1)%8] += ore;
                    oreLevels[(dirInt+7)%8] += ore;
                    totalOre += 3*ore;
                }
            }
            double randomOre = totalOre*rand.nextDouble();
            Direction dirToMove = Direction.NONE;
            int dirInt = 0;
            do {
                if(randomOre > oreLevels[dirInt]) {
                    randomOre -= oreLevels[dirInt];
                } else {
                    dirToMove = directions[dirInt];
                    if(rc.canMove(dirToMove)) {
                        rc.move(dirToMove);
                        previousDirection = dirToMove;
                        break;
                    }
                    else {
                        //Reset and choose direction again
                        dirInt = 0;
                        randomOre = totalOre*rand.nextDouble();
                    }
                }
                dirInt++;
            } while(dirInt < 8);
        }
    }
    
    
    public static void goTowardsOre() throws GameActionException {
        if(rc.isCoreReady()) {
            //Attractive force towards each direction
            double[] attraction = new double[8];
            
            MapLocation[] sensingLoc = MapLocation.getAllMapLocationsWithinRadiusSq(myLocation, 8);
            //TODO test this!
            // repel from direction previously came from
            if (pathingPreviousDir != Direction.NONE && pathingPreviousDir!= Direction.OMNI) {
                //System.out.println(pathingPreviousDir);
                attraction[pathingPreviousDir.opposite().ordinal()] -= 10000;
            }
            /**
            for (double i : attraction) {
                System.out.print(i + " ");
            }
            **/
            
            //Sum forces from map
            for(MapLocation loc : sensingLoc) {
                int dirInt = myLocation.directionTo(loc).ordinal();
                if(dirInt < 8) {
                    //Ore attraction
                    double force = rc.senseOre(loc);
                    //Void repulsion
                    if(rc.isPathable(myType,loc)) {
                        force -= 10/myLocation.distanceSquaredTo(loc);
                    }
                    
                    //Add forces
                    attraction[dirInt] += force;
                    attraction[(dirInt+1)%8] += force;
                    attraction[(dirInt+7)%8] += force;
                }
            }
            
            //Sum forces from friendly robots
            for(RobotInfo robotInfo : friends) {
                RobotType type = robotInfo.type;
                if(type == myType || type == RobotType.HQ) {
                    MapLocation loc = robotInfo.location;
                    int dirInt = myLocation.directionTo(loc).ordinal();
                    if(dirInt < 8) {
                        //Add forces
                        double force = -500/myLocation.distanceSquaredTo(loc);
                        attraction[dirInt] += force;
                        attraction[(dirInt+1)%8] += force;
                        attraction[(dirInt+7)%8] += force;
                    }
                }
            }
            
            //Sum forces from enemy robots
            for(RobotInfo robotInfo : enemies) {
                RobotType type = robotInfo.type;
                if(type == myType || type == RobotType.HQ) {
                    MapLocation loc = robotInfo.location;
                    int dirInt = myLocation.directionTo(loc).ordinal();
                    if(dirInt < 8) {
                        //Add forces
                        double force = -10000;
                        attraction[dirInt] += force;
                        attraction[(dirInt+1)%8] += force;
                        attraction[(dirInt+7)%8] += force;
                    }
                }
            }
            
            //Sum forces from enemy towers
            for(MapLocation loc : enemyTowers) {
                int dirInt = myLocation.directionTo(loc).ordinal();
                int distance = myLocation.distanceSquaredTo(loc);
                if(dirInt < 8 && distance <= 36) {
                    //Add forces
                    double force = -100000000;
                    attraction[dirInt] += force;
                    attraction[(dirInt+1)%8] += force;
                    attraction[(dirInt+7)%8] += force;
                }
            }
            
            //Find direction with most ore
            int[] bestDirInts = {-1,-1,-1,-1,-1,-1,-1,-1};
            int maxCount = 0;
            double bestAttraction = -10000000;
            for(int dirInt=0; dirInt<8; dirInt++) {
                if(rc.canMove(directions[dirInt])) {
                    if(attraction[dirInt] > bestAttraction) {
                        bestAttraction = attraction[dirInt];
                        bestDirInts[0] = dirInt;
                        maxCount = 1;
                    } else if(attraction[dirInt] == bestAttraction) {
                        bestDirInts[maxCount] = dirInt;
                        maxCount++;
                    }
                }
            }
            
            //Move
            if(maxCount > 0) {
                Direction dirToMove = directions[bestDirInts[rand.nextInt(maxCount)]];
                rc.move(dirToMove);
                previousDirection = dirToMove;
                pathingPreviousDir = dirToMove;
            }

        }
    }
    
    protected static void minerWander() throws GameActionException {
       //Vigilance
       checkForEnemies();

       //Hill climb ore distribution while being repelled from other units
       updateFriendlyInRange(8);
       updateEnemyInSight();
       goTowardsOre();

       //Distribute supply
       distributeSupply(suppliabilityMultiplier_Preattack);
    }

    // Vigilance: stops everything and attacks when enemies are in attack range.
    protected static void checkForEnemies() throws GameActionException {
        while (enemies.length > 0) {
            if (rc.isWeaponReady()) {
                // basicAttack(enemies);
                priorityAttack(enemies, attackPriorities);
            }
            updateEnemyInRange(attackRange);
            RobotCount.report();
            rc.yield();
        }
    }

    //Parameters ==============================================================

    /**
     * The importance rating that enemy units of each RobotType should be attacked
     * (so higher means attack first). Needs to be adjusted dynamically based on
     * defence strategy.
     */
    protected static int[] attackPriorities = {
        20/*0:HQ*/,         21/*1:TOWER*/,      6/*2:SUPPLYDPT*/,   3/*3:TECHINST*/,
        7/*4:BARRACKS*/,    8/*5:HELIPAD*/,     5/*6:TRNGFIELD*/,   9/*7:TANKFCTRY*/,
        4/*8:MINERFCTRY*/,  1/*9:HNDWSHSTN*/,   10/*10:AEROLAB*/,   13/*11:BEAVER*/,
        2/*12:COMPUTER*/,   16/*13:SOLDIER*/,   15/*14:BASHER*/,    12/*15:MINER*/,
        14/*16:DRONE*/,     17/*17:TANK*/,      18/*18:COMMANDER*/, 11/*19:LAUNCHER*/,
        19/*20:MISSILE*/
    };

    /**
     * Multipliers for the effective supply capacity for friendly unit robotTypes, by
     * which the dispenseSupply() and distributeSupply() methods allocate supply (so
     * higher means give more supply to units of that type).
     */
    protected static double[] suppliabilityMultiplier_Conservative = {
        1/*0:HQ*/,          1/*1:TOWER*/,       1/*2:SUPPLYDPT*/,   1/*3:TECHINST*/,
        1/*4:BARRACKS*/,    1/*5:HELIPAD*/,     1/*6:TRNGFIELD*/,   1/*7:TANKFCTRY*/,
        1/*8:MINERFCTRY*/,  1/*9:HNDWSHSTN*/,   1/*10:AEROLAB*/,    0/*11:BEAVER*/,
        0/*12:COMPUTER*/,   0/*13:SOLDIER*/,    0/*14:BASHER*/,     0.5/*15:MINER*/,
        0/*16:DRONE*/,      0/*17:TANK*/,       0/*18:COMMANDER*/,  0/*19:LAUNCHER*/,
        0/*20:MISSILE*/
    };

    protected static double[] suppliabilityMultiplier_Preattack = {
        0/*0:HQ*/,          0/*1:TOWER*/,       0/*2:SUPPLYDPT*/,   0/*3:TECHINST*/,
        0/*4:BARRACKS*/,    0/*5:HELIPAD*/,     0/*6:TRNGFIELD*/,   0/*7:TANKFCTRY*/,
        0/*8:MINERFCTRY*/,  0/*9:HNDWSHSTN*/,   0/*10:AEROLAB*/,    1/*11:BEAVER*/,
        0/*12:COMPUTER*/,   1/*13:SOLDIER*/,    1/*14:BASHER*/,     1/*15:MINER*/,
        1/*16:DRONE*/,      1/*17:TANK*/,       1/*18:COMMANDER*/,  1/*19:LAUNCHER*/,
        0/*20:MISSILE*/
    };
}