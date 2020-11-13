package com.specknet.pdiot.utils;

import androidx.annotation.NonNull;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class DelayRespeck implements Delayed {
    private RespeckData data;
    private long time;

    // Constructor
    public DelayRespeck(RespeckData data, long delayTime) {
        this.data = data;
        this.time = System.currentTimeMillis() + delayTime;
    }

    public RespeckData getData() {
        return this.data;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        long diff = time - System.currentTimeMillis();
        return unit.convert(diff, TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed obj) {
        if(this.time < ((DelayRespeck)obj).time) {
            return -1;
        }
        if(this.time > ((DelayRespeck)obj).time) {
            return 1;
        }
        return 0;
    }

    @NonNull
    @Override
    public String toString() {
        return "\n{"
                + "data= " + data.toString()
                +", time= " + time
                +"}";
    }
}
