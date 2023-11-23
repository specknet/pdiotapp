package com.specknet.pdiotapp.detect

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class Classifier(
    val modelPath: String,
    val windowSize: Int,
    val numFeatures: Int = 3,
    val stepSize: Int = 10,
    val outputSize: Int,
    val activityList: List<String>) {

    private val bufferSize = 4  // Size of float32 in bytes
    private val inputBuffer = ByteBuffer.allocateDirect(bufferSize * 1 * windowSize * numFeatures)
    var index = 0

    init {
        inputBuffer.order(ByteOrder.nativeOrder())
    }

    fun addData(accel_x: Float, accel_y: Float, accel_z: Float, gyro_x: Float, gyro_y: Float, gyro_z: Float) {
        Log.d("ADDING_DATA", "$accel_x, $accel_y, $accel_z, $gyro_x, $gyro_y, $gyro_z")
        if (index == windowSize) {
            // Delete the first 10 elements and shift the remaining data to the left
            inputBuffer.position(stepSize * numFeatures * bufferSize)
            val remainingData = inputBuffer.slice()
            inputBuffer.clear()
            inputBuffer.put(remainingData)
            index -= stepSize
        }

        // Calculate the position to write to in the ByteBuffer
        val position = (index * numFeatures) * bufferSize

        // Write the sensor data to the ByteBuffer
        inputBuffer.putFloat(position, accel_x)
        inputBuffer.putFloat(position + bufferSize, accel_y)
        inputBuffer.putFloat(position + 2 * bufferSize, accel_z)

        index++
    }


    fun classifyData(context: Context): String {
        val assetManager = context.assets

        val outputBuffer = ByteBuffer.allocateDirect(bufferSize * 1 * outputSize)
        outputBuffer.order(ByteOrder.nativeOrder())

        val fileDescriptor = assetManager.openFd(modelPath)
        val inputStream = fileDescriptor.createInputStream()
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        val tfliteModel: ByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

        val interpreter = Interpreter(tfliteModel)

        interpreter.run(inputBuffer, outputBuffer)

        val outputArray = FloatArray(bufferSize * 1 * outputSize)
        for (i in 0 until outputSize) {
            outputArray[i] = outputBuffer.getFloat(i * bufferSize)
        }

        val maxIndex = outputArray.indices.maxByOrNull { outputArray[it] }
        return activityList[maxIndex!!]
    }

    fun clearInputBuffer() {
        inputBuffer.clear()
        index = 0
    }

}