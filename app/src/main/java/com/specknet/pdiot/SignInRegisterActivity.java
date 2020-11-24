package com.specknet.pdiot;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignInRegisterActivity extends AppCompatActivity {


    private Button registerButton;
    private TextView loginLink;
    private  Intent moveToLogin;
    private  Intent backToMain;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in_register);
        registerButton=findViewById(R.id.regButton);
        loginLink=findViewById(R.id.login_link);
        moveToLogin=new Intent(this, SignInLoginActivity.class);
        backToMain=new Intent(this,MainActivity.class);


        /* Setting register Button to use the registration function on the entered data */
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText userName=findViewById(R.id.editUserName);
                EditText email=findViewById(R.id.editEmailAddress);
                EditText password=findViewById(R.id.editPassword);
                EditText age=findViewById(R.id.editAge);
                registerUser(userName.getText().toString(),email.getText().toString(),password.getText().toString(),Integer.parseInt(age.getText().toString()));
            }
        });
        loginLink.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                startActivity(moveToLogin);
                finish();
            }
        });
    }


    private void registerUser(final String userName, String email, final String password,final int age)
    {
        if(userName.equals("") || email.equals("") || password.equals(""))
           return;
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email,password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>()
        {

            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful())
                {

                    Toast.makeText(SignInRegisterActivity.this, "Authentication successful!",
                            Toast.LENGTH_SHORT).show();
                    saveUser(userName,age);
                    startActivity(backToMain);
                    finish();
                }



            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(SignInRegisterActivity.this, "Authentication failed!"+e.getMessage(),
                        Toast.LENGTH_SHORT).show();
                Log.w("TAG",e.getMessage());

            }
        });
    }

    private void saveUser(String username,int age)
    {
        String uid=FirebaseAuth.getInstance().getUid();
        User newUser=new User(uid,username,age);
         DatabaseReference dataRef=FirebaseDatabase.getInstance().getReference("users/");
        dataRef.child(uid).setValue(newUser).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                Log.w("TAG","User added to database!");
            }
        });
    }
}