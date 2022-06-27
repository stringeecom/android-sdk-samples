package com.stringee.videocallsample;

import com.stringee.StringeeClient;
import com.stringee.call.StringeeCall2;

import java.util.HashMap;
import java.util.Map;

public class Common {
    public static StringeeClient client;
    public static Map<String, StringeeCall2> callMap = new HashMap<>();
    public static boolean isInCall = false;
    public static int REQUEST_PERMISSION_CALL = 1;
}
