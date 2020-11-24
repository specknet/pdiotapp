package com.specknet.pdiot;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;

import static com.specknet.pdiot.AppNotify.CHANNEL_ID;

public class LevelReminder extends BroadcastReceiver {


  private User user;
  private MovementData movementData;
  private NotificationManagerCompat notificationManager;
  private NotificationCompat.Builder builder;

    @Override
    public void onReceive(Context context, Intent intent) {
        String uid=intent.getStringExtra("userUID");
        Intent notificationIntent= new Intent(context,WelcomeActivity.class);
        PendingIntent pendingIntent=PendingIntent.getActivity(context,0,notificationIntent,0);
        builder= new NotificationCompat.Builder(context,CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_baseline_notifications_none_24)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        notificationManager=NotificationManagerCompat.from(context);
        getUserAndMove(uid);



    }

    private void getUserAndMove(final String uid) {
        DatabaseReference dataref= FirebaseDatabase.getInstance().getReference("/users/");
        dataref.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                user=snapshot.getValue(User.class);
                getMovesToday(uid);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void getMovesToday(String uid) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        String today = sdf.format(new Date());

        DatabaseReference dataref= FirebaseDatabase.getInstance().getReference("/movements/");
        dataref.child(uid).child(today).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                movementData=snapshot.getValue(MovementData.class);
                setupNotification();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void setupNotification() {
        String message="";
        if(movementData==null)
        {
            message="Start the recording service to measure your activity level today!";
        }
        else
        {
            float convertGoal=user.activityGoal/100.0f;
            if(movementData.ActivityLevel()<convertGoal)
            {
                message="Your activity level today is lower than your set goal! ";
            }
            else
                {
                    message="Good job! Today your active beyond your set goal!";
                }
        }
        builder.setContentTitle("Hello "+user.username);
        builder.setContentText(message);
        notificationManager.notify(200,builder.build());
    }
}