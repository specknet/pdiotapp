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

        respeckQueue.add(
                new float[]{accelX, accelY, accelZ,
                        gyroX, gyroY, gyroZ
                });

        if (respeckQueue.size()>queueLimit){
           respeckQueue.remove();
        }
    }

    public Queue<float[]> getQueue(){return respeckQueue; }

}
