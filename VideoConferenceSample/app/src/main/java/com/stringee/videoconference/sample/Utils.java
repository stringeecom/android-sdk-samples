package com.stringee.videoconference.sample;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;

import com.stringee.video.RemoteParticipant;

import java.util.List;

public class Utils {
    public static String getRoomName(String localId, List<RemoteParticipant> remoteParticipantList) {
        if (localId == null) {
            return "";
        }
        String roomName = localId;
        for (RemoteParticipant remoteParticipant : remoteParticipantList) {
            roomName = roomName + ", " + remoteParticipant.getId().trim();
        }
        return roomName;
    }

    public static String getNewRoomName(String oldRoomName, String participantId, String type) {
        if (oldRoomName == null) {
            return "";
        }
        String newRoomName = "";
        switch (type) {
            case "add":
                newRoomName = (oldRoomName + ", " + participantId).trim();
                break;
            case "remove":
                newRoomName = oldRoomName.replace((", " + participantId), "");
                break;
        }
        return newRoomName;
    }

    public static int dpToPx(Context context, int dp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int px = Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return px;
    }

    public static void postDelay1s(Runnable runnable) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(runnable, 1500);
    }

    public static void runOnUIThreat(Runnable runnable) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(runnable);
    }
}
