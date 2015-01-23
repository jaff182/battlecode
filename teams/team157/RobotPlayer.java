package team157;

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
        //my properties
        myType = rc.getType();
        if(myType == RobotType.MISSILE){
            try {
                // need to do this or missiles will hit bytecode limit
                Missile.start();
            } catch (Exception e) {
                e.printStackTrace();
            }

        } 
        
        try {
            Common.commonInit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
 
}