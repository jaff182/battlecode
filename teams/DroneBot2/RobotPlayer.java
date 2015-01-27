package DroneBot2;

import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class RobotPlayer {
    
    //Global variables ========================================================
  
    //Unset Variables
    public static RobotController rc;
    public static RobotType myType;
    
    //Main script =============================================================
    public static void run(RobotController RC) {
        
        //Global inits --------------------------------------------------------
        rc = RC; //set robotcontroller
        myType = rc.getType();
        
        try {
            //Branch depending on whether you are a missile
            //need to do this or missiles will hit bytecode limit
            if(myType == RobotType.MISSILE) Missile.start();
            else Common.commonInit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}