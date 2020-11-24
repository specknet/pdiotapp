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
import com.specknet.pdiot.R;

public class SignInLoginActivity extends AppCompatActivity {

    private TextView registerLink;
    private  Button loginButton;
    private  Intent moveToRegister;
    private  Intent backToMain;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in_login);
         registerLink=findViewById(R.id.reg_link);
         loginButton=findViewById(R.id.loginButton);
         moveToRegister=new Intent(this, SignInRegisterActivity.class);
         backToMain=new Intent(this, MainActivity.class);





        registerLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(moveToRegister);
                finish();
            }
        });
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText email=findViewById(R.id.loginEmail);
                EditText password=findViewById(R.id.loginPass);
                loginUser(email.getText().toString(),password.getText().toString());

            }
        });



    }

    private void loginUser(final String email, final String password)
    {
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email,password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                Toast.makeText(SignInLoginActivity.this, "Login successful!",
                        Toast.LENGTH_SHORT).show();
                startActivity(backToMain);
                finish();


            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(SignInLoginActivity.this, "Login failed!"+e.getMessage(),
                        Toast.LENGTH_LONG).show();
                Log.w("TAG",e.getMessage());
            }
        });

    }
}