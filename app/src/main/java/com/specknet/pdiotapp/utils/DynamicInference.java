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

public class DynamicInference {
    private Context context;
    private Interpreter dynamic_model;
    private Map<Integer, String> dynamicTypes;

    public DynamicInference(Context context) {

        this.context = context.getApplicationContext();

        dynamicTypes = new HashMap<>();
        dynamicTypes.put(0, "normalWalking");
        dynamicTypes.put(1, "running");
        dynamicTypes.put(2, "descending");
        dynamicTypes.put(3, "ascending");
        dynamicTypes.put(4, "shuffleWalking");
        dynamicTypes.put(5, "miscMovement");
    }

    public void loadModel() {

        try {
            AssetManager assetManager = context.getAssets();
            AssetFileDescriptor modelFileDescriptor = assetManager.openFd("dynamic_model.tflite");
            FileInputStream inputStream = new FileInputStream(modelFileDescriptor.getFileDescriptor());
            FileChannel fileChannel = inputStream.getChannel();
            long startOffset = modelFileDescriptor.getStartOffset();
            long declaredLength = modelFileDescriptor.getDeclaredLength();
            dynamic_model = new Interpreter(fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength));

            int inputTensorIndex = 0; // Modify accordingly based on your model
            int[] inputTensorShape = dynamic_model.getInputTensor(inputTensorIndex).shape();
            Log.i("DYNAMIC INPUT SHAPE", "Dynamic Input Tensor Shape: " + Arrays.toString(inputTensorShape));

            // Print output tensor shape
            int outputTensorIndex = 0; // Modify accordingly based on your model
            int[] outputTensorShape = dynamic_model.getOutputTensor(outputTensorIndex).shape();
            Log.i("DYNAMIC OUTPUT SHAPE","Dynamic Output Tensor Shape: " + Arrays.toString(outputTensorShape));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Run inference and return the result
    public String runInference(float[][][] inputData) {
        float[][] outputDynamic = new float[1][6];

        Log.e("DYNAMIC", "Before Model Run");
        dynamic_model.run(inputData, outputDynamic);
        Log.e("DYNAMIC", "After Model Run");

        String classification = dynamicTypes.get(getArgmax(outputDynamic[0]));

        Log.i("Dynamic classification", classification);
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
