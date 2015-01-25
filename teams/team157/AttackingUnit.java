package team157;
import team157.Utility.*;
import battlecode.common.*;


public class AttackingUnit extends MovableUnit{
    /**
     * AF:<br>
     * Represents an attack unit that has state represented by variable state. <br>
     * 
     * Three states are available:<br>
     * 1) ATTACKING_UNIT, where the unit moves closer to attack the unit
     * specified by attackTarget<br>
     * 2) RETREATING, where the unit attempts to move to retreatLocation, firing
     * its weapon only if it has no effect on the retreat<br>
     * 3) ADVANCING, where the unit attempts to move towards advanceLocation
     * without firing its weapon<br>
     * 
     * RI:<br>
     * When the unit is ATTACKING_UNIT, attackTarget may not be null.<br>
     * advanceLocation, state and retreatLocation may never be null.<br>
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
    
    
    // Constants to tweak ========================================
    /**
     * Distance (squared) threshold at which friendly units are counted in
     * whether deciding whether to retreat or advance (and attack).
     * 
     * Motivation: friendly units far away are of little help in taking or
     * giving damage, while enemies can engage at all distances (from the
     * perspective of individual units)
     * 
     * This is the same argument as seen in
     * {@link #macroScoringOfAdvantageInArea(RobotInfo[], int)}
     * 
     * A value to use might be 10.
     */
     static int macroScoringFriendlyLocationThreshold;

    /**
     * The threshold of advantage, as given by the equation in
     * {@link #macroScoringOfAdvantageInArea}, before the unit will retreat.
     * 
     * A greater number here will make the unit retreat even in the face of good
     * odds.
     * 
     * Bias: Enemy units may be invisible beyond sight range, but friendly units
     * are not. Hence, you might consistently overestimate the advantage in your
     * area. You might thus want to set this greater than 1.0 to balance for this.
     * 
     * Note the caveats about missiles and launchers.
     */
     static double advantageBeforeRetreating;
    
    /**
     * Entry point into robot from external code.
     * @throws GameActionException
     */
    public static void start(int macroScoringFriendlyLocationThreshold, double advantageBeforeRetreating) throws GameActionException {
        AttackingUnit.macroScoringFriendlyLocationThreshold = macroScoringFriendlyLocationThreshold;
        AttackingUnit.advantageBeforeRetreating = advantageBeforeRetreating;

        init();
        while(true) {
            loop();
            Common.rc.yield(); //Yield the round
        }
    }
    
    /**
     * Code to init robot goes here.
     * @throws GameActionException 
     */
    private static void init() throws GameActionException {
        advanceLocation = Common.enemyHQLocation;
        retreatLocation = Common.HQLocation;
        state = MovableUnitState.ADVANCING;

    }

    private static void setAttackTargetToNearestEnemy(RobotInfo[] nearbyEnemies)
    {

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
        RobotController rc = Common.rc; // bring rc into local scope
        
        myLocation = rc.getLocation();
        enemiesInSight = rc.senseNearbyRobots(sightRange, enemyTeam);
        numberOfEnemiesInSight = enemiesInSight.length;
        enemies = rc.senseNearbyRobots(attackRange, enemyTeam);
        
        if (Clock.getRoundNum() >= 1750) {
            setTargetToClosestTowerOrHQ();
            advanceLocation = target;
        }
        
        double macroScoringAdvantage = macroScoringOfAdvantageInArea(
                rc.senseNearbyRobots(25), macroScoringFriendlyLocationThreshold);

        // State transitions
        if (macroScoringAdvantage < advantageBeforeRetreating) {
            state = MovableUnitState.RETREATING;
        } else {
            RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(30, Common.enemyTeam);
            if (nearbyEnemies.length != 0) {
                setAttackTargetToNearestEnemy(nearbyEnemies);
                state = MovableUnitState.ATTACKING_UNIT;
            } else {
                state = MovableUnitState.ADVANCING;
            }
        }

        rc.setIndicatorString(2, "State: " + state);
        // Action
        switch (state) {
        case ADVANCING:
            if (myType == RobotType.COMMANDER
                    && rc.hasLearnedSkill(CommanderSkillType.FLASH)
                    && macroScoringAdvantage > 2.5
                    && rc.getFlashCooldown() == 0
                    && rc.getCoreDelay() < 1
                    && bugDirection(advanceLocation) != myLocation
                            .directionTo(advanceLocation)) {

                MapLocation bestFlashLocation = getBestFlashLocation(advanceLocation);

                if (bestFlashLocation != null) {
                    rc.castFlash(bestFlashLocation);
                } else {
                    bug(advanceLocation);
                }
            } else {
                bug(advanceLocation);
            }
            break;
        case ATTACKING_UNIT:
            rc.setIndicatorString(2, "State: " + state + " with target "
                    + attackTarget.location);
            attackTarget(macroScoringAdvantage);
            break;
        case RETREATING:
            if (myType.cooldownDelay == 0 && rc.isWeaponReady()) {
                MovableUnit.basicAttack(rc.senseNearbyRobots(
                        myType.attackRadiusSquared, Common.enemyTeam));
            }

            if (!MovableUnit.retreat()) {
                bug(retreatLocation);
            }
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
     * For launchers, special purpose code is used to check if they can launch.<br>
     * Warning: This function might not be reliable in the presence of missiles.
     * Write special purpose code to execute retreats.
     * 
     * @param robots
     *            all movable robots in an area (excluding HQ and towers)
     * @return a positive score if area is safe, negative if dangerous
     */
    public static double macroScoringOfAdvantageInArea(RobotInfo[] robots, int macroScoringFriendlyDistanceThreshold) {
        double yourHP = rc.getHealth();
        double yourDamageDealtPerUnitTime = myType.attackPower/myType.attackDelay;

        double enemyHP = 0;
        double enemyDamageDealtPerUnitTime = 0;
        
        for (int i = 0; i < robots.length; ++i) {
            final RobotInfo robot = robots[i];
            if (!robot.type.isBuilding && robot.type != RobotType.LAUNCHER && robot.type != RobotType.MISSILE) {
                if (robot.team == Common.myTeam && myLocation.distanceSquaredTo(robot.location) < macroScoringFriendlyLocationThreshold) {
                    yourHP += robot.health;
                    yourDamageDealtPerUnitTime += robot.type.attackPower
                            / robot.type.attackDelay;
                } else if (robot.team != Common.myTeam) {
                    enemyHP += robot.health;
                    enemyDamageDealtPerUnitTime += robot.type.attackPower
                            / robot.type.attackDelay;
                }
            } else if (robot.type == RobotType.LAUNCHER) {
                // Special code for LAUNCHER, since it's attack power is technically 0
                // Roughly perform attack computation over next 3 missile launches (based on missile quantities) and average that
                // TODO: rationalizing choice of 3 (possibly engagement length?)
                double robotDamageDealtPerUnitTime = Math.min(robot.missileCount, 3)*RobotType.MISSILE.attackPower;
                if (robot.missileCount<3) {
                    // (Missile damage)/(Time taken to produce next missile)
                    robotDamageDealtPerUnitTime += RobotType.MISSILE.attackPower/robot.weaponDelay;
                }
                if (robot.team == Common.myTeam && myLocation.distanceSquaredTo(robot.location) < macroScoringFriendlyLocationThreshold) {
                    yourHP += robot.health;
                    yourDamageDealtPerUnitTime += robotDamageDealtPerUnitTime;
                } else if (robot.team != Common.myTeam) {
                    enemyHP += robot.health;
                    enemyDamageDealtPerUnitTime += robotDamageDealtPerUnitTime;
                }
            } else if (robot.type == RobotType.MISSILE){
                // Again, special purpose code for missiles, which only add damage
                if (robot.team == Common.myTeam && myLocation.distanceSquaredTo(robot.location) < macroScoringFriendlyLocationThreshold)
                    yourDamageDealtPerUnitTime += RobotType.MISSILE.attackPower;
                else if (robot.team != Common.myTeam)
                    enemyDamageDealtPerUnitTime += RobotType.MISSILE.attackPower;
            }
        }

        if (enemyHP != 0 && enemyDamageDealtPerUnitTime !=0)
            return ((yourHP/enemyDamageDealtPerUnitTime) / (enemyHP/yourDamageDealtPerUnitTime));
        else 
            return Double.POSITIVE_INFINITY;
    }

    private static MapLocation getBestFlashLocation(MapLocation advanceLocation) throws GameActionException {
        // We're not bugging properly (possibly), and we can flash, and
        // we have an overwhelming advantage (possibly irrelvant, since
        // we can't see enemies to be in this state).
        MapLocation bestFlashLocation = null;
        int bestFlashDistance = Integer.MAX_VALUE;
        for (MapLocation location :
                MapLocation.getAllMapLocationsWithinRadiusSq(myLocation, GameConstants.FLASH_RANGE_SQUARED)) {
            if (rc.isPathable(myType, location) && MovableUnit.movePossible(location)) {
                final int targetDistance = location.distanceSquaredTo(advanceLocation);

                if (targetDistance < bestFlashDistance) {
                    bestFlashDistance = targetDistance;
                    bestFlashLocation = location;
                }
            }
        }
        return bestFlashLocation;
    }

    /**
     * Time until robot can move, close in on enemy, and then shoot it, lower than how long it takes for enemy to
     * shoot you, assuming it's stationary
     * @param distanceToEnemy
     * @param enemyAttackRadius
     * @param myCooldownRate
     * @param enemyCooldownRate
     * @return
     */
    private static boolean canShootFasterThanEnemy(double distanceToEnemy, double enemyAttackRadius, double myCooldownRate,
                                                   double enemyCooldownRate)
    {
        return (myType.movementDelay * (distanceToEnemy - enemyAttackRadius) + myType.loadingDelay) / myCooldownRate
                <= attackTarget.weaponDelay / enemyCooldownRate;
    }

    private static void attackTarget(double macroScoringAdvantage) throws GameActionException {
        final int distanceToEnemySquared = myLocation.distanceSquaredTo(attackTarget.location);
        final double distanceToEnemy = Math.sqrt(distanceToEnemySquared);
        final double enemyAttackRadius = Math.sqrt(attackTarget.type.attackRadiusSquared);

        final double myCooldownRate;
        if (rc.getSupplyLevel() >= myType.supplyUpkeep) {
            myCooldownRate = 1.0;
        }
        else {
            myCooldownRate = 0.5;
        }

        final double enemyCooldownRate;

        if (attackTarget.supplyLevel >= attackTarget.type.supplyUpkeep) {
            enemyCooldownRate = 1.0;
        }
        else {
            enemyCooldownRate = 0.5;
        }

        if (rc.canAttackLocation(attackTarget.location)) {
            if (rc.isWeaponReady()) {
                rc.attackLocation(attackTarget.location);
            }
            rc.setIndicatorString(1, "Attacking in range");
        } else if (macroScoringAdvantage > 1.5 || !attackTarget.type.canAttack()) {
            rc.setIndicatorString(1, "Advancing to attack");
            bug(attackTarget.location);
        } else if (canShootFasterThanEnemy(distanceToEnemy, enemyAttackRadius, myCooldownRate, enemyCooldownRate)) {
            bug(attackTarget.location);
            rc.setIndicatorString(1, "advancing to attack");
        } else {
            rc.setIndicatorString(1, "No attack indicated, waiting here.");
        }
    }
}