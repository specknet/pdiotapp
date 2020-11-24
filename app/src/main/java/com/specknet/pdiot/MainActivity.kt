package com.specknet.pdiot

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.specknet.pdiot.bluetooth.BluetoothService
import com.specknet.pdiot.bluetooth.ConnectingActivity
import com.specknet.pdiot.live.LiveDataActivity
import com.specknet.pdiot.onboarding.OnBoardingActivity
import com.specknet.pdiot.utils.Constants
import com.specknet.pdiot.utils.Utils
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    // buttons and textviews
    lateinit var liveProcessingButton: Button
    lateinit var pairingButton: Button
    lateinit var recordButton: Button
    lateinit var stopRecordButton: Button
    lateinit var profileButton: Button
    lateinit var respeckStatus: TextView
    lateinit var welcomeText: TextView
    lateinit var mAuth: FirebaseAuth
    var  user: FirebaseUser? =null


    // permissions
    lateinit var permissionAlertDialog: AlertDialog.Builder

    val permissionsForRequest = arrayListOf<String>()

    var locationPermissionGranted = false
    var cameraPermissionGranted = false
    var readStoragePermissionGranted = false
    var writeStoragePermissionGranted = false

    // broadcast receiver
    lateinit var respeckStatusReceiver: BroadcastReceiver
    val filter = IntentFilter()

    var isUserFirstTime = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mAuth= FirebaseAuth.getInstance()
        user=mAuth.currentUser
        if(user==null)
        {
            val intent = Intent(this, SignInRegisterActivity::class.java)
            startActivity(intent)
            finish()
        }
        else
        {
            updateUI(user!!)
        }


        // check whether the onboarding screen should be shown
        val sharedPreferences = getSharedPreferences(Constants.PREFERENCES_FILE, Context.MODE_PRIVATE)
        if (sharedPreferences.contains(Constants.PREF_USER_FIRST_TIME)) {
            isUserFirstTime = false
        }
        else {
            isUserFirstTime = true
            sharedPreferences.edit().putBoolean(Constants.PREF_USER_FIRST_TIME, false).apply()
            val introIntent = Intent(this, OnBoardingActivity::class.java)
            startActivity(introIntent)
        }

        liveProcessingButton = findViewById(R.id.live_button)
        pairingButton = findViewById(R.id.ble_button)
        recordButton = findViewById(R.id.record_button)
        stopRecordButton=findViewById(R.id.stop_record_button)
        profileButton=findViewById(R.id.profile_button)
        respeckStatus = findViewById(R.id.respeck_status_welcome)
        welcomeText=findViewById(R.id.welcome_TextView)

        permissionAlertDialog = AlertDialog.Builder(this)

        setupClickListeners()

        setupPermissions()

        setupRespeckStatus()

        // register a broadcast receiver for respeck status
        filter.addAction(Constants.ACTION_RESPECK_CONNECTED)
        filter.addAction(Constants.ACTION_RESPECK_DISCONNECTED)

        respeckStatusReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action

                when(action) {
                    Constants.ACTION_RESPECK_CONNECTED -> respeckStatus.text = "Respeck status: Connected"
                    Constants.ACTION_RESPECK_DISCONNECTED -> respeckStatus.text = "Respeck status: Disconnected"
                    else -> respeckStatus.text = "Error"
                }
            }
        }

        this.registerReceiver(respeckStatusReceiver, filter)
        val notifyIntent = Intent(this, LevelReminder::class.java)
        notifyIntent.putExtra("userUID", user!!.uid)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, notifyIntent, 0)

        val alarmManager =
            getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis()+AlarmManager.INTERVAL_HOUR*4,
            AlarmManager.INTERVAL_HOUR * 4,
            pendingIntent
        )
    }

    private fun updateUI(user: FirebaseUser) {
        val uid = user.uid
        val databaseRef = FirebaseDatabase.getInstance().getReference("/users/")
        databaseRef.child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userData =
                    snapshot.getValue(User::class.java)!!
                welcomeText.setText("Welcome " + userData.username)
            }

            override fun onCancelled(error: DatabaseError) {}
        })


    }

    fun setupClickListeners() {
        liveProcessingButton.setOnClickListener {
            val intent = Intent(this, LiveDataActivity::class.java)
            startActivity(intent)
        }

        pairingButton.setOnClickListener {
            val intent = Intent(this, ConnectingActivity::class.java)
            startActivity(intent)
        }

        recordButton.setOnClickListener {
            if(respeckStatus.text=="Respeck status: Connected")
            {
                startService()
            }
            else
            {
                Toast.makeText(this,"Connect Respect first to record your activity!",Toast.LENGTH_LONG).show();
            }
        }
        stopRecordButton.setOnClickListener{
            stopService()
        }
        profileButton.setOnClickListener {
             val intent=Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }
    }

    private fun stopService() {
        val serviceIntent = Intent(this, TrackService::class.java)
        stopService(serviceIntent)
    }

    fun setupPermissions() {
        // request permissions

        // location permission
        Log.i("Permissions", "Location permission = " + locationPermissionGranted)
        if (ActivityCompat.checkSelfPermission(applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsForRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissionsForRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        else {
            locationPermissionGranted = true
        }

        // camera permission
        Log.i("Permissions", "Camera permission = " + cameraPermissionGranted)
        if (ActivityCompat.checkSelfPermission(applicationContext,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.i("Permissions", "Camera permission = " + cameraPermissionGranted)
            permissionsForRequest.add(Manifest.permission.CAMERA)
        }
        else {
            cameraPermissionGranted = true
        }

        // read storage permission
        Log.i("Permissions", "Read st permission = " + readStoragePermissionGranted)
        if (ActivityCompat.checkSelfPermission(applicationContext,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.i("Permissions", "Read st permission = " + readStoragePermissionGranted)
            permissionsForRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        else {
            readStoragePermissionGranted = true
        }

        // write storage permission
        Log.i("Permissions", "Write storage permission = " + writeStoragePermissionGranted)
        if (ActivityCompat.checkSelfPermission(applicationContext,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.i("Permissions", "Write storage permission = " + writeStoragePermissionGranted)
            permissionsForRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        else {
            writeStoragePermissionGranted = true
        }

        if (permissionsForRequest.size >= 1) {
            ActivityCompat.requestPermissions(this,
                permissionsForRequest.toTypedArray(),
                Constants.REQUEST_CODE_PERMISSIONS)
        }

    }

    fun setupRespeckStatus() {
        val isServiceRunning = Utils.isServiceRunning(
            BluetoothService::class.java, applicationContext)
        Log.i("debug","isServiceRunning = " + isServiceRunning)

        // check sharedPreferences for an existing Respeck id
        val sharedPreferences = getSharedPreferences(Constants.PREFERENCES_FILE, Context.MODE_PRIVATE)
        if (sharedPreferences.contains(Constants.RESPECK_MAC_ADDRESS_PREF)) {
            Log.i("sharedpref", "Already saw a respeckID, starting service and attempting to reconnect")
            respeckStatus.text = "Respeck status: Connecting..."

            // launch service to reconnect
            // start the bluetooth service if it's not already running
            if(!isServiceRunning) {
                Log.i("service", "Starting BLT service")
                val simpleIntent = Intent(this, BluetoothService::class.java)
                this.startService(simpleIntent)
            }
        }
        else {
            Log.i("sharedpref", "No Respeck seen before, must pair first")
            respeckStatus.text = "Respeck status: Unpaired"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(respeckStatusReceiver)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if(requestCode == Constants.REQUEST_CODE_PERMISSIONS) {
            if(grantResults.isNotEmpty()) {
                for (i in grantResults.indices) {
                    when(permissionsForRequest[i]) {
                        Manifest.permission.ACCESS_COARSE_LOCATION -> locationPermissionGranted = true
                        Manifest.permission.ACCESS_FINE_LOCATION -> locationPermissionGranted = true
                        Manifest.permission.CAMERA -> cameraPermissionGranted = true
                        Manifest.permission.READ_EXTERNAL_STORAGE -> readStoragePermissionGranted = true
                        Manifest.permission.WRITE_EXTERNAL_STORAGE -> writeStoragePermissionGranted = true
                    }

                }
            }
        }

        // count how many permissions need granting
        var numberOfPermissionsUngranted = 0
        if (!locationPermissionGranted) numberOfPermissionsUngranted++
        if (!cameraPermissionGranted) numberOfPermissionsUngranted++
        if (!readStoragePermissionGranted) numberOfPermissionsUngranted++
        if (!writeStoragePermissionGranted) numberOfPermissionsUngranted++

        // show a general message if we need multiple permissions
        if (numberOfPermissionsUngranted > 1) {
            val generalSnackbar = Snackbar
                .make(coordinatorLayout, "Several permissions are needed for correct app functioning", Snackbar.LENGTH_LONG)
                .setAction("SETTINGS") {
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                }
                .show()
        }
        else if(numberOfPermissionsUngranted == 1) {
            var snackbar: Snackbar = Snackbar.make(coordinatorLayout, "", Snackbar.LENGTH_LONG)
            if (!locationPermissionGranted) {
                snackbar = Snackbar
                    .make(
                        coordinatorLayout,
                        "Location permission needed for Bluetooth to work.",
                        Snackbar.LENGTH_LONG
                    )
            }

            if(!cameraPermissionGranted) {
                snackbar = Snackbar
                    .make(
                        coordinatorLayout,
                        "Camera permission needed for QR code scanning to work.",
                        Snackbar.LENGTH_LONG
                    )
            }

            if(!readStoragePermissionGranted || !writeStoragePermissionGranted) {
                snackbar = Snackbar
                    .make(
                        coordinatorLayout,
                        "Storage permission needed to record sensor.",
                        Snackbar.LENGTH_LONG
                    )
            }

            snackbar.setAction("SETTINGS") {
                val settingsIntent = Intent(Settings.ACTION_SETTINGS)
                startActivity(settingsIntent)
            }
                .show()
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_welcome, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.show_tutorial) {
            val introIntent = Intent(this, OnBoardingActivity::class.java)
            startActivity(introIntent)
            return true
        }
        if(id==R.id.sign_out)
        {
            stopService()
            mAuth.signOut()
            val intent = Intent(this, SignInRegisterActivity::class.java)
            startActivity(intent)
            finish()
        }

        return super.onOptionsItemSelected(item)
    }
    private fun startService() {
        val input =
            "Keep service running to record your actions!" //will change it just testing
        val serviceIntent = Intent(this, TrackService::class.java)
        serviceIntent.putExtra("inputExtra", input)
        startService(serviceIntent)
    }

}