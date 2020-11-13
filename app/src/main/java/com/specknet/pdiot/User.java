package com.specknet.pdiot;

public class User {

    public String uid;
    public String username;
    public int age;
    public float activityLevel;
    public float activityGoal;

    User()
    {
        uid="";
        username="";
    }
    User(String uid, String username,int age)
    {
        this.uid=uid;
        this.username=username;
        this.age=age;
        activityLevel=0;
        activityGoal=20; //default value set, might change it
    }


}
