package com.stringee.apptoappcallsample.common;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.PowerManager;

@SuppressLint("WakelockTimeout")
public class SensorManagerUtils implements SensorEventListener {
    private static volatile SensorManagerUtils instance;
    private SensorManager mSensorManager;
    private Sensor mProximity;
    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;
    private final Context context;

    public static SensorManagerUtils getInstance(Context context) {
        if (instance == null) {
            synchronized (SensorManagerUtils.class) {
                if (instance == null) {
                    instance = new SensorManagerUtils(context);
                }
            }
        }
        return instance;
    }

    public SensorManagerUtils(Context context) {
        this.context = context.getApplicationContext();
    }

    public SensorManagerUtils initialize(String tag) {
        if (mSensorManager == null) {
            mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            if (mProximity == null) {
                mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
            }
            mSensorManager.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL);
        }

        if (powerManager == null) {
            powerManager = ((PowerManager) context.getSystemService(Context.POWER_SERVICE));

            int screenLockValue = PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK;
            if (wakeLock == null) {
                wakeLock = powerManager.newWakeLock(screenLockValue, tag);
            }
        }
        return instance;
    }

    public void turnOn() {
        if (!wakeLock.isHeld()) {
            wakeLock.acquire();
        }
    }

    public void turnOff() {
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
    }


    public void releaseSensor() {
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this);
            mSensorManager = null;
        }

        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
        }

        if (instance != null) {
            instance = null;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
