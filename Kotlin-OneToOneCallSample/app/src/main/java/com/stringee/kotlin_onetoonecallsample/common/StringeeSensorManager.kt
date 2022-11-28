package com.stringee.kotlin_onetoonecallsample.common

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.PowerManager
import android.os.PowerManager.WakeLock

class StringeeSensorManager(context: Context) :
    SensorEventListener {
    private var mSensorManager: SensorManager? = null
    private var mProximity: Sensor? = null
    private var powerManager: PowerManager? = null
    private var wakeLock: WakeLock? = null
    private val context: Context

    init {
        this.context = context.applicationContext
    }

    fun initialize(): StringeeSensorManager? {
        if (mSensorManager == null) {
            mSensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            if (mProximity == null) {
                mProximity = mSensorManager!!.getDefaultSensor(Sensor.TYPE_PROXIMITY)
            }
            mSensorManager!!.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL)
        }
        if (powerManager == null) {
            powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val screenLockValue: Int
            screenLockValue = PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK
            if (wakeLock == null) {
                wakeLock = powerManager!!.newWakeLock(screenLockValue, context.packageName)
            }
        }
        return instance
    }

    fun turnOn() {
        if (wakeLock != null) {
            if (!wakeLock!!.isHeld) {
                wakeLock!!.acquire()
            }
        }
    }

    fun turnOff() {
        if (wakeLock != null) {
            if (wakeLock!!.isHeld) {
                wakeLock!!.release()
            }
        }
    }

    fun releaseSensor() {
        if (mSensorManager != null) {
            mSensorManager!!.unregisterListener(this)
            mSensorManager = null
        }
        if (wakeLock != null && wakeLock!!.isHeld) {
            wakeLock!!.release()
            wakeLock = null
        }
        if (instance != null) {
            instance = null
        }
    }

    override fun onSensorChanged(sensorEvent: SensorEvent) {}
    override fun onAccuracyChanged(sensor: Sensor, i: Int) {}

    companion object {
        @Volatile
        private var instance: StringeeSensorManager? = null
        private val lock = Any()
        @Synchronized
        fun getInstance(context: Context): StringeeSensorManager? {
            if (instance == null) {
                synchronized(lock) {
                    if (instance == null) {
                        instance = StringeeSensorManager(context)
                    }
                }
            }
            return instance
        }
    }
}