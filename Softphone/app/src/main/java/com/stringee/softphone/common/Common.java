package com.stringee.softphone.common;

import android.content.Context;

import com.stringee.StringeeClient;
import com.stringee.call.StringeeCall;
import com.stringee.softphone.database.MessageHandler;
import com.stringee.softphone.service.CheckAppInBackgroundThread;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by luannguyen on 7/27/2017.
 */

public class Common {

    public static MessageHandler messageDb;
    public static int userId;
    public static int messageId = (int) (System.currentTimeMillis() / 1000) - 1394583259;
    public static Context context;
    public static boolean alreadyConnected;
    public static boolean isInCall;
    public static StringeeClient client;
    public static boolean isVisible;
    public static long lastTime;
    public static CheckAppInBackgroundThread checkAppInBackgroundThread;
    public static boolean isConnecting;
    public static Map<String, StringeeCall> callMap = new HashMap<>();
}
