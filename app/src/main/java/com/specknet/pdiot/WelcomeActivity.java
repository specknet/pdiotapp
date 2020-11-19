package com.specknet.pdiot;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

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

public class WelcomeActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private TextView welcomeText;
    private Intent toApp;
    private Intent toProfile;

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
        FirebaseUser user = mAuth.getCurrentUser();
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


}