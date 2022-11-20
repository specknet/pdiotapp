package com.specknet.pdiotapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.content.Intent
import android.util.Log
import android.widget.TextView
import androidx.core.view.isVisible

import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.MutableData
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue

import com.specknet.pdiotapp.utils.GlobalVars
import kotlinx.android.synthetic.main.activity_login.*

class Login : AppCompatActivity() {

    lateinit var gso : GoogleSignInOptions;
    lateinit var gsc : GoogleSignInClient;
    lateinit var gsa : GoogleSignInAccount;
    lateinit var googleLoginButton : SignInButton;
    lateinit var signOutButton : Button;
    lateinit var deleteDataButton : Button;

    var RC_SIGN_IN = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)

        // setup for getting google account
        gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestId()
            .build();

        gsc = GoogleSignIn.getClient(this, gso)

        // login
        googleLoginButton = findViewById(R.id.sign_in_button)
        googleLoginButton.setOnClickListener {
            val signInIntent: Intent = gsc.getSignInIntent()
            startActivityForResult(signInIntent, RC_SIGN_IN);
        }

        // log out
        signOutButton = findViewById(R.id.sign_out_button)
        sign_out_button.setOnClickListener {
            handleSignOut()
        }

        // log out + delete all user data
        deleteDataButton = findViewById<Button>(R.id.delete_data_button)
        deleteDataButton.setOnClickListener {
            GlobalVars.dbRef.child(GlobalVars.accId).removeValue()
            handleSignOut()
        }

        // change ui depending on user logged in state
        handleSignedInStateUI(GlobalVars.loggedIn)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == RC_SIGN_IN) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task)
        }
    }

    fun getRandomString(length: Int) : String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val loggedInAcc: GoogleSignInAccount = completedTask.getResult(ApiException::class.java);
            GlobalVars.accName = loggedInAcc.givenName.toString()
            GlobalVars.loggedIn = true
            GlobalVars.accId = loggedInAcc.id.toString();
            Log.i("bean db", "---" + GlobalVars.dbRef.get().toString())

            // read db to check if user exists
            GlobalVars.dbRef.child(GlobalVars.accId).get().addOnSuccessListener {
                var accountExists = it.exists();

                if(accountExists) {
                    // already existing user...

                    // if user hasn't logged in today, add new Key, Val pair for today's date Date to Activities
                    var historicData = (it.children.filter { snapshot -> snapshot.key == "historicData"}[0].value as MutableMap<String, List<Int>>)
                    if(!historicData.keys.contains(GlobalVars.curDateStr)) {
                        historicData[GlobalVars.curDateStr] = List (24) {0}

                        var updatedUser = UserData(GlobalVars.accId, GlobalVars.accName, historicData)
                        var updatedUserValues = updatedUser.toMap()

                        GlobalVars.dbRef.child(GlobalVars.accId).updateChildren(updatedUserValues)
                            .addOnCompleteListener{
                                Log.i("DATABASE", "inserted!")
                            }.addOnFailureListener{
                                Log.i("DATABASE", "failed to insert >:(")
                            }
                    }

                }
                else{
                    // new user...
                    var uData = UserData(
                        loggedInAcc.id.toString(),
                        loggedInAcc.givenName.toString(),
                        mapOf(GlobalVars.curDateStr to List(24) {0})
                    )

                    GlobalVars.dbRef.child(GlobalVars.accId).setValue(uData)
                        .addOnCompleteListener{
                            Log.i("DATABASE", "inserted!")
                        }.addOnFailureListener{
                            Log.i("DATABASE", "failed to insert >:(")
                        }
                }

            }

            handleSignedInStateUI(true)
        }

        catch (e: ApiException){
            Log.i("FUCK WE BROKE IT",  e.statusCode.toString())

        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    // state - true, user is signed in
    // state - false, user is signed out
    private fun handleSignedInStateUI(state: Boolean){
        runOnUiThread{
            (findViewById(R.id.welcome_user) as TextView).isEnabled = state;
            (findViewById(R.id.welcome_user) as TextView).isVisible = state;
            (findViewById(R.id.welcome_user) as TextView).setText(GlobalVars.accName)

            googleLoginButton.isEnabled = !state;
            googleLoginButton.isVisible = !state;

            signOutButton.isEnabled = state;
            signOutButton.isVisible = state;

            deleteDataButton.isEnabled = state;
            deleteDataButton.isVisible = state;
        }
    }

    // ensure all references to user is removed & revoke any further access to the account
    private fun handleSignOut(){
        GlobalVars.accName = ""
        GlobalVars.accId = ""
        GlobalVars.loggedIn = false

        gsc.signOut()
        gsc.revokeAccess()

        handleSignedInStateUI(false)
    }

}