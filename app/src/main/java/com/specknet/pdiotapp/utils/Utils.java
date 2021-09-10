package com.specknet.pdiotapp.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.specknet.pdiotapp.bluetooth.BluetoothSpeckService;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class Utils {

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    private static int last_seq_number = -1;
    private static long lastProcessedMinute = -1;

    private static long mPhoneTimestampCurrentPacketReceived = -1;
    private static long mPhoneTimestampLastPacketReceived = -1;
    private static int currentSequenceNumberInBatch = -1;

    private static ArrayList<Long> frequencyTimestampsRespeck = new ArrayList<>();
    private static ArrayList<Long> frequencyTimestampsPhone = new ArrayList<>();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static float qToFloat(Byte upper, Byte lower) {
        short unsigned_lower = (short) (lower & 0xFF);
        short value = (short) ((upper << 8) | unsigned_lower);
        return  (((float)value) / 1024);
    }

    public static String getRESpeckUUID(Context context) {
        // this will always exist because the service is going to be started only after the respeck has been scanned\

        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.PREFERENCES_FILE, Context.MODE_PRIVATE);

        return sharedPreferences.getString(Constants.RESPECK_MAC_ADDRESS_PREF, "");
    }

    public static String getThingyUUID(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.PREFERENCES_FILE, Context.MODE_PRIVATE);

        return sharedPreferences.getString(Constants.THINGY_MAC_ADDRESS_PREF, "");
    }

    public static int getRESpeckVersion(Context context) {
        // this will always exist because the service is going to be started only after the respeck has been scanned\

        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.PREFERENCES_FILE, Context.MODE_PRIVATE);

        return sharedPreferences.getInt(Constants.RESPECK_VERSION, 0);
    }

    public static void processRESpeckPacket(final byte[] values, int respeckVersion, BluetoothSpeckService bltService) {

        long currentProcessedMinute = 0;

        if(respeckVersion == 5) {
            byte[] time_array = {values[0], values[1], values[2], values[3]};
            // and try ByteBuffer:
            ByteBuffer buffer = ByteBuffer.wrap(time_array);
            buffer.order(ByteOrder.BIG_ENDIAN);
            buffer.position(0);
            long uncorrectedRESpeckTimestamp = ((long) buffer.getInt()) & 0xffffffffL;

            long newRESpeckTimestamp = uncorrectedRESpeckTimestamp * 197 * 1000 / 32768;
        }

        else if(respeckVersion == 6) {
            //get the respeck timestamp
            ByteBuffer buffer = ByteBuffer.wrap(values);
            buffer.order(ByteOrder.BIG_ENDIAN);
            buffer.position(0);

            long uncorrectedRESpeckTimestamp = ((long) buffer.getInt()) & 0xffffffffL;
            long newRESpeckTimestamp = uncorrectedRESpeckTimestamp * 197 * 1000 / 32768;
            Log.i("RESpeckPacketHandler", "Respeck timestamp (ms): " + Long.toString(newRESpeckTimestamp));
            frequencyTimestampsRespeck.add(newRESpeckTimestamp);

            // get the packet sequence number.
            // This counts from zero when the respeck is reset and is a uint32 value,
            // so we'll all be long dead by the time it wraps!

            int seqNumber  = ((int)buffer.getShort()) & 0xffff;
            Log.i("RESpeckPacketHandler", "Respeck seq number: " + Integer.toString(seqNumber));

            if (last_seq_number >= 0 && seqNumber - last_seq_number != 1) {
                // have we just wrapped?
                if (seqNumber == 0 && last_seq_number == 65535) {
                    Log.w("RESpeckPacketHandler", "Respeck seq number wrapped");
                }
                else {
                    Log.w("RESpeckPacketHandler", "Unexpected respeck seq number. Expected: " + Long.toString(last_seq_number + 1) + ", received: " + Long.toString(seqNumber));
                }
            }

            if (last_seq_number >=0 && last_seq_number == seqNumber) {
                Log.e("RESpeckPacketHandler", "DUPLICATE SEQ");
            }
            last_seq_number = seqNumber;


            // Read battery level and charging status
            byte battLevel  = values[6];
            Log.i("RESpeckPacketHandler", "Respeck battery level: " + Byte.toString(battLevel) + "%");

            boolean chargingStatus = false;
            if (values[7] == (byte)0x01) chargingStatus = true;
            Log.i("RESpeckPacketHandler", "Respeck charging?: " + Boolean.toString(chargingStatus));

            final long actualPhoneTimestamp = System.currentTimeMillis();

            if (mPhoneTimestampCurrentPacketReceived == -1 || mPhoneTimestampCurrentPacketReceived + 2.5 * Constants.AVERAGE_TIME_DIFFERENCE_BETWEEN_RESPECK_PACKETS < actualPhoneTimestamp) {
                mPhoneTimestampLastPacketReceived = actualPhoneTimestamp - Constants.AVERAGE_TIME_DIFFERENCE_BETWEEN_RESPECK_PACKETS;
            }
            else {
                mPhoneTimestampLastPacketReceived = mPhoneTimestampCurrentPacketReceived;
            }

            long extrapolatedPhoneTimestamp = mPhoneTimestampLastPacketReceived + Constants.AVERAGE_TIME_DIFFERENCE_BETWEEN_RESPECK_PACKETS;

            if(Math.abs(
                    extrapolatedPhoneTimestamp - actualPhoneTimestamp) > Constants.MAXIMUM_MILLISECONDS_DEVIATION_ACTUAL_AND_CORRECTED_TIMESTAMP) {
                mPhoneTimestampCurrentPacketReceived = actualPhoneTimestamp;
            }
            else {
                mPhoneTimestampCurrentPacketReceived = extrapolatedPhoneTimestamp;
            }

        }

        currentSequenceNumberInBatch = 0;

        for (int i = 8; i < values.length; i += 6) {

            long now = System.currentTimeMillis();
            frequencyTimestampsPhone.add(now);

            currentProcessedMinute = TimeUnit.MILLISECONDS.toMinutes(now);
            Log.i("Debug", "current min = " + currentProcessedMinute);

            if(currentProcessedMinute != lastProcessedMinute && lastProcessedMinute != -1) {
                float currentRespeckFreq = calculateRespeckFrequency();
                Log.i("Debug", "current freq = " + currentRespeckFreq);

                float currentPhoneFreq = calculatePhoneFrequency();
                Log.i("Debug", "current freq = " + currentPhoneFreq);
            }

            long interpolatedPhoneTimestamp = (long) ((mPhoneTimestampCurrentPacketReceived - mPhoneTimestampLastPacketReceived) *
                    (currentSequenceNumberInBatch * 1. / Constants.NUMBER_OF_SAMPLES_PER_BATCH)) + mPhoneTimestampLastPacketReceived;

            final float x = combineAccelerationBytes(values[i + 0], values[i + 1]);
            final float y = combineAccelerationBytes(values[i + 2], values[i + 3]);
            final float z = combineAccelerationBytes(values[i + 4], values[i + 5]);

            Log.i("Debug", "(x = " + x + ", y = " + y + ", z = " + z + ")");

            Intent liveDataIntent = new Intent(Constants.ACTION_INNER_RESPECK_BROADCAST);
            liveDataIntent.putExtra(Constants.EXTRA_RESPECK_LIVE_X, x);
            liveDataIntent.putExtra(Constants.EXTRA_RESPECK_LIVE_Y, y);
            liveDataIntent.putExtra(Constants.EXTRA_RESPECK_LIVE_Z, z);
            liveDataIntent.putExtra(Constants.EXTRA_INTERPOLATED_TS, interpolatedPhoneTimestamp);

            bltService.sendBroadcast(liveDataIntent);

            lastProcessedMinute = currentProcessedMinute;
            currentSequenceNumberInBatch += 1;
        }



    }

    private static float combineAccelerationBytes(Byte upper, Byte lower) {
        short unsigned_lower = (short) (lower & 0xFF);
        short value = (short) ((upper << 8) | unsigned_lower);
        return (value) / 16384.0f;
    }

    public static long getUnixTimestamp() {
        return System.currentTimeMillis();
    }

    public static boolean isServiceRunning(Class<?> serviceClass, Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        for(ActivityManager.RunningServiceInfo service: manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }

        return false;
    }

    public static float calculateRespeckFrequency() {
        int num_freq = frequencyTimestampsRespeck.size();

        if(num_freq <= 1) {
            return 0;
        }

        long first_ts = frequencyTimestampsRespeck.get(0);
        long last_ts = frequencyTimestampsRespeck.get(num_freq - 1);

        float samplingFreq = ((num_freq * 1.f) / (last_ts - first_ts)) * 1000.f;
        Log.i("Debug", "samplingFrequencyRespeck = " + samplingFreq);

        frequencyTimestampsRespeck.clear();

        return samplingFreq;
    }

    public static float calculatePhoneFrequency() {
        int num_freq = frequencyTimestampsPhone.size();

        if(num_freq <= 1) {
            return 0;
        }

        long first_ts = frequencyTimestampsPhone.get(0);
        long last_ts = frequencyTimestampsPhone.get(num_freq - 1);

        float samplingFreq = ((num_freq * 1.f) / (last_ts - first_ts)) * 1000.f;
        Log.i("Debug", "samplingFrequencyPhone = " + samplingFreq);

        frequencyTimestampsPhone.clear();

        return samplingFreq;
    }

    public static String getStackTraceAsString(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    public static String bytesToHexNfc(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2 + bytes.length];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[hexChars.length - 3 - j * 3] = HEX_ARRAY[v >>> 4];
            hexChars[hexChars.length - 2 - j * 3] = HEX_ARRAY[v & 0x0F];
            hexChars[hexChars.length - 1 - j * 3] = ':';
        }
        return new String(Arrays.copyOfRange(hexChars,0,hexChars.length - 1));
    }

    public static int unsignedByteToInt(byte b) {
        return b & 0xFF;
    }

    /**
     * Convert signed bytes to a 16-bit unsigned int.
     */
    public static int unsignedBytesToInt(byte b0, byte b1) {
        return (unsignedByteToInt(b0) + (unsignedByteToInt(b1) << 8));
    }

    public static int unsignedToSigned(int unsigned, int size) {
        if ((unsigned & (1 << size - 1)) != 0) {
            unsigned = -1 * ((1 << size - 1) - (unsigned & ((1 << size - 1) - 1)));
        }
        return unsigned;
    }

    public static Integer getIntValue(byte[] mValue, int offset) {

        return unsignedToSigned(unsignedBytesToInt(mValue[offset],
                mValue[offset + 1]), 16);

    }

    public static float[] decodeThingyPacket(byte[] values) {
        float accel_x = (float) (getIntValue(values, 0)) / (1 << 10);
        float accel_y = (float) (getIntValue(values, 2)) / (1 << 10);
        float accel_z = (float) (getIntValue(values, 4)) / (1 << 10);

        float gyro_x = (float) (getIntValue(values, 6)) / (1 << 5);
        float gyro_y = (float) (getIntValue(values, 8)) / (1 << 5);
        float gyro_z = (float) (getIntValue(values, 10)) / (1 << 5);

        float mag_x = (float) (getIntValue(values, 12)) / (1 << 4);
        float mag_y = (float) (getIntValue(values, 14)) / (1 << 4);
        float mag_z = (float) (getIntValue(values, 16)) / (1 << 4);

        float[] decoded = new float[]{accel_x, accel_y, accel_z, gyro_x, gyro_y, gyro_z, mag_x, mag_y, mag_z};

        return decoded;
    }

}
