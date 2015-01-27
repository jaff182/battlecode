package launcherBot;
import battlecode.common.*;

public class Commander extends MovableUnit{
    ;
    
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
            Common.rc.yield(); //Yield the round
        }
    }
    
    /**
     * Code to init robot goes here.
     */
    private static void init() {
        advanceLocation = Common.enemyHQLocation;
        retreatLocation = Common.HQLocation;
        state = MovableUnitState.ADVANCING;
    }

    /**
     * This code is run once every round, until rc.yield() is called.
     * @throws GameActionException 
     */
    private static void loop() throws GameActionException {
        RobotController rc = Common.rc; // bring rc into local scope
        updateMyLocation();
        
        // State transitions
        if (isDisadvantaged()) {
            state = MovableUnitState.RETREATING;
        } else {
            if (rc.senseNearbyRobots(30, Common.enemyTeam).length > 0) {
                setAttackTargetToNearestEnemy();
                state = MovableUnitState.ATTACKING_UNIT;
            } else {
                state = MovableUnitState.ADVANCING;
            }
        }
        
        rc.setIndicatorString(2, "State: " + state);
        // Action
        switch (state) {
        case ADVANCING:
            bug(advanceLocation);
            break;
        case ATTACKING_UNIT:
            final int distanceToEnemySquared = myLocation.distanceSquaredTo(attackTarget.location);
            
            if (distanceToEnemySquared <= myType.attackRadiusSquared) {
                if (rc.isWeaponReady() && rc.canAttackLocation(attackTarget.location))
                {
                    rc.attackLocation(attackTarget.location);
                }
                rc.setIndicatorString(1, "Attacking in range");
            } else if (canApproachAndShoot(distanceToEnemySquared)) {
                // Time until robot can move, close in on enemy, and then shoot
                // it, lower than how long it takes for enemy to shoot you assuming it's stationary
                bug(attackTarget.location);
                rc.setIndicatorString(1, "Our moving+shooting delay<enemy shooting delay, advancing advantageous");
            }
            else
                rc.setIndicatorString(1, "No attack indicated, waiting here.");
            break;
        case RETREATING:
            MovableUnit.retreat();
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
    private static double macroScoringOfAdvantageInArea(RobotInfo[] robots) {
        double yourHP = rc.getHealth();
        double yourDamageDealtPerUnitTime = myType.attackPower/myType.attackDelay;

        double enemyHP = 0;
        double enemyDamageDealtPerUnitTime = 0;
        
        for (int i = 0; i < robots.length; ++i) {
            final RobotInfo robot = robots[i];
            if (!robot.type.isBuilding) {
                if (robot.team == Common.myTeam) {
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
        rc.setIndicatorString(1, "AlliesHP: " + yourHP + " myDPR: " + yourDamageDealtPerUnitTime + " EnemyHP: " + enemyHP + " enemyDamage " + enemyDamageDealtPerUnitTime);

        if (enemyHP != 0 && enemyDamageDealtPerUnitTime !=0)
            return ((yourHP/enemyDamageDealtPerUnitTime) / (enemyHP/yourDamageDealtPerUnitTime));
        else 
            return Double.POSITIVE_INFINITY;
    }

    private static boolean isDisadvantaged()
    {
        return macroScoringOfAdvantageInArea(rc.senseNearbyRobots(30))<1.5;
    }

    private static void setAttackTargetToNearestEnemy()
    {
        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(30, Common.enemyTeam);
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

    private static boolean canApproachAndShoot(int distanceToEnemySquared)
    {
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

        return (myType.movementDelay*(distanceToEnemy-enemyAttackRadius)+myType.loadingDelay)/myCooldownRate <=
                attackTarget.weaponDelay/enemyCooldownRate;
    }
}