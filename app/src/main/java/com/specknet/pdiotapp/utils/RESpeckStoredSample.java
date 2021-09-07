package com.specknet.pdiotapp.utils;

/**
 * Created by esotsm54 on 7/25/2016.
 */
public class RESpeckStoredSample {
    private Long phoneTimestamp;
    private Long RESpeckTimestamp;
    private Integer sequenceNumber;
    private Integer numberOfBreaths;
    private Float meanBreathingRate;
    private Float sdBreathingRate;
    private Float activityLevel;

    public RESpeckStoredSample(Long phoneTimestamp, Long RESpeckTimestamp, Integer sequenceNumber, Integer numberOfBreaths, Float meanBreathingRate, Float sdBreathingRate, Float activityLevel) {
        super();
        this.phoneTimestamp = phoneTimestamp;
        this.RESpeckTimestamp = RESpeckTimestamp;
        this.sequenceNumber = sequenceNumber;
        this.numberOfBreaths = numberOfBreaths;
        this.meanBreathingRate = meanBreathingRate;
        this.sdBreathingRate = sdBreathingRate;
        this.activityLevel = activityLevel;
    }

    @Override
    public String toString() {
        return "RESpeckStoredSample (" + this.phoneTimestamp + ", " +  this.RESpeckTimestamp + ", " + this.sequenceNumber + ", " + this.numberOfBreaths + ", " + this.meanBreathingRate + ", " + this.sdBreathingRate + ", " + this.activityLevel + ")";
    }

    public Long getPhoneTimestamp() {return this.phoneTimestamp;};
    public Long getRESpeckTimestamp() { return this.RESpeckTimestamp;};
    public Integer getSequenceNumber() { return this.sequenceNumber;};
    public Integer getNumberOfBreaths() {return this.numberOfBreaths;};
    public Float getAverageBreathingRate(){ return this.meanBreathingRate; };
    public Float getStdBreathingRate(){ return this.sdBreathingRate; };
    public Float getActivityLevel(){ return this.activityLevel; };
}