package com.specknet.pdiot;

public class User {

    public String uid;
    public String username;
    public int age;
    public float activityLevel;
    public int activityGoal;

    User()
    {
        uid="";
        username="";
        age=0;
        activityGoal=0;
        activityLevel=0;
    }
    User(String uid, String username,int age)
    {
        this.uid=uid;
        this.username=username;
        this.age=age;
        activityLevel=0;
        activityGoal=20; //default value set, might change it
    }
    public void editUser(String username,int age, int activityGoal)
    {
        this.username=username;
        this.age=age;
        this.activityGoal=activityGoal;
    }

}
