package team157;

public enum DroneState {
    SWARM, // aggressive mode for drones in a group
    UNSWARM, // defensive mode for lone drones
    FOLLOW, // following enemy
    SUICIDE, // all out attack
    RETREAT, // retreating from enemy
    SURROUND, // surrounding enemy
    SCOUT // scouting map for enemies or terrain
}
