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
