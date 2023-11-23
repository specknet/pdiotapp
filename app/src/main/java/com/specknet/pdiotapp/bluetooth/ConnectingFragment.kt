package com.specknet.pdiotapp.bluetooth

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.nfc.NfcAdapter
import android.nfc.NfcManager
import android.nfc.tech.NfcF
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.specknet.pdiotapp.R
import com.specknet.pdiotapp.barcode.BarcodeActivity
import com.specknet.pdiotapp.utils.Constants
import com.specknet.pdiotapp.utils.Utils

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"


/**
 * A simple [Fragment] subclass.
 * Use the [ConnectingFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ConnectingFragment : Fragment() {
    private var param1: String? = null
    private var param2: String? = null

    val REQUEST_CODE_SCAN_RESPECK = 0

    // Respeck
    private lateinit var scanRespeckButton: Button
    private lateinit var respeckID: EditText
    private lateinit var connectSensorsButton: Button
    private lateinit var restartConnectionButton: Button
//    private lateinit var disconnectRespeckButton: Button

    lateinit var sharedPreferences: SharedPreferences

    var nfcAdapter: NfcAdapter? = null
    val MIME_TEXT_PLAIN = "application/vnd.bluetooth.le.oob"
    private val TAG = "NFCReader"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view =
            inflater.inflate(R.layout.fragment_connecting, container, false)

        // scan respeck
        scanRespeckButton = view.findViewById(R.id.scan_respeck)!!
        respeckID = view.findViewById(R.id.respeck_code)!!
        connectSensorsButton = view.findViewById(R.id.connect_sensors_button)!!
        restartConnectionButton = view.findViewById(R.id.restart_service_button)!!

        scanRespeckButton.setOnClickListener {
            val barcodeScanner = Intent(requireActivity(), BarcodeActivity::class.java)
            startActivityForResult(barcodeScanner, REQUEST_CODE_SCAN_RESPECK)
        }

        connectSensorsButton.setOnClickListener {
            // start the bluetooth service

            sharedPreferences.edit().putString(
                Constants.RESPECK_MAC_ADDRESS_PREF,
                respeckID.text.toString()
            ).apply()
            sharedPreferences.edit().putInt(Constants.RESPECK_VERSION, 6).apply()

            startSpeckService()

        }

        restartConnectionButton.setOnClickListener {
            startSpeckService()
        }


        // first read shared preferences to see if there was a respeck there already
        sharedPreferences = requireActivity().getSharedPreferences(Constants.PREFERENCES_FILE, Context.MODE_PRIVATE)
        if (sharedPreferences.contains(Constants.RESPECK_MAC_ADDRESS_PREF)) {
            Log.i("sharedpref", "Already saw a respeckID")
            respeckID.setText(
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

        respeckID.filters = arrayOf<InputFilter>(InputFilter.AllCaps())

        val nfcManager = requireActivity().getSystemService(Context.NFC_SERVICE) as NfcManager
        nfcAdapter = nfcManager.defaultAdapter

        if (nfcAdapter == null) {
            Toast.makeText(requireContext(), "Phone does not support NFC pairing", Toast.LENGTH_LONG).show()
        } else if (nfcAdapter!!.isEnabled()) {
            Toast.makeText(requireContext(), "NFC Enabled", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(requireContext(), "NFC Disabled", Toast.LENGTH_LONG).show()
        }

        // Inflate the layout for this fragment
        return view
    }


    fun startSpeckService() {
        // TODO if it's not already running
        val isServiceRunning =
            Utils.isServiceRunning(
                BluetoothSpeckService::class.java,
                requireActivity().applicationContext
            )
        Log.i("service", "isServiceRunning = $isServiceRunning")

        if (!isServiceRunning) {
            Log.i("service", "Starting BLT service")
            val simpleIntent = Intent(requireActivity(), BluetoothSpeckService::class.java)
            requireActivity().startService(simpleIntent)
        } else {
            Log.i("service", "Service already running, restart")
            requireActivity().stopService(Intent(requireActivity(), BluetoothSpeckService::class.java))
            Toast.makeText(requireActivity(), "restarting service with new sensors", Toast.LENGTH_SHORT)
                .show()
            val simpleIntent = Intent(requireActivity(), BluetoothSpeckService::class.java)
            requireActivity().startService(simpleIntent)
        }
    }

    override fun onResume() {
        super.onResume()

        if (nfcAdapter != null) {
            setupForegroundDispatch(requireActivity(), nfcAdapter!!)
        }
    }

    /**
     * @param activity The corresponding [Activity] requesting the foreground dispatch.
     * @param adapter The [NfcAdapter] used for the foreground dispatch.
     */
    fun setupForegroundDispatch(activity: Activity, adapter: NfcAdapter) {
        Log.d(TAG, "setupForegroundDispatch: here ")
        val intent = Intent(requireContext(), activity.javaClass)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        val pendingIntent = PendingIntent.getActivity(requireContext(), 0, intent, 0)

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

    /**
     * @param activity The corresponding [BaseActivity] requesting to stop the foreground dispatch.
     * @param adapter The [NfcAdapter] used for the foreground dispatch.
     */
    fun stopForegroundDispatch(activity: Activity?, adapter: NfcAdapter) {
        adapter.disableForegroundDispatch(activity)
    }

    override fun onPause() {

        if (nfcAdapter != null) {
            stopForegroundDispatch(activity, nfcAdapter!!)
        }
        super.onPause()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            var scanResult = data?.extras?.getString("ScanResult")

            if (scanResult != null) {
                Log.i("ble", "Scan result=$scanResult")

                if (scanResult.contains(":")) {
                    // this is a respeck V6 and we should store its MAC address
                    respeckID.setText(scanResult)
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

                    Log.i("Debug", "Scan result = $scanResult")
                    respeckID.setText(scanResult)
                    sharedPreferences.edit().putString(
                        Constants.RESPECK_MAC_ADDRESS_PREF,
                        scanResult
                    ).apply()
                    sharedPreferences.edit().putInt(Constants.RESPECK_VERSION, 5).apply()
                }

                connectSensorsButton.isEnabled = true
                connectSensorsButton.isClickable = true

            } else {
                respeckID.setText("No respeck found :(")
            }

        }

    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ConnectingFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ConnectingFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}