package com.specknet.pdiotapp.utils;

import org.tensorflow.lite.Interpreter;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.AssetFileDescriptor;
import android.util.Log;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;

public class Inference {
    private Context context;
    private Interpreter breathing_model;
    private Interpreter activity_model;
    private Map<Integer, String> breathingTypes;
    private Map<Integer, String> activityTypes;

    public Inference(Context context) {
        breathingTypes = new HashMap<>();
        breathingTypes.put(0, "Normal");
        breathingTypes.put(1, "Coughing");
        breathingTypes.put(2, "Singing");
        breathingTypes.put(3, "Eating");
        breathingTypes.put(4, "Hyperventilating");
        breathingTypes.put(5, "Laughing");
        breathingTypes.put(6, "Talking");

        activityTypes = new HashMap<>();
        activityTypes.put(0, "Sitting");
        activityTypes.put(1, "Standing");
        activityTypes.put(2, "Lying down back");
        activityTypes.put(3, "Lying down right");
        activityTypes.put(4, "Lying down on left");
        activityTypes.put(5, "Lying down on stomach");
        activityTypes.put(6, "Ascending stairs");
        activityTypes.put(7, "Descending stairs");
        activityTypes.put(8, "Running");
        activityTypes.put(9, "Normal walking");
        activityTypes.put(10, "Shuffle walking");
        activityTypes.put(11, "Miscellaneous movements");

        this.context = context.getApplicationContext();
    }

    public void loadModel() {

        try {
            AssetManager assetManager = context.getAssets();
            AssetFileDescriptor modelFileDescriptor = assetManager.openFd("breathing_model.tflite");
            FileInputStream inputStream = new FileInputStream(modelFileDescriptor.getFileDescriptor());
            FileChannel fileChannel = inputStream.getChannel();
            long startOffset = modelFileDescriptor.getStartOffset();
            long declaredLength = modelFileDescriptor.getDeclaredLength();
            breathing_model = new Interpreter(fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength));

            modelFileDescriptor = assetManager.openFd("activity_model.tflite");
            inputStream = new FileInputStream(modelFileDescriptor.getFileDescriptor());
            fileChannel = inputStream.getChannel();
            startOffset = modelFileDescriptor.getStartOffset();
            declaredLength = modelFileDescriptor.getDeclaredLength();
            activity_model = new Interpreter(fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Run inference and return the result
    public String runInference(float[] inputData) {
        float[][] outputBreathing = new float[1][7];
        float[][] outputActivity = new float[1][12];

        breathing_model.run(inputData, outputBreathing);
        activity_model.run(inputData, outputActivity);

        String breathingType = breathingTypes.get(getArgmax(outputBreathing[0]));
        String activityType = activityTypes.get(getArgmax(outputActivity[0]));
        String classification = activityType + " " + breathingType;

        Log.i("Respeck classification", classification);
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
