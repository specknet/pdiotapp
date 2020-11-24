package com.specknet.pdiot;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions;
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager;
import com.google.firebase.ml.custom.FirebaseCustomRemoteModel;
import com.specknet.pdiot.utils.Constants;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.specknet.pdiot.AppNotify.CHANNEL_ID;

public class TrackService extends Service {

    private BroadcastReceiver respeckLiveReceiver;
    private Looper looper;
    private CountDownTimer timer;

    private MovementQueue movementQueue;
    private String today;

    private final IntentFilter filterTest=new IntentFilter(Constants.ACTION_INNER_RESPECK_BROADCAST);
    private Interpreter interpreter;
    private String[] classLabels;
    private Map<String, Long> movementTimes;
    private Map<String, Integer> minuteActions;
    private int minutes=0;
    private final int savePerMin=10;
    private MovementData saveData;


    private boolean disconnect=false;


    public TrackService() {
    }

    @Override
    public void onCreate() {
        filterTest.addAction(Constants.ACTION_RESPECK_DISCONNECTED);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        today = sdf.format(new Date());
        Toast.makeText(this, "Service launched!", Toast.LENGTH_SHORT).show();
        movementQueue=new MovementQueue(36);
        classLabels=getClassLabels("model_class.txt",6);
        movementTimes=new HashMap<String,Long>();
        minuteActions=new HashMap<String, Integer>();

        FirebaseCustomRemoteModel remoteModel =
                new FirebaseCustomRemoteModel.Builder("Movement_Classifier").build();
        FirebaseModelDownloadConditions conditions = new FirebaseModelDownloadConditions.Builder()
                .requireWifi()
                .build();
        FirebaseModelManager.getInstance().download(remoteModel, conditions)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void v) {
                        // Download complete. Depending on your app, you could enable
                        // the ML feature, or switch from the local model to the remote
                        // model, etc.
                    }
                });
        FirebaseModelManager.getInstance().getLatestModelFile(remoteModel)
                .addOnCompleteListener(new OnCompleteListener<File>() {
                    @Override
                    public void onComplete(@NonNull Task<File> task) {
                        File modelFile = task.getResult();
                        if (modelFile != null) {
                            interpreter = new Interpreter(modelFile);
                            startReciever();
                        }
                    }
                });

        // File not found?

        timer=new CountDownTimer(60000, 1000)
        {

            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                //onActivityChange(lastActivity,System.currentTimeMillis()/1000);
                //String dom=dominantAction();
               // if(dom!="")
                //{
                 //   addActionToMinutes(dom);
                //}
                addMovesToSave();
                this.start();

            }
        };
        timer.start();
    }

    private void addMovesToSave() {

        long sumTime=0;

        for(String key:movementTimes.keySet())
        {
            sumTime+=movementTimes.get(key);
        }
        int remainderTime=0;
        for(String key:movementTimes.keySet())
        {
            float percentile=movementTimes.get(key)/(float)sumTime;
            if(percentile>5/60) //if 5 seconds of a motion is in a minute then it's probably a relevant move
            {
                int addseconds=Math.round(percentile*60);
                remainderTime+=addseconds;
                if(minuteActions.containsKey(key))
                {
                    int seconds=minuteActions.get(key);
                    minuteActions.put(key,seconds+addseconds);
                }
                else
                {
                    minuteActions.put(key,addseconds);
                }
            }
        }
        if(remainderTime<60)
        {
            minuteActions.put("transition",60-remainderTime);
        }
        movementTimes.clear();
        minutes++;
        if(minutes>=savePerMin)
        {
            saveActionToDataBase();
        }
    }

    private void addActionToMinutes(String dom) {
        if(minuteActions.containsKey(dom))
        {
            int minutes=minuteActions.get(dom);
            minuteActions.put(dom,minutes+1);
        }
        else
            {
                minuteActions.put(dom,1);
            }
        minutes++;
        if(minutes>=savePerMin)
        {
            saveActionToDataBase();
        }
    }

    private void saveActionToDataBase() {

        saveData=new MovementData(minuteActions);
        String uid=FirebaseAuth.getInstance().getCurrentUser().getUid();
        updateTodayRecord(uid);
        minutes=0;
        minuteActions.clear();
    }
    private void updateTodayRecord(final String uid)
    {
        DatabaseReference databaseRef= FirebaseDatabase.getInstance().getReference("/movements/");
        databaseRef.child(uid).child(today).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
               if(snapshot.getValue()!=null) //if data couldn't be found, we skip addition with null data to avoid crash, current sessions data is saved.
                saveData.AddMovementData(snapshot.getValue(MovementData.class));
                editMoveDataBase(uid);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w("TAG", "loadPost:onCancelled", error.toException());
                editMoveDataBase(uid);

            }

        });
    }
    private void editMoveDataBase(String uid)
    {
        DatabaseReference dataRef=FirebaseDatabase.getInstance().getReference("movements/");
        dataRef.child(uid).child(today).setValue(saveData).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                Log.w("TAG","User added to database!");
            }
        });
    }


    /*private void onActivityChange(String newlabel, long timestamp) {
        long newTime=(timestamp-lastTimeStamp);
        if(newTime==0)
        {
            return;
        }
        Log.i("Change","Activity "+lastActivity+" lasted: "+newTime+" seconds.");
        if(movementTimes.containsKey(lastActivity))
          {
              newTime= movementTimes.get(lastActivity)+(timestamp-lastTimeStamp);
              movementTimes.put(lastActivity,newTime);
          }
        else
            {
                movementTimes.put(lastActivity,newTime);
            }
        lastActivity=newlabel;
        lastTimeStamp=timestamp;
    }*/
    private void addClassifiedMove(String move)
    {
        if(movementTimes.containsKey(move))
        {
            long existingTime=movementTimes.get(move);
            movementTimes.put(move,existingTime+1);
        }
        else
            {
                movementTimes.put(move, (long) 1);
            }
    }

    private String[] getClassLabels(String filename,int numLabels) {
        String[] classes=new String[numLabels];
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(getAssets().open(filename)));
            for(int i=0; i<numLabels; i++)
            {
                classes[i]=reader.readLine();
            }
        } catch (IOException e) {

        }
        return classes;

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String input=intent.getStringExtra("inputExtra");
        Intent notificationIntent= new Intent(this,MainActivity.class);
        PendingIntent pendingIntent=PendingIntent.getActivity(this,0,notificationIntent,0);
        Notification notification=new NotificationCompat.Builder(this,CHANNEL_ID)
                .setContentTitle("Movement Tracker")
                .setContentText(input)
                .setSmallIcon(R.drawable.ic_recording_icon)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1,notification);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(disconnect)
        Toast.makeText(this, "Respeck disconnected!", Toast.LENGTH_SHORT).show();
        saveActionToDataBase();
        timer.cancel();
        unregisterReceiver(respeckLiveReceiver);
        looper.quit();



    }


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    private String FindLabel(FloatBuffer probabilities)
    {
        float maxprob=0.0f;
        int maxIndex=-1;
        for(int i=0; i<probabilities.capacity(); i++)
        {
            if(probabilities.get(i)>maxprob)
            {
                maxprob=probabilities.get(i);
                maxIndex=i;
            }
        }
        return classLabels[maxIndex];

    }
    private String dominantAction()
    {
        long max=0;
        String maxKey="";
        for(String key:movementTimes.keySet())
        {
            if(movementTimes.get(key)>max)
            {
                max=movementTimes.get(key);
                maxKey=key;
            }
        }
        return maxKey;
    }
    private void startReciever()
    {
         respeckLiveReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                if (intent.getAction().equals(Constants.ACTION_INNER_RESPECK_BROADCAST)) {
                    float x = intent.getFloatExtra(Constants.EXTRA_RESPECK_LIVE_X, 0f);
                    float y = intent.getFloatExtra(Constants.EXTRA_RESPECK_LIVE_Y, 0f);
                    float z = intent.getFloatExtra(Constants.EXTRA_RESPECK_LIVE_Z, 0f);
                    MovePoint nextPoint = new MovePoint(x, y, z);
                    movementQueue.AddMove(nextPoint);
                    if (movementQueue.isFull()) {
                        ByteBuffer input = movementQueue.ConvertDataToBuffer();
                        int bufferSize = 12 * Float.SIZE / Byte.SIZE;
                        ByteBuffer modelOutput = ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.nativeOrder());
                        interpreter.run(input, modelOutput);
                        modelOutput.rewind();
                        FloatBuffer probabilities = modelOutput.asFloatBuffer();
                        String curLabel = FindLabel(probabilities);
                        Log.i("Label:", String.format("Current activity: %s", curLabel));
                        addClassifiedMove(curLabel);
                        /*if (curLabel != lastActivity) {
                            if (lastActivity == "") {
                                lastActivity = curLabel;
                                lastTimeStamp = System.currentTimeMillis() / 1000;
                            } else {
                                onActivityChange(curLabel, System.currentTimeMillis() / 1000);
                            }
                        }*/



                        // File not found?

                    }

                }
                if(intent.getAction()==Constants.ACTION_RESPECK_DISCONNECTED)
                {
                    stopService();
                }


            }
        };
        HandlerThread handlerThread=new HandlerThread("bgThread");
        handlerThread.start();
        looper = handlerThread.getLooper();
        Handler handler=new Handler(looper);
        this.registerReceiver(respeckLiveReceiver,filterTest , null,handler);
    }
    private void stopService()
    {
        disconnect=true;
        this.stopSelf();
    }

}