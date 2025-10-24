package com.aura.starter.steps;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class StepTracker implements SensorEventListener {

    private final SensorManager sm;
    private final Sensor counter;
    private float base = -1f;
    private int todaySteps = 0;
    private final MutableLiveData<Integer> live = new MutableLiveData<>(0);

    public StepTracker(Context ctx) {
        sm = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
        counter = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
    }

    public void start() {
        if (counter != null) {
            sm.registerListener(this, counter, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    public void stop() {
        sm.unregisterListener(this);
    }

    public LiveData<Integer> stepsLive() { return live; }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float v = event.values[0];
        if (base < 0) base = v;
        todaySteps = Math.max(0, Math.round(v - base));
        live.postValue(todaySteps);
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
