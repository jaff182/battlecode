package team157;
import team157.Utility.*;
import battlecode.common.*;

public class AttackingUnit extends MovableUnit{
    /**
     * AF:<br>
     * Represents an attack unit that has state represented by variable state. <br>
     * 
     * Three states are available:<br>
     * 1) ATTACKING_UNIT, where the unit moves closer to attack the unit specified by attackTarget<br>
     * 2) RETREATING, where the unit attempts to move to retreatLocation without firing its weapon<br>
     * 3) ADVANCING, where the unit attempts to move towards advanceLocation without firing its weapon<br>
     * 
     * RI:<br>
     * When the unit is ATTACKING_UNIT, attackTarget may not be null.<br>
     * Both advanceLocation, state and retreatLocation may never be null.<br>
     * 
     * These rep invariants must be satisfied after init() is called.
     */
    
    // Variables controlling state ==============================
    enum MovableUnitState {
        ATTACKING_UNIT,
        RETREATING, 
        ADVANCING
    };
    
    private static MovableUnitState state;
    
    private static RobotInfo attackTarget;
    private static MapLocation advanceLocation;
    private static MapLocation retreatLocation;
    
    /**
     * Entry point into robot from external code.
     * @throws GameActionException
     */
    public static void start() throws GameActionException {
        init();
        while(true) {
            loop();
            RobotPlayer.rc.yield(); //Yield the round
        }
    }
    
    /**
     * Code to init robot goes here.
     * @throws GameActionException 
     */
    private static void init() throws GameActionException {
        advanceLocation = RobotPlayer.enemyHQLocation;
        retreatLocation = RobotPlayer.HQLocation;
        state = MovableUnitState.ADVANCING;
        if (Clock.getRoundNum() < 1800) {
            initInternalMap(); //set locations within attack radius of enemy tower or hq as unpathable
            //Map.printInternalMap();
        } 

    }

    private static void setAttackTargetToNearestEnemy()
    {
        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(30, RobotPlayer.enemyTeam);

        RobotInfo nearestEnemy = null;
        int nearestEnemyDistance = Integer.MAX_VALUE;
        for (int i=0; i<nearbyEnemies.length; i++) {
            final int distance = nearbyEnemies[i].location.distanceSquaredTo(myLocation);
            if (nearestEnemyDistance > nearbyEnemies[i].location.distanceSquaredTo(myLocation)) {
                nearestEnemy = nearbyEnemies[i];
                nearestEnemyDistance = distance;
            }
        }
        attackTarget = nearestEnemy;
    }

    /**
     * This code is run once every round, until rc.yield() is called.
     * @throws GameActionException 
     */
    private static void loop() throws GameActionException {
        RobotController rc = RobotPlayer.rc; // bring rc into local scope
        
        myLocation = rc.getLocation();
        enemiesInSight = rc.senseNearbyRobots(sightRange, enemyTeam);
        numberOfEnemiesInSight = enemiesInSight.length;
        enemies = rc.senseNearbyRobots(attackRange, enemyTeam);
        
        
        if (Clock.getRoundNum() >= 1750) {
            setTargetToClosestTowerOrHQ();
            advanceLocation = target;
        }
        
        double macroScoringAdvantage = macroScoringOfAdvantageInArea(rc.senseNearbyRobots(25));
        // State transitions
        if (macroScoringAdvantage<2) {
            state = MovableUnitState.RETREATING;
        } else {
            if (rc.senseNearbyRobots(30, RobotPlayer.enemyTeam).length != 0) {
                setAttackTargetToNearestEnemy();
                state = MovableUnitState.ATTACKING_UNIT;
            } else
                state = MovableUnitState.ADVANCING;

        }
        
        rc.setIndicatorString(2, "State: " + state);
        // Action
        switch (state) {
        case ADVANCING:
            if (rc.getType() == RobotType.COMMANDER){
                if (rc.hasLearnedSkill(CommanderSkillType.FLASH)
                        && macroScoringAdvantage > 2.5
                        && rc.getFlashCooldown() == 0
                        && rc.getCoreDelay() < 1
                        && bugDirection(advanceLocation) != myLocation
                                .directionTo(advanceLocation)) {
                    // We're not bugging properly (possibly), and we can flash, and
                    // we have an overwhelming advantage (possibly irrelvant, since
                    // we can't see enemies to be in this state).
                    MapLocation bestFlashLocation = null;
                    int bestFlashDistance = Integer.MAX_VALUE;
                    for (MapLocation location : MapLocation
                            .getAllMapLocationsWithinRadiusSq(myLocation,
                                    GameConstants.FLASH_RANGE_SQUARED)) {
                        if (rc.isPathable(myType, location) && MovableUnit.movePossible(location)) {
                            final int targetDistance = location
                                    .distanceSquaredTo(advanceLocation);
                            if (targetDistance < bestFlashDistance) {
                                bestFlashDistance = targetDistance;
                                bestFlashLocation = location;
                            }
                        }
                    }
                    if (bestFlashLocation != null) {
                        rc.castFlash(bestFlashLocation);
                    } else
                        bug(advanceLocation);
            } else
                bug(advanceLocation);
            }
            break;
        case ATTACKING_UNIT:
            rc.setIndicatorString(2, "State: " + state + " with target " + attackTarget.location);

            final int distanceToEnemySquared = myLocation.distanceSquaredTo(attackTarget.location);
            final double distanceToEnemy = Math.sqrt(distanceToEnemySquared);
            final double enemyAttackRadius = Math.sqrt(attackTarget.type.attackRadiusSquared);
            
            final double myCooldownRate;
            if (rc.getSupplyLevel() >= myType.supplyUpkeep)
                myCooldownRate = 1.0;
            else
                myCooldownRate = 0.5;
            
            final double enemyCooldownRate;
            if (attackTarget.supplyLevel >= attackTarget.type.supplyUpkeep)
                enemyCooldownRate = 1.0;
            else
                enemyCooldownRate = 0.5;
            
            if (rc.canAttackLocation(attackTarget.location)) {
                if (rc.isWeaponReady())
                        rc.attackLocation(attackTarget.location);
                rc.setIndicatorString(1, "Attacking in range");
            } else if (macroScoringAdvantage > 1.5 || !attackTarget.type.canAttack()) {
                rc.setIndicatorString(1, "Advancing to attack");
                bug(attackTarget.location);
            } else if ((myType.movementDelay
                    * (distanceToEnemy - enemyAttackRadius) + myType.loadingDelay)
                    / myCooldownRate <= attackTarget.weaponDelay
                    / enemyCooldownRate) {
                // Time until robot can move, close in on enemy, and then shoot
                // it, lower than how long it takes for enemy to shoot you
                // assuming it's stationary
                bug(attackTarget.location);
                rc.setIndicatorString(1,
                        "advancing to attack");
            } else
                rc.setIndicatorString(1, "No attack indicated, waiting here.");
            break;
        case RETREATING:
            if (myType.cooldownDelay == 0 && rc.isWeaponReady())
                MovableUnit.basicAttack(rc.senseNearbyRobots(
                        myType.attackRadiusSquared, RobotPlayer.enemyTeam));
            if (MovableUnit.retreat())
                bug(retreatLocation);
            break;
        default:
            break;
        
        }
        
    }
    
    /**
     * Scoring function for advantage in an area.
     * 
     * Computes (where all variables are aggregates over robots of one side)<br>
     * 
     * (your hp)/(average damage dealt over time by enemy) <br>
     * --------------------------------------------------- <br>
     * (enemy hp)/(average damage dealt over time by you)
     * 
     * i.e., in an idealized and very rough scenario, how many times your robots
     * can fight the same battle before giving out.
     * 
     * @param robots
     *            all movable robots in an area (excluding HQ and towers)
     * @return a positive score if area is safe, negative if dangerous
     */
    public static double macroScoringOfAdvantageInArea(RobotInfo[] robots) {
        double yourHP = rc.getHealth();
        double yourDamageDealtPerUnitTime = myType.attackPower/myType.attackDelay;

        double enemyHP = 0;
        double enemyDamageDealtPerUnitTime = 0;
        
        for (int i = 0; i < robots.length; ++i) {
            final RobotInfo robot = robots[i];
            if (!robot.type.isBuilding) {
                if (robot.team == RobotPlayer.myTeam) {
                    yourHP += robot.health;
                    yourDamageDealtPerUnitTime += robot.type.attackPower
                            / robot.type.attackDelay;
                } else {
                    enemyHP += robot.health;
                    enemyDamageDealtPerUnitTime += robot.type.attackPower
                            / robot.type.attackDelay;
                }
            }
        }

        if (enemyHP != 0 && enemyDamageDealtPerUnitTime !=0)
            return ((yourHP/enemyDamageDealtPerUnitTime) / (enemyHP/yourDamageDealtPerUnitTime));
        else 
            return Double.POSITIVE_INFINITY;
    }

}