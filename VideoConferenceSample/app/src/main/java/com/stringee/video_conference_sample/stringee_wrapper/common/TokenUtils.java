package com.stringee.video_conference_sample.stringee_wrapper.common;

import android.content.Context;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.stringee.exception.StringeeError;
import com.stringee.messaging.listeners.CallbackListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class TokenUtils {
    private static volatile TokenUtils instance;
    private static final String KEY_SID = "SKE1RdUtUaYxNaQQ4Wr15qF1zUJuQdAaVT";
    private static final String KEY_SECRET = "M3Fkcmswc1hvYllmOGR0VzY5TXhUcXZxWFJ2OHVudVc=";
    private static final int EXPIRE_TIME = 60 * 60 * 24 * 365;


    public static TokenUtils getInstance() {
        if (instance == null) {
            synchronized (TokenUtils.class) {
                if (instance == null) {
                    instance = new TokenUtils();
                }
            }
        }
        return instance;
    }


    public String genAccessToken(String userId) {
        userId = userId.trim().replace(" ", "_");
        try {
            Algorithm algorithmHS = Algorithm.HMAC256(KEY_SECRET);

            Map<String, Object> headerClaims = new HashMap<>();
            headerClaims.put("typ", "JWT");
            headerClaims.put("alg", "HS256");
            headerClaims.put("cty", "stringee-api;v=1");

            long exp = System.currentTimeMillis() + EXPIRE_TIME * 1000L;

            return JWT.create().withHeader(headerClaims)
                    .withClaim("jti", KEY_SID + "-" + System.currentTimeMillis())
                    .withClaim("iss", KEY_SID)
                    .withClaim("userId", userId)
                    .withExpiresAt(new Date(exp))
                    .sign(algorithmHS);
        } catch (Exception ex) {
            Utils.reportException(Utils.class, ex);
        }
        return null;
    }

    private String genAuthToken() {
        try {
            Algorithm algorithmHS = Algorithm.HMAC256(KEY_SECRET);

            Map<String, Object> headerClaims = new HashMap<>();
            headerClaims.put("typ", "JWT");
            headerClaims.put("alg", "HS256");
            headerClaims.put("cty", "stringee-api;v=1");

            long exp = System.currentTimeMillis() + EXPIRE_TIME * 1000L;

            return JWT.create().withHeader(headerClaims)
                    .withClaim("jti", KEY_SID + "-" + System.currentTimeMillis())
                    .withClaim("iss", KEY_SID)
                    .withClaim("rest_api", true)
                    .withExpiresAt(new Date(exp))
                    .sign(algorithmHS);
        } catch (Exception ex) {
            Utils.reportException(Utils.class, ex);
        }

        return null;
    }

    private String genRoomToken(String roomId) {
        try {
            Algorithm algorithmHS = Algorithm.HMAC256(KEY_SECRET);

            Map<String, Object> headerClaims = new HashMap<>();
            headerClaims.put("typ", "JWT");
            headerClaims.put("alg", "HS256");
            headerClaims.put("cty", "stringee-api;v=1");

            Map<String, Object> permissionsClaims = new HashMap<>();
            permissionsClaims.put("publish", true);
            permissionsClaims.put("subscribe", true);
            permissionsClaims.put("control_room", true);

            long exp = System.currentTimeMillis() + EXPIRE_TIME * 1000L;

            return JWT.create().withHeader(headerClaims)
                    .withClaim("jti", KEY_SID + "-" + System.currentTimeMillis())
                    .withClaim("iss", KEY_SID)
                    .withClaim("roomId", roomId)
                    .withClaim("permissions", permissionsClaims)
                    .withExpiresAt(new Date(exp))
                    .sign(algorithmHS);
        } catch (Exception ex) {
            Utils.reportException(Utils.class, ex);
        }

        return null;
    }

    public void createRoom(Context context, String roomName, CallbackListener<String> listener) {
        String apiUrl = "https://api.stringee.com/v1/room2/create";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("name", roomName);
        requestBody.put("uniqueName", roomName);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, apiUrl, new JSONObject(requestBody), response -> {
            try {
                int r = response.getInt("r");
                if (r == 2 || r == 0) {
                    String roomToken = genRoomToken(response.getString("roomId"));
                    if (listener != null) {
                        listener.onSuccess(roomToken);
                    }
                } else {
                    if (listener != null) {
                        listener.onError(new StringeeError());
                    }
                }
            } catch (JSONException e) {
                Utils.reportException(TokenUtils.class, e);
                if (listener != null) {
                    listener.onError(new StringeeError(-1, e.getMessage()));
                }
            }
        }, volleyError -> {
            if (listener != null) {
                listener.onError(new StringeeError(-1, volleyError.getMessage()));
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<>();
                params.put("X-STRINGEE-AUTH", genAuthToken());
                return params;
            }
        };
        request.setRetryPolicy(new DefaultRetryPolicy(60000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        RequestQueue queue = Volley.newRequestQueue(context);
        queue.add(request);
    }
}
