package com.stringee.apptoappcallsample.common;

import android.media.Ringtone;

import com.stringee.call.StringeeCall;
import com.stringee.call.StringeeCall2;

import java.util.HashMap;
import java.util.Map;

public class Common {
    public static Map<String, StringeeCall> callsMap = new HashMap<>();
    public static Map<String, StringeeCall2> calls2Map = new HashMap<>();
    public static StringeeAudioManager audioManager;
    public static boolean isInCall = false;
    public static Ringtone ringtone;
    public static boolean isAppInBackground = false;
}
