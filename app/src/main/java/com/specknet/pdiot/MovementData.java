package com.specknet.pdiot;

import java.util.Map;

public class MovementData {
    public int walking;
    public int running;
    public int sitting;
    public int lying;
    //public int sitstanding;
    //public int stairsdown;
    public int stairsup;
    public int deskwork;
    public int stateTransition;
    public void Init()
    {
        walking=0;
        sitting=0;
        running=0;
        stairsup=0;
        //stairsdown=0;
        lying=0;
        deskwork=0;
        stateTransition=0;
        //sitstanding=0;
    }
    public MovementData()
    {
        Init();
    }
    public MovementData(Map<String,Integer> actionRecorder)
    {
        this.Init();
        for(String key:actionRecorder.keySet())
        {
            switch (key)
            {
                case "walking":
                    walking=actionRecorder.get(key);
                    break;
                case "lying":
                    lying=actionRecorder.get(key);
                    break;
                case "sitting":
                    sitting=actionRecorder.get(key);
                    break;
                case "running":
                    running=actionRecorder.get(key);
                    break;
                case "deskworking":
                    deskwork=actionRecorder.get(key);
                    break;
                case "stairsup" :
                    stairsup=actionRecorder.get(key);
                    break;
                //case "stairsdown":
                  /*  stairsdown=actionRecorder.get(key);
                    break;
                case "sitstanding":
                    sitstanding=actionRecorder.get(key);
                    break;*/
                case "transition":
                    stateTransition=actionRecorder.get(key);
                default:
            }
        }
    }
    public void AddMovementData(MovementData otherMove)
    {
        stairsup+=otherMove.stairsup;
        deskwork+=otherMove.deskwork;
        sitting+=otherMove.sitting;
        walking+=otherMove.walking;
        running+=otherMove.running;
        lying+=otherMove.lying;
        stateTransition+=otherMove.stateTransition;
        //sitstanding+=otherMove.sitstanding;
        //stairsdown+=otherMove.stairsdown;
    }
    public float ActivityLevel()
    {
        int activeSecs=10*running+5*stairsup+2*walking+stateTransition;
        int passiveSecs=10*lying+5*sitting+2*deskwork;
        int allMotion=activeSecs+passiveSecs;
        return activeSecs/(float)allMotion;
    }


}
