package com.specknet.pdiotapp.utils;

import android.net.Uri;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Constants {
    // Respeck extras
    public static final int NUMBER_OF_SAMPLES_PER_BATCH = 32;
    public static final float SAMPLING_FREQUENCY = 12.7f;
    public static final int AVERAGE_TIME_DIFFERENCE_BETWEEN_RESPECK_PACKETS = (int) Math.round(
            NUMBER_OF_SAMPLES_PER_BATCH / SAMPLING_FREQUENCY * 1000.);
    public static final int MAXIMUM_MILLISECONDS_DEVIATION_ACTUAL_AND_CORRECTED_TIMESTAMP = 400;

    public static final String ACTION_RESPECK_CONNECTED = "com.specknet.pdiotapp.RESPECK_CONNECTED";
    public static final String ACTION_RESPECK_DISCONNECTED = "com.specknet.pdiotapp.RESPECK_DISCONNECTED";

    public static final String PREFERENCES_FILE = "com.specknet.pdiotapp.PREFERENCE_FILE";
    public static final String RESPECK_MAC_ADDRESS_PREF = "respeck_id_pref";
    public static final String RESPECK_VERSION = "respeck_version";
    public static final String THINGY_MAC_ADDRESS_PREF = "thingy_id_pref";

    //The REQUEST_ENABLE_BT constant passed to startActivityForResult(android.content.Intent, int)
    // is a locally-defined integer (which must be greater than 0) that the system passes back
    // to you in your onActivityResult(int, int, android.content.Intent) implementation as the requestCode parameter.

    public static final int REQUEST_CODE_PERMISSIONS = 4;

    public static final String RECORDING_CSV_HEADER_RESPECK = "timestamp,accel_x,accel_y,accel_z,gyro_x,gyro_y,gyro_z";
    public static final String RECORDING_CSV_HEADER_THINGY = "timestamp,accel_x,accel_y,accel_z,gyro_x,gyro_y,gyro_z,mag_x,mag_y,mag_z";

    // activity mappings
    public static final Map<String, Integer> ACTIVITY_NAME_TO_CODE_MAPPING = new HashMap<String, Integer>() {{
        put("Sitting", 0);
        put("Sitting bent forward", 4);
        put("Sitting bent backward", 5);
        put("Standing", 100);
        put("Walking at normal speed", 1);
        put("Lying down on back", 2);
        put("Lying down left", 7);
        put("Lying down right", 6);
        put("Lying down on stomach", 8);
        put("Movement", 9);
        put("Walking slow", 10);
        put("Running", 11);
        put("Climbing stairs", 12);
        put("Descending stairs", 13);
        put("Riding a bike", 14);
        put("Sit to stand (PR)", 21);
        put("Knee extension (PR)", 22);
        put("Squats (PR)", 23);
        put("Heel raises (PR)", 24);
        put("Bicep curls (PR)", 25);
        put("Shoulder press (PR)", 26);
        put("Wall push offs (PR)", 27);
        put("Leg slides (PR)", 28);
        put("Step ups", 29);
        put("Driving in car or bus", 30);
        put("Desk work", 31);
        put("Falling on knees", 48);
        put("Falling on the left", 45);
        put("Falling on the right", 46);
        put("Falling on the back", 47);
    }};

    public static final Map<Integer, String> ACTIVITY_CODE_TO_NAME_MAPPING = new HashMap<Integer, String>() {{
        put(0, "Sitting");
        put(4, "Sitting bent forward");
        put(5, "Sitting bent backward");
        put(1, "Walking at normal speed");
        put(2, "Lying down on back");
        put(7, "Lying down left");
        put(6, "Lying down right");
        put(8, "Lying down on stomach");
        put(9, "Movement");
        put(10, "Walking slow");
        put(11, "Running");
        put(12, "Climbing stairs");
        put(13, "Descending stairs");
        put(14, "Riding a bike");
        put(21, "Sit to stand (PR)");
        put(22, "Knee extension (PR)");
        put(23, "Squats (PR)");
        put(24, "Heel raises (PR)");
        put(25, "Bicep curls (PR)");
        put(26, "Shoulder press (PR)");
        put(27, "Wall push offs (PR)");
        put(28, "Leg slides (PR)");
        put(29, "Step ups");
        put(30, "Driving in car or bus");
        put(31, "Desk work");
        put(100, "Standing");
        put(48, "Falling on knees");
        put(45, "Falling on the left");
        put(46, "Falling on the right");
        put(47, "Falling on the back");
    }};



    public static final String PREF_USER_FIRST_TIME = "user_first_time";

    // Broadcast strings
    public static final String ACTION_RESPECK_RECORDING_PAUSE = "com.specknet.respeck.ACTION_RESPECK_RECORDING_PAUSE";
    public static final String ACTION_RESPECK_RECORDING_CONTINUE = "com.specknet.respeck.ACTION_RESPECK_RECORDING_CONTINUE";
    public static final String RESPECK_USE_IMU_CHARACTERISTIC = "respeck_char_imu";
    public static final String ACTION_SPECK_BLUETOOTH_SERVICE_SCAN_DEVICES = "com.specknet.airrespeck.ACTION_SPECK_BLUETOOTH_SERVICE_SCAN_DEVICES";


    public final static String RESPECK_LIVE_CHARACTERISTIC = "00002010-0000-1000-8000-00805f9b34fb";
    public final static String RESPECK_LIVE_V4_CHARACTERISTIC = "00001524-1212-efde-1523-785feabcd125";
    // https://github.com/specknet/respeckmodeltesting/blob/two_characteristics/app/src/main/java/com/specknet/respeckmodeltesting/utils/Constants.java#L60
    public final static String RESPECK_IMU_CHARACTERISTIC_UUID = "00001527-1212-efde-1523-785feabcd125"; // accel + gyro + mag

    // Bluetooth connection timeout: how long to wait after loosing connection before trying reconnect
    public static final int RECONNECTION_TIMEOUT_MILLIS = 10000;
    public static final long RESPECK_CHARACTERISTIC_CHANGE_TIMEOUT_MS = 4000; // 4 seconds
    public static final String CSV_DELIMITER = ","; // yes it's just a comma :)

    // Information for config content provider
    public static class Config {
        public static final String RESPECK_UUID = "RESpeckUUID";
        public static final String THINGY_UUID = "ThingyUUID";
    }

    public static final String RESPECK_DATA_DIRECTORY_NAME = "/RESpeck/";
    public static final String RESPECK_IMU_DATA_DIRECTORY_NAME = "/RESpeck-IMU/";
    public static final long NUMBER_OF_MILLIS_IN_A_DAY = 1000 * 60 * 60 * 24;
    public static final String RESPECK_LIVE_DATA = "respeck_live_data";
    public static final String ACTION_RESPECK_LIVE_BROADCAST =
            "com.specknet.respeck.RESPECK_LIVE_BROADCAST";
    public static final float MINUTES_FOR_MEDIAN_CALC = 500;
    public static final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.UK);

    public static final String THINGY_MOTION_CHARACTERISTIC = "ef680406-9b35-4933-9b10-52ffa9740042";

    public static final String ACTION_THINGY_BROADCAST = "com.specknet.pdiotapp.THINGY_BROADCAST";
    public static final String ACTION_THINGY_CONNECTED = "com.specknet.pdiotapp.THINGY_CONNECTED";
    public static final String ACTION_THINGY_DISCONNECTED = "com.specknet.pdiotapp.THINGY_DISCONNECTED";
    public static final String THINGY_LIVE_DATA = "thingy_live_data";



}
