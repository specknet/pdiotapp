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
import java.util.Queue;

public class MotionInference {
    private Context context;
    private Interpreter motion_model;
    private Map<Integer, String> motionTypes;

    public MotionInference(Context context) {

        motionTypes = new HashMap<>();
        motionTypes.put(0, "Static");
        motionTypes.put(1, "Dynamic");

        this.context = context.getApplicationContext();
    }

    public void loadModel() {

        try {
            AssetManager assetManager = context.getAssets();
            AssetFileDescriptor modelFileDescriptor = assetManager.openFd("new_motion_model.tflite");
            FileInputStream inputStream = new FileInputStream(modelFileDescriptor.getFileDescriptor());
            FileChannel fileChannel = inputStream.getChannel();
            long startOffset = modelFileDescriptor.getStartOffset();
            long declaredLength = modelFileDescriptor.getDeclaredLength();
            motion_model = new Interpreter(fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength));

            int inputTensorIndex = 0; // Modify accordingly based on your model
            int[] inputTensorShape = motion_model.getInputTensor(inputTensorIndex).shape();
            Log.i("MOTION INPUT SHAPE", "Motion Input Tensor Shape: " + Arrays.toString(inputTensorShape));

            // Print output tensor shape
            int outputTensorIndex = 0; // Modify accordingly based on your model
            int[] outputTensorShape = motion_model.getOutputTensor(outputTensorIndex).shape();
            Log.i("MOTION OUTPUT SHAPE","Motion Output Tensor Shape: " + Arrays.toString(outputTensorShape));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Run inference and return the result
    public String runInference(float[][][] inputData) {
        float[][] outputMotion = new float[1][2];
        Log.e("MOTION", "Before Model Run");
        motion_model.run(inputData, outputMotion);
        Log.e("MOTION", "After Model Run");


        String classification = motionTypes.get(getArgmax(outputMotion[0]));

        Log.i("Motion classification", classification);
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
