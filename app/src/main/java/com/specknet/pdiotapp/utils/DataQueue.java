package com.specknet.pdiotapp.utils;

import java.util.LinkedList;
import java.util.Queue;

public class DataQueue {
    public Queue<float[]> respeckQueue;
    final int queueLimit;

    public DataQueue(int queueLimit){
        respeckQueue = new LinkedList<>();
        this.queueLimit = queueLimit;
    }

    public void add(
            float accelX, float accelY, float accelZ,
            float gyroX, float gyroY, float gyroZ){
        // Need to make sure that array is of right size

        float[] data = new float[]{accelX, accelY, accelZ,
                gyroX, gyroY, gyroZ};

        if (data==null){
            int x=5+5;
        }

        respeckQueue.add(data);

        if (respeckQueue.size()>queueLimit){
           respeckQueue.remove();
        }
    }

    public float[][][] getList(){
        float[][][] formattedList = new float[1][queueLimit][6];
        formattedList[0] = respeckQueue.toArray(new float[queueLimit][6]);
        return formattedList;
    }

    public float[][][] getSpecificList(int size){
        float[][] fullList = new float[queueLimit][6];
        float[][][] specificList = new float[1][size][6];
        fullList = respeckQueue.toArray(new float[queueLimit][6]);

        for (int i = 0; i<size; i++){
            specificList[0][i] = fullList[i+queueLimit-size];
        }
        return specificList;
    }

    public int getLength(){return respeckQueue.size();}
}
