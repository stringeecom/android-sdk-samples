package com.stringee.stringeechatuikit.common;

import android.content.SharedPreferences;
import android.graphics.Typeface;

import com.stringee.StringeeClient;
import com.stringee.call.StringeeCall;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by luannguyen on 3/18/2016.
 */
public class Common {
    public static SharedPreferences preferences;
    public static StringeeClient client;
    public static Map<String, StringeeCall> callsMap = new HashMap<>();
    public static String currentConvId;
    public static boolean isChatting = false;
    public static boolean isChangeListenerSet = false;
    public static Typeface boldType;
    public static Typeface normalType;
    public static Typeface iconTypeface;
    public static List<String> stickerDirectories = new ArrayList<>();
}
