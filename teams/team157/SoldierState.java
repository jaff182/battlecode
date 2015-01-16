package team157;

public enum SoldierState {
    WAIT, // Stay stationary
    ATTACK_MOVE, // Move towards a location, attacking units it sees
    RETREAT // Move towards a location, without attacking
}