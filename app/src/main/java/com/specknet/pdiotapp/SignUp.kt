package com.specknet.pdiotapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.specknet.pdiotapp.sql.DBHelper
import kotlinx.android.synthetic.main.activity_sign_up.regConfirmPass

class SignUp : AppCompatActivity() {
    private lateinit var registerButton: Button
    private lateinit var goToLoginButton: TextView

    private lateinit var username: EditText
    private lateinit var password: EditText
    private lateinit var confirmPassword: EditText

    private lateinit var db: DBHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        registerButton = findViewById(R.id.signupbutton)
        goToLoginButton = findViewById(R.id.loginLink)
        username = findViewById(R.id.regUsername)
        password = findViewById(R.id.regPass)
        confirmPassword = findViewById(R.id.regConfirmPass)
        db = DBHelper(this)

        setUpClickListeners()
    }

    private fun setUpClickListeners() {
        registerButton.setOnClickListener {
            val user = username.text.toString()
            val pass = password.text.toString()
            val confirmPass = confirmPassword.text.toString()

            if (user == "") {
                Toast.makeText(this, "Please enter a valid username", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (pass == "" || pass != confirmPass) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userExists = db.checkUsername(user)
            if (userExists) {
                Toast.makeText(this, "User already exists", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            } else {
                val success = db.addUser(user, pass)
                if (success) {
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    Toast.makeText(this, "User registered successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Registration failed", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }
        }

        goToLoginButton.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }
}