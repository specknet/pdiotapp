package com.specknet.pdiotapp.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.specknet.pdiotapp.bluetooth.BluetoothService;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Utils {

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    private static int last_seq_number = -1;

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static void processRESpeckPacket(final byte[] values, int respeckVersion, BluetoothService bltService) {
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
        }

        for (int i = 8; i < values.length; i += 6) {
            final float x = combineAccelerationBytes(values[i + 0], values[i + 1]);
            final float y = combineAccelerationBytes(values[i + 2], values[i + 3]);
            final float z = combineAccelerationBytes(values[i + 4], values[i + 5]);

            Log.i("Debug", "(x = " + x + ", y = " + y + ", z = " + z + ")");

            Intent liveDataIntent = new Intent(Constants.ACTION_INNER_RESPECK_BROADCAST);
            liveDataIntent.putExtra(Constants.EXTRA_RESPECK_LIVE_X, x);
            liveDataIntent.putExtra(Constants.EXTRA_RESPECK_LIVE_Y, y);
            liveDataIntent.putExtra(Constants.EXTRA_RESPECK_LIVE_Z, z);
            liveDataIntent.putExtra(Constants.EXTRA_RESPECK_PACKET_SEQ, last_seq_number);

            bltService.sendBroadcast(liveDataIntent);
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



}
