package team156;
import team156.Utility.*;
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
     */
    private static void init() {
        advanceLocation = RobotPlayer.enemyHQLocation;
        retreatLocation = RobotPlayer.HQLocation;
        state = MovableUnitState.ADVANCING;
        initInternalMap(); //set locations within attack radius of enemy tower or hq as unpathable
    }
    
    /**
     * This code is run once every round, until rc.yield() is called.
     * @throws GameActionException 
     */
    private static void loop() throws GameActionException {
        RobotController rc = RobotPlayer.rc; // bring rc into local scope
        
        // State transitions
        if (macroScoringOfAdvantageInArea(rc.senseNearbyRobots(30))<1.3) {
            state = MovableUnitState.RETREATING;
        } else {
            RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(40,
                    RobotPlayer.enemyTeam);
            if (nearbyEnemies.length != 0) {
                RobotInfo nearestEnemy = null;
                int nearestEnemyDistance = Integer.MAX_VALUE;
                for (int i=0; i<nearbyEnemies.length; i++) {
                    final int distance = nearbyEnemies[i].location.distanceSquaredTo(myLocation);
                    if (nearestEnemyDistance > nearbyEnemies[i].location.distanceSquaredTo(myLocation)) {
                        nearestEnemy = nearbyEnemies[i];
                        nearestEnemyDistance = distance;
                    }
                }
                state = MovableUnitState.ATTACKING_UNIT;
                attackTarget = nearestEnemy;
            } else
                state = MovableUnitState.ADVANCING;

        }
        
        rc.setIndicatorString(2, "State: " + state);
        // Action
        switch (state) {
        case ADVANCING:
            bug(advanceLocation);
            break;
        case ATTACKING_UNIT:
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
            
            if (distanceToEnemySquared <= myType.attackRadiusSquared) {
                if (rc.isWeaponReady() && rc.canAttackLocation(attackTarget.location))
                        rc.attackLocation(attackTarget.location);
                rc.setIndicatorString(1, "Attacking in range");
            }
            else if ((distanceToEnemy - enemyAttackRadius) > 1.0/myType.movementDelay) {
                bug(attackTarget.location);
                rc.setIndicatorString(1, "Enemy much too far, move closer");
            }
            else if ((myType.movementDelay*(distanceToEnemy-enemyAttackRadius)+myType.loadingDelay)/myCooldownRate <= attackTarget.weaponDelay/enemyCooldownRate) {
                // Time until robot can move, close in on enemy, and then shoot
                // it, lower than how long it takes for enemy to shoot you assuming it's stationary
                bug(attackTarget.location);
                rc.setIndicatorString(1, "Our moving+shooting delay<enemy shooting delay, advancing advantageous");
            }
            else
                rc.setIndicatorString(1, "No attack indicated, waiting here.");
            break;
        case RETREATING:
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
    private static double macroScoringOfAdvantageInArea(RobotInfo[] robots) {
        double yourHP = 0;
        double yourDamageDealtPerUnitTime = 0;

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