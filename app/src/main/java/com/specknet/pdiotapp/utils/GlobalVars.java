package com.specknet.pdiotapp.utils;

import com.google.firebase.database.DatabaseReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class GlobalVars {
    // Setup global user information
    public static String accName = "";
    public static boolean loggedIn = false;
    public static String accId = "";

    // Setup current date + formatting for any future use of dates
    private static Date curDateObject = new Date();
    public static SimpleDateFormat dateFormatter = new SimpleDateFormat("ddMMyy");
    public static String curDateStr = dateFormatter.format(curDateObject);

    // Setup database connections
    public static DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("UserData");

    public static ArrayList<String> basicActivities = new ArrayList<String>() {
        {
            add("Sitting");
            add("Walking");
            add("Lying Down");
            add("Running");
        }
    };

    public static ArrayList<String> complexActivities = new ArrayList<String>() {
        {
            add("Sitting");
            add("Walking");
            add("Walking up stairs");
            add("Walking down stairs");
            add("Lying Down on Front");
            add("Lying Down on Left");
            add("Lying Down on Right");
            add("Lying Down on Back");
            add("Running");
            // TODO: add the rest of the activities
        }
    };


}
