package com.stringee.apptoappcallsample;

import com.stringee.StringeeClient;
import com.stringee.call.StringeeCall;
import com.stringee.call.StringeeCall2;

import java.util.HashMap;
import java.util.Map;

public class Common {
    public static StringeeClient client;
    public static Map<String, StringeeCall> callsMap = new HashMap<>();
    public static Map<String, StringeeCall2> calls2Map = new HashMap<>();
    public static boolean isInCall = false;
    public static int REQUEST_PERMISSION_CALL = 1;
    public static String TAG = "Stringee";
}
