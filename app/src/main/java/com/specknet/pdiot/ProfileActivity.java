package com.specknet.pdiot;

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ProfileActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EditText editAge;
    private EditText editUsername;
    private EditText editLevel;
    private TextView todayLevel;
    private TextView averageLevel;
    private User userData;
    private Button restoreData;
    private Button editData;
    private SeekBar levelBar;
    private String today;
    private ProgressBar todayProgress;
    private ProgressBar averageProgress;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        today = sdf.format(new Date());
        editAge=findViewById(R.id.ageEdit);
        editUsername=findViewById(R.id.usernameEdit);
        editLevel=findViewById(R.id.activeEdit);
        todayLevel=findViewById(R.id.todayText);
        averageLevel=findViewById(R.id.averageText);
        disableEditText(editLevel);
        levelBar=findViewById(R.id.seekBarLevel);
        todayProgress=findViewById(R.id.todayProgressBar);
        averageProgress=findViewById(R.id.averageProgressBar);
        restoreData=findViewById(R.id.restore_button);
        editData=findViewById(R.id.edit_Button);
        mAuth=FirebaseAuth.getInstance();
        FirebaseUser curUser=mAuth.getCurrentUser(); //suppose that it cannot be null (then the previous activity would jump back to login)
        getUserInfo(curUser);
        restoreData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(userData!=null)
                {
                    updateProfileField(userData);
                }
            }
        });
        editData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int newAge=Integer.parseInt(editAge.getText().toString());
                int newGoal=Integer.parseInt(editLevel.getText().toString());
                String newUsername=editUsername.getText().toString();
                userData.editUser(newUsername,newAge,newGoal);
                saveUserData(userData);
                finish();
            }
        });
        levelBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                editLevel.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });



    }

    private void saveUserData(User userData) {

        DatabaseReference dataRef=FirebaseDatabase.getInstance().getReference("users/");
        dataRef.child(userData.uid).setValue(userData).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                Toast.makeText(ProfileActivity.this, "User data was modified!",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateProfileField(User profileInfo) {

        editUsername.setText(profileInfo.username);
        editLevel.setText(String.valueOf(profileInfo.activityGoal));
        editAge.setText(String.valueOf( profileInfo.age));
        levelBar.setProgress(profileInfo.activityGoal);


    }

    private void getUserInfo(FirebaseUser user)
    {
        final User[] userInfo = new User[1];
        String uid=user.getUid();
        DatabaseReference databaseRef= FirebaseDatabase.getInstance().getReference("/users/");
        databaseRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userData=snapshot.getValue(User.class);
                updateProfileField(userData);
                getTodayActivityLevel();
                getAverageActivityLevel();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private void disableEditText(EditText editText) {
        editText.setFocusable(false);
        editText.setEnabled(false);
        editText.setCursorVisible(false);
        editText.setKeyListener(null);
        editText.setBackgroundColor(Color.TRANSPARENT);
    }
    private void getAverageActivityLevel()
    {
        DatabaseReference dataref=FirebaseDatabase.getInstance().getReference("movements/");
        dataref.child(userData.uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int numofDays=0;
                float activitySum=0;
               for(DataSnapshot year:snapshot.getChildren())
               {
                   for(DataSnapshot month:year.getChildren())
                   {
                       for(DataSnapshot day:month.getChildren())
                       {
                           numofDays++;
                           MovementData dayData=day.getValue(MovementData.class);
                           activitySum+=dayData.ActivityLevel();

                       }
                   }
               }
               if(numofDays==0) //to avoid 0 division
                   numofDays++;
               activitySum/=numofDays;
               updateAverageScore(activitySum);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void updateAverageScore(float activitySum) {
        DecimalFormat decFormat= new DecimalFormat("0");
        String message="Current average activity level: "+decFormat.format(activitySum*100);
        averageLevel.setText(message);
        averageProgress.setProgress(Math.round(activitySum*100));
    }

    private void getTodayActivityLevel()
    {
        DatabaseReference dataref=FirebaseDatabase.getInstance().getReference("movements/");
        dataref.child(userData.uid).child(today).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                MovementData todayScore=snapshot.getValue(MovementData.class);
                updateTodayScore(todayScore);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void updateTodayScore(MovementData todayscore) {
        String message="";
        if(todayscore!=null)
        {
            DecimalFormat decFormat= new DecimalFormat("0");
             message="Today's activity level: "+decFormat.format(todayscore.ActivityLevel()*100);
             todayProgress.setProgress(Math.round(todayscore.ActivityLevel()*100));
        }
        else
            {
                 message="Record your activity to measure your activity level!";
                 todayProgress.setProgress(0);
            }
        todayLevel.setText(message);
    }
}
