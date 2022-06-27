package com.stringee.videocallsample;

import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.PowerManager;

public class SensorManagerUtils implements SensorEventListener {
    private static SensorManagerUtils instance;
    private SensorManager mSensorManager;
    private Sensor mProximity;
    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;
    private KeyguardLock lock;
    private Context context;

    public static SensorManagerUtils getInstance(Context context) {
        if (instance == null) {
            instance = new SensorManagerUtils(context);
        }
        return instance;
    }

    public SensorManagerUtils(Context context) {
        this.context = context;
    }

    public void acquireProximitySensor(String tag) {
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        mSensorManager.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL);

        powerManager = ((PowerManager) context.getSystemService(Context.POWER_SERVICE));

        int screenLockValue;

        if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            screenLockValue = PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK;
        } else {
            try {
                screenLockValue = PowerManager.class.getField("PROXIMITY_SCREEN_OFF_WAKE_LOCK").getInt(null);
            } catch (Exception exc) {
                screenLockValue = 32; // default integer value of PROXIMITY_SCREEN_OFF_WAKE_LOCK
            }
        }
        wakeLock = powerManager.newWakeLock(screenLockValue, tag);

        wakeLock.acquire();
    }

    public void disableKeyguard() {
        lock = ((KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE)).newKeyguardLock(Context.KEYGUARD_SERVICE);
        lock.disableKeyguard();
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

        if (lock != null) {
            lock.reenableKeyguard();
            lock = null;
        }

        if (instance != null) {
            instance = null;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        float value = sensorEvent.values[0];
        if (value == 0) {
            if (wakeLock != null && !wakeLock.isHeld()) {
                wakeLock.acquire();
            }
        } else {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
