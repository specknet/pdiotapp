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

public class StaticInference {
    private Context context;
    private Interpreter static_model;
    private Map<Integer, String> staticTypes;

    public StaticInference(Context context) {

        this.context = context.getApplicationContext();

        staticTypes = new HashMap<>();
        staticTypes.put(0, "sitting/standing");
        staticTypes.put(1, "lyingLeft");
        staticTypes.put(2, "lyingRight");
        staticTypes.put(3, "lyingBack");
        staticTypes.put(4, "lyingStomach");
    }

    public void loadModel() {

        try {
            AssetManager assetManager = context.getAssets();
            AssetFileDescriptor modelFileDescriptor = assetManager.openFd("static_model.tflite");
            FileInputStream inputStream = new FileInputStream(modelFileDescriptor.getFileDescriptor());
            FileChannel fileChannel = inputStream.getChannel();
            long startOffset = modelFileDescriptor.getStartOffset();
            long declaredLength = modelFileDescriptor.getDeclaredLength();
            static_model = new Interpreter(fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength));

            int inputTensorIndex = 0; // Modify accordingly based on your model
            int[] inputTensorShape = static_model.getInputTensor(inputTensorIndex).shape();
            Log.i("STATIC INPUT SHAPE", "Static Input Tensor Shape: " + Arrays.toString(inputTensorShape));

            // Print output tensor shape
            int outputTensorIndex = 0; // Modify accordingly based on your model
            int[] outputTensorShape = static_model.getOutputTensor(outputTensorIndex).shape();
            Log.i("STATIC OUTPUT SHAPE","Static Output Tensor Shape: " + Arrays.toString(outputTensorShape));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Run inference and return the result
    public String runInference(float[][][] inputData) {
        float[][] outputStatic = new float[1][5];

        Log.e("STATIC", "Before Model Run");
        static_model.run(inputData, outputStatic);
        Log.e("STATIC", "After Model Run");

        String classification = staticTypes.get(getArgmax(outputStatic[0]));

        Log.i("Static classification", classification);
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
