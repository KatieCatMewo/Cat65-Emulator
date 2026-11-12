package org.kittykat.cat65.core;

import javafx.beans.property.FloatProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart;

import java.util.ArrayList;

public abstract class DebugData {
    public static final int VISUAL_AUDIO_BUFFER_SIZE = 2 * AudioManager.READ_BUFFER_SIZE;

    public static final float MIN_AUDIO_SCALE     = 1f;
    public static final float MAX_AUDIO_SCALE     = .1f;
    public static final float DEFAULT_AUDIO_SCALE = MIN_AUDIO_SCALE;
    public static final FloatProperty audioScaleProperty = new SimpleFloatProperty(DEFAULT_AUDIO_SCALE);
    public static volatile float audioScale = DEFAULT_AUDIO_SCALE;

    public  static volatile int  audioBufferFillSum   = 0;
    public  static volatile int  audioBufferFillCount = 0;
    private static float lastAudioBufferFillLevel = 0f;
    public  static final float[] audioBuffer = new float[VISUAL_AUDIO_BUFFER_SIZE];
    public  static int audioBufferSegment = 0;
    public  static final XYChart.Series<Number, Number> audioSeries = new XYChart.Series<>();

    public static volatile int  executedClocks            = 0;
    public static volatile long lastClockSpeedSampleTime  = System.nanoTime();
    public static final ArrayList<Double> lastClockSpeeds = new ArrayList<>();

    static {
        audioScaleProperty.addListener((obs, oldVal, newVal) -> audioScale = (float) newVal);

        ObservableList<XYChart.Data<Number, Number>> series = audioSeries.getData();
        series.clear();
        for (int s = 0; s < VISUAL_AUDIO_BUFFER_SIZE; s++) {
            series.add(new XYChart.Data<>(s, 0f));
        }
    }

    public static float getAudioBufferFillLevel() {
        int sum = audioBufferFillSum;
        int count = audioBufferFillCount;
        audioBufferFillSum = 0;
        audioBufferFillCount = 0;
        if (count != 0) lastAudioBufferFillLevel = sum / ((float) count);
        return lastAudioBufferFillLevel;
    }
    public static double getCurrentClockSpeed() {
        long   now         = System.nanoTime();
        long   elapsedTime = now - lastClockSpeedSampleTime;
        double clockSpeed  = executedClocks * 1_000_000_000d / elapsedTime;
        executedClocks           = 0;
        lastClockSpeedSampleTime = now;

        double clockSpeedAverage = clockSpeed;
        for (double pastSpeed : lastClockSpeeds) {
            clockSpeedAverage += pastSpeed;
        }
        lastClockSpeeds.add(clockSpeed);
        int sampleCount = lastClockSpeeds.size();
        clockSpeedAverage /= sampleCount;
        if (sampleCount > 10) lastClockSpeeds.removeFirst();

        return clockSpeedAverage;
    }

    public static void uiUpdate() {
        ObservableList<XYChart.Data<Number, Number>> series = audioSeries.getData();
        for (int s = 0; s < audioBuffer.length; s++) {
            series.get(s).setYValue(audioBuffer[s] / audioScale);
        }
    }
}
