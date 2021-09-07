package com.specknet.pdiotapp.bluetooth

import android.app.Activity
import android.content.*
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputFilter.AllCaps
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.specknet.pdiotapp.R
import com.specknet.pdiotapp.barcode.BarcodeActivity
import com.specknet.pdiotapp.utils.Constants
import kotlinx.android.synthetic.main.activity_connecting.*

class ConnectingActivity : AppCompatActivity() {

    val REQUEST_CODE_SCAN_RESPECK = 0

    // Respeck
    private lateinit var scanRespeckButton: Button
    private lateinit var respeckQrCode: EditText
    private lateinit var connectRespeckButton: Button
    private lateinit var disconnectRespeckButton: Button

    // Thingy
    private lateinit var scanThingyButton: Button
    private lateinit var thingyQrCode: EditText
    private lateinit var connectThingyButton: Button
    private lateinit var disconnectThingyButton: Button

    lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connecting)

        // scan respeck
        scanRespeckButton = findViewById(R.id.scan_respeck)
        respeckQrCode = findViewById(R.id.respeck_code)
        connectRespeckButton = findViewById(R.id.connect_button)
        disconnectRespeckButton = findViewById(R.id.disconnect_button)

        scanThingyButton = findViewById(R.id.scan_thingy)
        thingyQrCode = findViewById(R.id.thingy_code)
        connectThingyButton = findViewById(R.id.connect_thingy_button)
        disconnectThingyButton = findViewById(R.id.disconnect_thingy_button)

        scanRespeckButton.setOnClickListener {
            val barcodeScanner = Intent(this, BarcodeActivity::class.java)
            startActivityForResult(barcodeScanner, REQUEST_CODE_SCAN_RESPECK)
        }

        scanThingyButton.setOnClickListener {
            // TODO this should only be done with NFC
        }

        connectRespeckButton.setOnClickListener {
            // start the bluetooth service

            sharedPreferences.edit().putString(
                Constants.RESPECK_MAC_ADDRESS_PREF,
                respeckQrCode.text.toString()
            ).apply()
            sharedPreferences.edit().putInt(Constants.RESPECK_VERSION, 6).apply()

            // TODO if it's not already running
            Log.i("service", "Starting BLT service")
            val simpleIntent = Intent(this, BluetoothSpeckService::class.java)
            this.startService(simpleIntent)
        }

        connectThingyButton.setOnClickListener {
            // start the bluetooth service

            sharedPreferences.edit().putString(
                Constants.THINGY_MAC_ADDRESS_PREF,
                thingyQrCode.text.toString()
            ).apply()

            // TODO if it's not already running

//            Log.i("service", "Starting BLT service")
//            val simpleIntent = Intent(this, BluetoothSpeckService::class.java)
//            this.startService(simpleIntent)

        }

        disconnectRespeckButton.setOnClickListener {
            Log.i("service", "Tearing down BLT service")
//            val simpleIntent = Intent(this, BluetoothService::class.java)
//            this.stopService(simpleIntent)
        }

        // first read shared preferences to see if there was a respeck there already
        sharedPreferences = getSharedPreferences(Constants.PREFERENCES_FILE, Context.MODE_PRIVATE)
        if (sharedPreferences.contains(Constants.RESPECK_MAC_ADDRESS_PREF)) {
            Log.i("sharedpref", "Already saw a respeckID")
            respeck_code.setText(
                sharedPreferences.getString(
                    Constants.RESPECK_MAC_ADDRESS_PREF,
                    ""
                )
            )
        } else {
            Log.i("sharedpref", "No respeck seen before")
            connectRespeckButton.isEnabled = false
            connectRespeckButton.isClickable = false
        }

        if (sharedPreferences.contains(Constants.THINGY_MAC_ADDRESS_PREF)) {
            Log.i("sharedpref", "Already saw a thingy ID")

            thingy_code.setText(
                sharedPreferences.getString(
                    Constants.THINGY_MAC_ADDRESS_PREF,
                    ""
                )
            )
        }

        respeckQrCode.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(cs: CharSequence, start: Int, before: Int, count: Int) {
                if (cs.toString().trim().length != 17) {
                    connectRespeckButton.isEnabled = false
                    connectRespeckButton.isClickable = false
                } else {
                    connectRespeckButton.isEnabled = true
                    connectRespeckButton.isClickable = true
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun afterTextChanged(p0: Editable?) {

            }
        })

        respeckQrCode.filters = arrayOf<InputFilter>(AllCaps())

        thingyQrCode.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(cs: CharSequence, start: Int, before: Int, count: Int) {
                if (cs.toString().trim().length != 17) {
                    connectThingyButton.isEnabled = false
                    connectThingyButton.isClickable = false
                } else {
                    connectThingyButton.isEnabled = true
                    connectThingyButton.isClickable = true
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun afterTextChanged(p0: Editable?) {

            }
        })

        thingyQrCode.filters = arrayOf<InputFilter>(AllCaps())


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            var scanResult = data?.extras?.getString("ScanResult")

            if (scanResult != null) {
                Log.i("ble", "Scan result=" + scanResult)

                if (scanResult.contains(":")) {
                    // this is a respeck V6 and we should store its MAC address
                    respeck_code.setText(scanResult)
                    sharedPreferences.edit().putString(
                        Constants.RESPECK_MAC_ADDRESS_PREF,
                        scanResult.toString()
                    ).apply()
                    sharedPreferences.edit().putInt(Constants.RESPECK_VERSION, 6).apply()

                }
                if (!scanResult.contains(":") && !scanResult.contains("-")) {
                    val sb = StringBuilder(scanResult)
                    if (scanResult.length == 20)
                        sb.insert(4, "-")
                    else if (scanResult.length == 16)
                        sb.insert(0, "0105-")
                    scanResult = sb.toString()

                    Log.i("Debug", "Scan result = " + scanResult)
                    respeck_code.setText(scanResult)
                    sharedPreferences.edit().putString(
                        Constants.RESPECK_MAC_ADDRESS_PREF,
                        scanResult
                    ).apply()
                    sharedPreferences.edit().putInt(Constants.RESPECK_VERSION, 5).apply()
                }

                connectRespeckButton.isEnabled = true
                connectRespeckButton.isClickable = true

            } else {
                respeck_code.setText("No respeck found :(")
            }

        }

    }

    override fun onDestroy() {
        super.onDestroy()
    }


}
