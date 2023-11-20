package com.specknet.pdiotapp.utils;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class BreathingInference {
    private Context context;
    private Interpreter breathing_model;
    private Map<Integer, String> breathingTypes;

    public BreathingInference(Context context) {

        this.context = context.getApplicationContext();

        breathingTypes = new HashMap<>();
        breathingTypes.put(0, "Breathing Normally");
        breathingTypes.put(1, "Coughing");
        breathingTypes.put(2, "Hyperventilating");
        breathingTypes.put(3, "Other");
    }

    public void loadModel() {

        try {
            AssetManager assetManager = context.getAssets();
            AssetFileDescriptor modelFileDescriptor = assetManager.openFd("new_breathing_model.tflite");
            FileInputStream inputStream = new FileInputStream(modelFileDescriptor.getFileDescriptor());
            FileChannel fileChannel = inputStream.getChannel();
            long startOffset = modelFileDescriptor.getStartOffset();
            long declaredLength = modelFileDescriptor.getDeclaredLength();
            breathing_model = new Interpreter(fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength));

            int inputTensorIndex = 0; // Modify accordingly based on your model
            int[] inputTensorShape = breathing_model.getInputTensor(inputTensorIndex).shape();
            Log.i("BREATHING INPUT SHAPE", "Breathing Input Tensor Shape: " + Arrays.toString(inputTensorShape));

            // Print output tensor shape
            int outputTensorIndex = 0; // Modify accordingly based on your model
            int[] outputTensorShape = breathing_model.getOutputTensor(outputTensorIndex).shape();
            Log.i("BREATHING OUTPUT SHAPE","Breathing Output Tensor Shape: " + Arrays.toString(outputTensorShape));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Run inference and return the result
    public String runInference(float[][][] inputData) {
        float[][] outputBreathing = new float[1][4];

        Log.e("BREATHING", "Before Model Run");
        breathing_model.run(inputData, outputBreathing);
        Log.e("BREATHING", "After Model Run");

        String classification = breathingTypes.get(getArgmax(outputBreathing[0]));

        Log.i("Breathing classification", classification);
        return classification;
    }

    public static int getArgmax(float[] array) {
        if (array.length == 0) {
            throw new IllegalArgumentException("Array is empty");
        }

        int argmax = 0;
        float max = array[0];

        for (int i = 1; i < array.length; i++) {
            if (array[i] > max) {
                max = array[i];
                argmax = i;
            }
        }

        return argmax;
    }
}
