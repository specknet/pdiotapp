package com.specknet.pdiotapp.barcode

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.SparseArray
import android.view.SurfaceHolder
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.util.isNotEmpty
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import com.specknet.pdiotapp.R
import kotlinx.android.synthetic.main.activity_barcode.*
import java.lang.Exception

class BarcodeActivity : AppCompatActivity() {

    private val requestCodeCameraPermission = 1001
    private lateinit var cameraSource: CameraSource
    private lateinit var detector: BarcodeDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_barcode)

        if(ContextCompat.checkSelfPermission(
                this@BarcodeActivity,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
        ) {
            askForCameraPermission()
        }
        else {
            setupControls()
        }
    }

    private fun setupControls() {
        detector = BarcodeDetector.Builder(this).build()
        cameraSource = CameraSource.Builder(this, detector)
            .setAutoFocusEnabled(true)
            .build()
        cameraSurfaceView.holder.addCallback(surfaceCallBack)
        detector.setProcessor(processor)
    }

    private fun askForCameraPermission() {
        ActivityCompat.requestPermissions(
            this@BarcodeActivity,
            arrayOf(Manifest.permission.CAMERA),
            requestCodeCameraPermission)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestCodeCameraPermission && grantResults.isNotEmpty()) {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupControls()
            }
            else {
                Toast.makeText(applicationContext, "Permission denied", Toast.LENGTH_LONG).show()
            }
        }
    }

    private val surfaceCallBack = object : SurfaceHolder.Callback {

        override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {
        }

        override fun surfaceDestroyed(p0: SurfaceHolder) {
            cameraSource.stop()
        }

        @SuppressLint("MissingPermission")
        override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
            try {
                cameraSource.start(surfaceHolder)

            } catch (exception: Exception) {
                Toast.makeText(applicationContext, "Something went wrong", Toast.LENGTH_LONG).show()
            }
        }

    }

    private val processor = object : Detector.Processor<Barcode> {
        override fun release() {
        }

        override fun receiveDetections(detections: Detector.Detections<Barcode>?) {
            if(detections != null && detections.detectedItems.isNotEmpty()) {
                Log.i("Barcode", "Barcode detected!")
                val qrCodes: SparseArray<Barcode> = detections.detectedItems
                val code = qrCodes.valueAt(0)

                runOnUiThread {
                    textScanResult.text = code.displayValue
                }

                val returnIntent = Intent()
                returnIntent.putExtra("ScanResult", code.displayValue)
                setResult(Activity.RESULT_OK, returnIntent)
                finish()

            } else {
                Log.i("Barcode", "Don't see nothin")

                runOnUiThread {
                    textScanResult.text = ""
                }

            }
        }

    }
}