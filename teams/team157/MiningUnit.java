package team157;

import java.util.Random;
import battlecode.common.*;

public class MiningUnit extends MovableUnit {
    
    /**
     * Move around randomly, drifting towards higher ore.
     * @throws GameActionException
     */
    public static void wanderTowardsOre() throws GameActionException {
        if(rc.isCoreReady()) {
            double[] oreLevels = new double[8];
            double totalOre = 0;
            myLocation = rc.getLocation();
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
    
    
    public static void goTowardsOre(RobotInfo[] friends, RobotInfo[] enemies) throws GameActionException {
        if(rc.isCoreReady()) {
            double[] attraction = new double[8];
            myLocation = rc.getLocation();
            MapLocation[] sensingLoc = MapLocation.getAllMapLocationsWithinRadiusSq(myLocation, 8);
            
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
                        double force = -1000/myLocation.distanceSquaredTo(loc);
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
                        double force = -1000/myLocation.distanceSquaredTo(loc);
                        attraction[dirInt] += force;
                        attraction[(dirInt+1)%8] += force;
                        attraction[(dirInt+7)%8] += force;
                    }
                }
            }
            
            //Find direction with most ore
            int[] bestDirInts = {-1,-1,-1,-1,-1,-1,-1,-1};
            int maxCount = 0;
            double bestAttraction = -1000000;
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
            }
        }
    }
    
    
}