package team157;

/**
 * Reflects the state of a robot. Configure individual behaviours based on this.
 * @author Josiah
 *
 */
public enum RobotState {
    ATTACK_MOVE, // Move and attack
    MOVE_NO_ATTACK, // Retreat
    HOLD_ACTIVE, // Provide supply to enable instantaneous attacking
    WANDER, // Random walk, attack when needed
    MINE, // Mining mode, for beavers, miners
    BUILD, //Building mode, for beavers
}
