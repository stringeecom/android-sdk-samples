package com.stringee.callpushnotificationsample.common;

import android.app.KeyguardManager.KeyguardLock;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

public class SensorManagerUtils implements SensorEventListener {
    private static volatile SensorManagerUtils instance;
    private static final Object lock = new Object();
    private SensorManager mSensorManager;
    private Sensor mProximity;
    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;
    private KeyguardLock keyguardLock;
    private final Context context;

    public static synchronized SensorManagerUtils getInstance(Context context) {
        if (instance == null) {
            synchronized (lock) {
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

            int screenLockValue;

            screenLockValue = PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK;
            if (wakeLock == null) {
                wakeLock = powerManager.newWakeLock(screenLockValue, tag);
            }
        }
        return instance;
    }

    public void turnOn() {
        if (wakeLock != null) {
            if (!wakeLock.isHeld()) {
                wakeLock.acquire();
            }
        }

        disableKeyguard();
    }

    public void turnOff() {
        if (wakeLock != null) {
            if (wakeLock.isHeld()) {
                wakeLock.release();
            }
        }

        enableKeyguard();
    }

    public void disableKeyguard() {
        if (keyguardLock != null) {
            keyguardLock.disableKeyguard();
        }
    }

    public void enableKeyguard() {
        if (keyguardLock != null) {
            keyguardLock.reenableKeyguard();
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

        if (keyguardLock != null) {
            keyguardLock.reenableKeyguard();
            keyguardLock = null;
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
