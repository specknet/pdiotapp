package com.specknet.pdiotapp.bluetooth

import android.app.Activity
import android.app.PendingIntent
import android.content.*
import android.nfc.NfcAdapter
import android.nfc.NfcManager
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NfcF
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputFilter.AllCaps
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.specknet.pdiotapp.R
import com.specknet.pdiotapp.barcode.BarcodeActivity
import com.specknet.pdiotapp.utils.Constants
import com.specknet.pdiotapp.utils.Utils
import kotlinx.android.synthetic.main.activity_connecting.*
import java.util.*
import kotlin.experimental.and

class ConnectingActivity : AppCompatActivity() {

    val REQUEST_CODE_SCAN_RESPECK = 0

    // Respeck
    private lateinit var scanRespeckButton: Button
    private lateinit var respeckID: EditText
    private lateinit var connectSensorsButton: Button
    private lateinit var restartConnectionButton: Button
//    private lateinit var disconnectRespeckButton: Button

    // Thingy
//    private lateinit var scanThingyButton: Button
    private lateinit var thingyID: EditText
//    private lateinit var connectThingyButton: Button
//    private lateinit var disconnectThingyButton: Button

    lateinit var sharedPreferences: SharedPreferences

    var nfcAdapter: NfcAdapter? = null
    val MIME_TEXT_PLAIN = "application/vnd.bluetooth.le.oob"
    private val TAG = "NFCReader"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connecting)

        // scan respeck
        scanRespeckButton = findViewById(R.id.scan_respeck)
        respeckID = findViewById(R.id.respeck_code)
        connectSensorsButton = findViewById(R.id.connect_sensors_button)
        restartConnectionButton = findViewById(R.id.restart_service_button)

        thingyID = findViewById(R.id.thingy_code)

        scanRespeckButton.setOnClickListener {
            val barcodeScanner = Intent(this, BarcodeActivity::class.java)
            startActivityForResult(barcodeScanner, REQUEST_CODE_SCAN_RESPECK)
        }

        connectSensorsButton.setOnClickListener {
            // TODO don't enable this until both sensors have been scanned? or at least warn the user
            // start the bluetooth service

            sharedPreferences.edit().putString(
                Constants.RESPECK_MAC_ADDRESS_PREF,
                respeckID.text.toString()
            ).apply()
            sharedPreferences.edit().putInt(Constants.RESPECK_VERSION, 6).apply()

            sharedPreferences.edit().putString(
                Constants.THINGY_MAC_ADDRESS_PREF,
                thingyID.text.toString()
            ).apply()

            startSpeckService()

        }

        restartConnectionButton.setOnClickListener {
            startSpeckService()
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
            connectSensorsButton.isEnabled = false
            connectSensorsButton.isClickable = false
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

        respeckID.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(cs: CharSequence, start: Int, before: Int, count: Int) {
                if (cs.toString().trim().length != 17) {
                    connectSensorsButton.isEnabled = false
                    connectSensorsButton.isClickable = false
                } else {
                    connectSensorsButton.isEnabled = true
                    connectSensorsButton.isClickable = true
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun afterTextChanged(p0: Editable?) {

            }
        })

        respeckID.filters = arrayOf<InputFilter>(AllCaps())

        thingyID.filters = arrayOf<InputFilter>(AllCaps())
        val nfcManager = getSystemService(Context.NFC_SERVICE) as NfcManager
        nfcAdapter = nfcManager.defaultAdapter

        if (nfcAdapter == null) {
            Toast.makeText(this, "Phone does not support NFC pairing", Toast.LENGTH_LONG).show()
        } else if (nfcAdapter!!.isEnabled()) {
            Toast.makeText(this, "NFC Enabled", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "NFC Disabled", Toast.LENGTH_LONG).show()
        }


    }

    fun startSpeckService() {
        // TODO if it's not already running
        val isServiceRunning = Utils.isServiceRunning(BluetoothSpeckService::class.java, applicationContext)
        Log.i("service","isServiceRunning = " + isServiceRunning)

        if (!isServiceRunning) {
            Log.i("service", "Starting BLT service")
            val simpleIntent = Intent(this, BluetoothSpeckService::class.java)
            this.startService(simpleIntent)
        }
        else {
            Log.i("service", "Service already running, restart")
            this.stopService(Intent(this, BluetoothSpeckService::class.java))
            Toast.makeText(this, "restarting service with new sensors", Toast.LENGTH_SHORT).show()
            this.startService(Intent(this, BluetoothSpeckService::class.java))

        }
    }

    override fun onResume() {
        super.onResume()

        if (nfcAdapter != null) {
            setupForegroundDispatch(this, nfcAdapter!!)
        }
    }

    /**
     * @param activity The corresponding [Activity] requesting the foreground dispatch.
     * @param adapter The [NfcAdapter] used for the foreground dispatch.
     */
    fun setupForegroundDispatch(activity: Activity, adapter: NfcAdapter) {
        Log.d(TAG, "setupForegroundDispatch: here ")
        val intent = Intent(activity.applicationContext, activity.javaClass)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        val pendingIntent = PendingIntent.getActivity(activity.applicationContext, 0, intent, 0)

        val filters = arrayOfNulls<IntentFilter>(2)
        val techList = arrayOf(
            arrayOf(
                NfcF::class.java.name
            )
        )

        // Notice that this is the same filter as in our manifest.
        filters[0] = IntentFilter()
        filters[0]!!.addAction(NfcAdapter.ACTION_NDEF_DISCOVERED)
        filters[0]!!.addCategory(Intent.CATEGORY_DEFAULT)

        filters[1] = IntentFilter()
        filters[1]!!.addAction(NfcAdapter.ACTION_NDEF_DISCOVERED)
        try {
            filters[0]!!.addDataType(MIME_TEXT_PLAIN)
            filters[1]!!.addDataScheme("vnd.android.nfc")
            filters[1]!!.addDataAuthority("ext", null)
        } catch (e: IntentFilter.MalformedMimeTypeException) {
            throw RuntimeException("Check your mime type.")
        }

        adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList)
    }

    override fun onNewIntent(intent: Intent?) {
        Log.d(TAG, "onNewIntent: here")
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        Log.d(TAG, "handleIntent: here")
        val action = intent?.action

        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            val type = intent.type

            Log.d(TAG, "handleIntent: type = " + type)

            if (MIME_TEXT_PLAIN.equals(type)) {
                // This is the Respeck
                val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)

                val ndef = Ndef.get(tag)

                if (ndef == null) {
                    // NDEF is not supported by this Tag
                    return
                }

                val ndefMessage = ndef.cachedNdefMessage
                val records = ndefMessage.records

                Log.i("NFCReader", "Read records")
                Log.i("NFCReader", "Found " + records.size + " record(s)")
                Log.i("NFCReader", records[0].toMimeType())

                val payload = records[0].payload
                Log.i("NFCReader", "Payload length: " + payload.size)

                val payload_str = String(payload)
                Log.i("NFCReader", "Payload : $payload_str")

                val ble_name = payload_str.substring(20)

                Log.i("NFCReader", "BLE name: $ble_name")
                val ble_addr: String = Utils.bytesToHexNfc(Arrays.copyOfRange(payload, 5, 11))
                Log.i("NFCReader", "BLE Address: $ble_addr")

                Toast.makeText(this, "NFC scanned $ble_name ($ble_addr)", Toast.LENGTH_LONG).show()

//                if (!ble_addr.contains(':')) {
//                    // insert a : after each two characters
//                }

                respeckID.setText(ble_addr.toString())

            }
            else {
                // this is the thingy
                Log.d(TAG, "handleIntent: here after type")
                val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)

                val ndef = Ndef.get(tag)

                if (ndef == null) {
                    // NDEF is not supported by this Tag
                    return
                }

                val ndefMessage = ndef.cachedNdefMessage
                val records = ndefMessage.records

                Log.i("NFCReader", "Read records")
                Log.i("NFCReader", "Found " + records.size + " record(s)")
                Log.i("NFCReader", records[1].toMimeType())

                val payload = records[1].payload
                Log.i("NFCReader", "Payload length: " + payload.size)

                val payload_str = String(payload)
                Log.i("NFCReader", "Payload: $payload_str")

                val ble_addr = payload_str.substring(3, 20)
//
//                Log.i("NFCReader", "BLE name: $ble_name")
//                val ble_addr: String = Utils.bytesToHex(Arrays.copyOfRange(payload, 5, 11))
                Log.i("NFCReader", "BLE Address: $ble_addr")
//
                Toast.makeText(this, "NFC scanned ($ble_addr)", Toast.LENGTH_LONG).show()

                thingyID.setText(ble_addr)

            }

        }
    }

    /**
     * @param activity The corresponding [BaseActivity] requesting to stop the foreground dispatch.
     * @param adapter The [NfcAdapter] used for the foreground dispatch.
     */
    fun stopForegroundDispatch(activity: Activity?, adapter: NfcAdapter) {
        adapter.disableForegroundDispatch(activity)
    }

    override fun onPause() {

        if(nfcAdapter != null) {
            stopForegroundDispatch(this, nfcAdapter!!)
        }
        super.onPause()
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

                connectSensorsButton.isEnabled = true
                connectSensorsButton.isClickable = true

            } else {
                respeck_code.setText("No respeck found :(")
            }

        }

    }

}
