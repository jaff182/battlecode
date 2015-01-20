package team156;

public enum TankState {
    DEFEND, // defense state for defending a friendly unit
    SWARM, // aggressive mode for drones in a group
    UNSWARM, // defensive mode for lone drones, stays away from target waits for reinforcements
    FOLLOW, // following enemy
    KAMIKAZE; // all out attack

}
