package com.specknet.pdiotapp.utils

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel


class RespeckClassifier(private val context: Context) {
    //    val activityList = listOf(
//        "Ascending stairs Normal", "Descending stairs Normal",
//        "Lying down back Coughing", "Lying down back Hyperventilating",
//        "Lying down back Laughing", "Lying down back Normal",
//        "Lying down back Singing", "Lying down back Talking",
//        "Lying down on left Coughing", "Lying down on left Hyperventilating",
//        "Lying down on left Laughing", "Lying down on left Normal",
//        "Lying down on left Singing", "Lying down on left Talking",
//        "Lying down on stomach Coughing", "Lying down on stomach Hyperventilating",
//        "Lying down on stomach Laughing", "Lying down on stomach Normal",
//        "Lying down on stomach Singing", "Lying down on stomach Talking",
//        "Lying down right Coughing", "Lying down right Hyperventilating",
//        "Lying down right Laughing", "Lying down right Normal",
//        "Lying down right Singing", "Lying down right Talking",
//        "Miscellaneous movements Normal', 'Normal walking Normal",
//        "Running Normal", "Shuffle walking Normal', 'Sitting Coughing",
//        "Sitting Eating", "Sitting Hyperventilating', 'Sitting Laughing",
//        "Sitting Normal", "Sitting Singing", "Sitting Talking",
//        "Standing Coughing", "Standing Eating",
//        "Standing Hyperventilating", "Standing Laughing",
//        "Standing Normal", "Standing Singing", "Standing Talking"
//    )
    val activityList = listOf(
        "Ascending stairs Normal",
        "Descending stairs Normal",
        "Lying down back Coughing",
        "Lying down back Hyperventilating",
        "Lying down back Laughing",
        "Lying down back Normal",
        "Lying down back Singing",
        "Lying down back Talking",
        "Lying down on left Coughing",
        "Lying down on left Hyperventilating",
        "Lying down on left Laughing",
        "Lying down on left Normal",
        "Lying down on left Singing",
        "Lying down on left Talking",
        "Lying down on stomach Coughing",
        "Lying down on stomach Hyperventilating",
        "Lying down on stomach Laughing",
        "Lying down on stomach Normal",
        "Lying down on stomach Singing",
        "Lying down on stomach Talking",
        "Lying down right Coughing",
        "Lying down right Hyperventilating",
        "Lying down right Laughing",
        "Lying down right Normal",
        "Lying down right Singing",
        "Lying down right Talking",
        "Miscellaneous movements Normal",
        "Normal walking Normal",
        "Running Normal",
        "Shuffle walking Normal",
        "Sitting Coughing",
        "Sitting Eating",
        "Sitting Hyperventilating",
        "Sitting Laughing",
        "Sitting Normal",
        "Sitting Singing",
        "Sitting Talking",
        "Standing Coughing",
        "Standing Eating",
        "Standing Hyperventilating",
        "Standing Laughing",
        "Standing Normal",
        "Standing Singing",
        "Standing Talking"
    )

    val bufferSize = 4  // Size of float32 in bytes
    val inputBuffer = ByteBuffer.allocateDirect(bufferSize * 1 * 50 * 6 * 1)
    var index = 0

    init {
        inputBuffer.order(ByteOrder.nativeOrder())
    }

    fun addData(
        accelX: Float,
        accelY: Float,
        accelZ: Float,
        gyroX: Float,
        gyroY: Float,
        gyroZ: Float
    ) {
        Log.d("ADDING_DATA", "$accelX, $accelY, $accelZ, $gyroX, $gyroY, $gyroZ")
        if (index == 50) {
            // Delete the first 10 elements and shift the remaining data to the left
            inputBuffer.position(10 * 6 * bufferSize)
            val remainingData = inputBuffer.slice()
            inputBuffer.clear()
            inputBuffer.put(remainingData)
            index -= 10
        }

        // Calculate the position to write to in the ByteBuffer
        val position = (index * 6) * bufferSize

        // Write the sensor data to the ByteBuffer
        inputBuffer.putFloat(position, accelX)
        inputBuffer.putFloat(position + bufferSize, accelY)
        inputBuffer.putFloat(position + 2 * bufferSize, accelZ)
        inputBuffer.putFloat(position + 3 * bufferSize, gyroX)
        inputBuffer.putFloat(position + 4 * bufferSize, gyroY)
        inputBuffer.putFloat(position + 5 * bufferSize, gyroZ)

        // Increment the index
        index++
    }

    fun classifyData(): String {
        val assetManager = context.assets
        val modelFileName = "trained_model.tflite"

        val outputBuffer = ByteBuffer.allocateDirect(bufferSize * 1 * 44)
        outputBuffer.order(ByteOrder.nativeOrder())

        Log.d("LOADING", "Loading the Model")
        val fileDescriptor = assetManager.openFd(modelFileName)
        val inputStream = fileDescriptor.createInputStream()
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        val tfliteModel: ByteBuffer =
            fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

        Log.d("LOADING", "Loaded the Interpreter")
        val interpreter = Interpreter(tfliteModel)

        Log.d("CLASSIFYING", "Classifying the data")
        interpreter.run(inputBuffer, outputBuffer)

        Log.d("DECODING OUTPUT", "Decoding the Output Buffer")
        val outputArray = FloatArray(bufferSize * 1 * 44)
        for (i in 0 until 44) {
            outputArray[i] = outputBuffer.getFloat(i * 4)
        }

        Log.d("RETURNING OUTPUT", "Returning the Prediction")
        val maxIndex = outputArray.indices.maxByOrNull { outputArray[it] }
        return activityList[maxIndex!!]
    }

}