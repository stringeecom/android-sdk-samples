package com.stringee.softphone.common;

import android.content.Context;
import android.media.AudioManager;

import com.stringee.call.CallType;
import com.stringee.call.StringeeCall;
import com.stringee.listener.StatusListener;

import org.json.JSONObject;

/** Bridges the legacy Softphone UI to the callback-based Stringee SDK 2.x API. */
public final class CallSdkCompat {
    private CallSdkCompat() { }

    private static final StatusListener IGNORE_RESULT = new StatusListener() {
        @Override public void onSuccess() { }
    };

    public static void makeCall(StringeeCall call) { call.makeCall(IGNORE_RESULT); }
    public static void answer(StringeeCall call) { call.answer(IGNORE_RESULT); }
    public static void reject(StringeeCall call) { call.reject(IGNORE_RESULT); }
    public static void hangup(StringeeCall call) { call.hangup(IGNORE_RESULT); }
    public static void sendCallInfo(StringeeCall call, JSONObject data) {
        call.sendCallInfo(data, IGNORE_RESULT);
    }
    public static boolean isPhoneToApp(StringeeCall call) {
        return call != null && call.getCallType() == CallType.PHONE_TO_APP;
    }
    public static void setSpeakerphoneOn(Context context, boolean enabled) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            audioManager.setSpeakerphoneOn(enabled);
        }
    }
}
