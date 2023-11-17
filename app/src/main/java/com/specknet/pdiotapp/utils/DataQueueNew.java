package com.specknet.pdiotapp.utils;

import java.util.LinkedList;
import java.util.Queue;

public class DataQueueNew {
    public Queue<float[]> respeckQueue;
    final int queueLimit;

    public DataQueueNew(int queueLimit){
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
        float[][][] formatedList = new float[1][15][6];
        formatedList[0] = respeckQueue.toArray(new float[queueLimit][6]);
        return formatedList;
    }

    public int getLength(){return respeckQueue.size();}
}
