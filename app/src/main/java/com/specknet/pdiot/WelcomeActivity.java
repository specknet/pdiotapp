package com.specknet.pdiot;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import static com.specknet.pdiot.AppNotify.CHANNEL_ID;

public class WelcomeActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private TextView welcomeText;
    private Intent toApp;
    private Intent toProfile;
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        welcomeText=findViewById(R.id.welcomeText);
        Button connectButton = findViewById(R.id.connectMenuButton);
        Button profileButton = findViewById(R.id.profile_button);
        toApp=new Intent(this,MainActivity.class);
        toProfile=new Intent(this,ProfileActivity.class);
        mAuth=FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        if(user ==null)
        {
            Intent intent = new Intent (this, SignInRegisterActivity.class);
            startActivity(intent);
            finish();
        }
        else
            {
                updateUI(user);
            }
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(toApp);
            }
        });
        profileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(toProfile);
            }
        });

        Intent notifyIntent=new Intent(this,LevelReminder.class);
        notifyIntent.putExtra("userUID",user.getUid());
        PendingIntent pendingIntent=PendingIntent.getBroadcast(this,0,notifyIntent,0);

        AlarmManager alarmManager= (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP,System.currentTimeMillis(),AlarmManager.INTERVAL_HOUR*4,pendingIntent);

    }

    private void updateUI(FirebaseUser user) {
        String uid=user.getUid();
        DatabaseReference databaseRef= FirebaseDatabase.getInstance().getReference("/users/");
        databaseRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User userData=snapshot.getValue(User.class);
                welcomeText.setText("Welcome "+userData.username);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
    @Override
    public void onResume() {

        super.onResume();
        updateUI(user);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_welcome,menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int itemId=item.getItemId();
        switch (itemId)
        {
            case R.id.sign_out:
                signOut();
                break;
         /*   case R.id.test_service:
                startService();
                break;
            case R.id.test_stop:
                stopService();
                break;*/
            default:
        }
        return super.onOptionsItemSelected(item);

    }

    private void signOut() {
        mAuth.signOut();
        Intent intent = new Intent (this, SignInRegisterActivity.class);
        startActivity(intent);
        finish();

    }
    private void startService()
    {
        String input = "Keep service running to record your actions!"; //will change it just testing

        Intent serviceIntent = new Intent(this,TrackService.class);
        serviceIntent.putExtra("inputExtra",input);
        startService(serviceIntent);
    }

    private void stopService()
    {
        Intent serviceIntent=new Intent(this,TrackService.class);
        stopService(serviceIntent);

    }


}