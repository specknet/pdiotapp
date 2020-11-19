package com.specknet.pdiot;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;

import androidx.core.app.NotificationCompat;

import com.specknet.pdiot.utils.Constants;

import static com.specknet.pdiot.AppNotify.CHANNEL_ID;

public class TrackService extends Service {
    private int counter=0;
    private Looper looper;
    private MovementQueue movementQueue;
    private BroadcastReceiver respeckLiveReceiver;
    private IntentFilter filterTest=new IntentFilter(Constants.ACTION_INNER_RESPECK_BROADCAST);
    public TrackService() {
    }

    @Override
    public void onCreate() {
        movementQueue=new MovementQueue(30);
           respeckLiveReceiver=new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                if(intent.getAction() == Constants.ACTION_INNER_RESPECK_BROADCAST)
                {
                    float x = intent.getFloatExtra(Constants.EXTRA_RESPECK_LIVE_X, 0f);
                    float y = intent.getFloatExtra(Constants.EXTRA_RESPECK_LIVE_Y, 0f);
                    float z = intent.getFloatExtra(Constants.EXTRA_RESPECK_LIVE_Z, 0f);
                    MovePoint nextPoint=new MovePoint(x,y,z);
                    movementQueue.AddMove(nextPoint);

                }

            }
        };
        HandlerThread handlerThread=new HandlerThread("bgThread");
        handlerThread.start();
        looper= handlerThread.getLooper();
        Handler handler=new Handler(looper);
        this.registerReceiver(respeckLiveReceiver,filterTest , null,handler);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String input=intent.getStringExtra("inputExtra");
        counter=1+intent.getIntExtra("counter",0);
        Intent notificationIntent= new Intent(this,MainActivity.class);
        PendingIntent pendingIntent=PendingIntent.getActivity(this,0,notificationIntent,0);
        Notification notification=new NotificationCompat.Builder(this,CHANNEL_ID)
                .setContentTitle("Movement Tracker")
                .setContentText(input)
                .setSmallIcon(R.drawable.android_black_24dp)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1,notification);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Intent restart=new Intent(this,TrackService.class);
        restart.putExtra("counter",counter);
        restart.putExtra("inputExtra","Welcome "+Integer.toString(counter)+" times");
        startService(restart);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }
}