package com.stringee.videoconference.sample;

import com.stringee.video.RemoteParticipant;

import java.util.List;

public class Utils {
    public static String getRoomName(String localId, List<RemoteParticipant> remoteParticipantList) {
        if (localId == null) {
            return "";
        }
        StringBuilder roomName = new StringBuilder(localId);
        for (RemoteParticipant remoteParticipant : remoteParticipantList) {
            roomName.append(", ").append(remoteParticipant.getId().trim());
        }
        return roomName.toString();
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
}
