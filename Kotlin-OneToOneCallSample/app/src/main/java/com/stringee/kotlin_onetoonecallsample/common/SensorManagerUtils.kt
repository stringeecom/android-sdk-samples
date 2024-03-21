package com.stringee.kotlin_onetoonecallsample.common

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.PowerManager

class SensorManagerUtils private constructor(private val applicationContext: Context) : SensorEventListener {
    private var mSensorManager: SensorManager? = null
    private var mProximity: Sensor? = null
    private var powerManager: PowerManager? = null
    private var wakeLock: PowerManager.WakeLock? = null

    fun initialize(tag: String?): SensorManagerUtils? {
        if (mSensorManager == null) {
            mSensorManager = applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            if (mProximity == null) {
                mProximity = mSensorManager!!.getDefaultSensor(Sensor.TYPE_PROXIMITY)
            }
            mSensorManager!!.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL)
        }
        if (powerManager == null) {
            powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
            val screenLockValue = PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK
            if (wakeLock == null) {
                wakeLock = powerManager!!.newWakeLock(screenLockValue, tag)
            }
        }
        return instance
    }

    @SuppressLint("WakelockTimeout")
    fun turnOn() {
        if (!wakeLock!!.isHeld) {
            wakeLock!!.acquire()
        }
    }

    fun turnOff() {
        if (wakeLock!!.isHeld) {
            wakeLock!!.release()
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
        private var instance: SensorManagerUtils? = null
        fun getInstance(context: Context): SensorManagerUtils {
            return instance ?: synchronized(this) {
                instance
                    ?: SensorManagerUtils(context.applicationContext).also {
                        instance = it
                    }
            }
        }
    }
}
