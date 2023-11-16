package com.specknet.pdiotapp.utils;

import java.util.Queue;
import java.util.LinkedList;

public class DataQueue{
    public Queue<Float> respeckQueue;
    final int queueLimit;
    private float dataMean;
    private float dataStd;
    private float sum;

    public DataQueue(int queueLimit){
        respeckQueue = new LinkedList<>();
        sum = 0;

        this.queueLimit = queueLimit;
    }

    public void add(float respeckData){
        // Need to make sure that array is of right size

        respeckQueue.add(respeckData);
        sum += respeckData;

        if (respeckQueue.size()>queueLimit){
           float removedValue = respeckQueue.remove();
           sum -= removedValue;
        }
    }

    public void calculateMeanAndStd(){
        dataMean = sum/respeckQueue.size();
        float squareSum = 0;

        for (float value: respeckQueue){
            squareSum += Math.pow( value - dataMean ,2);
        }

        if (respeckQueue.size()==1){
            dataStd = 0;
        }
        else {
            dataStd = (float) (Math.sqrt(squareSum / (respeckQueue.size() - 1)));
        }
    }

    public float getMean(){
        return dataMean;
    }

    public float getStd(){
        return dataStd;
    }

    public Queue<Float> getQueue(){return respeckQueue; }

}
