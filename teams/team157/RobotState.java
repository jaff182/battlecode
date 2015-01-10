package team157;

/**
 * Reflects the state of a robot. Configure individual behaviours based on this.
 * @author Josiah
 *
 */
public enum RobotState {
    ADVANCE, // Move and attack
    HOLD_ACTIVE, // Provide supply to enable instantaneous attacking
    HOLD_INACTIVE, // Remove supply in area (concentrating in certain units, possibly this one)
    RETREAT // Run!
}
