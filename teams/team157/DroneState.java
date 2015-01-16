package team157;

public enum DroneState {
    SWARM, // aggressive mode for drones in a group
    UNSWARM, // defensive mode for lone drones, stays away from target waits for reinforcements
    FOLLOW, // following enemy
    KAMIKAZE, // all out attack
    RETREAT, // retreats when enemy is in sight range and then stays still.
    SURROUND, // circles around enemy
    SCOUT // scouting map for enemies or terrain
}
