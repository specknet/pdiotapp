package com.specknet.pdiot;

import java.util.Map;

public class MovementData {
    public int walking;
    public int running;
    public int sitting;
    public int lying;
    public int stairs;
    public int deskwork;
    public void Init()
    {
        walking=0;
        sitting=0;
        running=0;
        stairs=0;
        lying=0;
        deskwork=0;
    }
    public MovementData()
    {
        walking=0;
        sitting=0;
        running=0;
        stairs=0;
        lying=0;
        deskwork=0;
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
                case "using Stairs" :
                    stairs=actionRecorder.get(key);
                    break;
                default:
            }
        }
    }
    public void AddMovementData(MovementData otherMove)
    {
        stairs+=otherMove.stairs;
        deskwork+=otherMove.deskwork;
        sitting+=otherMove.sitting;
        walking+=otherMove.walking;
        running+=otherMove.running;
        lying+=otherMove.lying;
    }


}
