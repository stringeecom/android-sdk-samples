package com.stringee.softphone.activity;

import android.Manifest;
import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.stringee.call.StringeeCall;
import com.stringee.softphone.R;
import com.stringee.softphone.common.Common;
import com.stringee.softphone.common.Constant;
import com.stringee.softphone.common.DataHandler;
import com.stringee.softphone.common.DateTimeUtils;
import com.stringee.softphone.common.Notify;
import com.stringee.softphone.common.NotifyUtils;
import com.stringee.softphone.common.StringeeBluetoothManager;
import com.stringee.softphone.common.Utils;
import com.stringee.softphone.model.Contact;
import com.stringee.softphone.model.Message;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by luannguyen on 8/19/2017.
 */

public class IncomingCallActivity extends MActivity implements View.OnClickListener, SensorEventListener {

    private TextView tvName;
    private TextView tvPhone;
    private TextView tvState;
    private View vSpeaker;
    private View vBottom;
    private View vEnd;
    private ImageButton btnSpeaker;
    private ImageButton btnMute;
    private ImageButton btnVideo;
    private ImageButton btnEndCall;
    private View vBlack;
    private ImageView imNetwork;
    private RelativeLayout vBackground;
    private FrameLayout vLocal;
    private FrameLayout vRemote;
    private View vTop;
    private View vMid;
    private ImageButton btnSwitch;

    private String phoneNumber;
    private String phoneNo;
    private String name;
    private boolean isSpeaker = false;
    private boolean isMute = false;
    private boolean isVideoOn = false;
    private long startTime;
    private TimerTask timerTask;
    private Timer timer;
    private Message mMessage;
    public static StringeeCall incomingCall;
    private Timer statsTimer;
    private TimerTask statsTimerTask;

    private Vibrator vibrator;
    private SensorManager mSensorManager;
    private Sensor mProximity;
    private MediaPlayer endPlayer;
    private Handler handler = new Handler();
    private AudioManager audioManager;
    private Ringtone ringtone;

    public static final int REQUEST_PERMISSION_CALLIN = 1;
    public static final int REQUEST_PERMISSION_CAMERA = 2;
    public static final int REQUEST_PERMISSION_CAMERA_WHEN_ANSWER = 3;

    private final String ACTION_SAVE_CALL = "save_call";
    private final String ACTION_UPDATE_CALL = "update_call";

    private double mPrevCallTimestamp = 0;
    private long mPrevCallBytes = 0;
    private long mCallBw = 0;

    private boolean isFromPush;

    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;
    private int notificationId = 10021993;

    private StringeeCall.SignalingState mState;
    private BroadcastReceiver endCallReceiver;

    private boolean isShowControl = true;
    private boolean isCanHide = false;

    private StringeeBluetoothManager bluetoothManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_income_call);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window w = getWindow();
            w.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            w.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        } else {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        Common.isInCall = true;

        KeyguardManager.KeyguardLock lock = ((KeyguardManager) getSystemService(KEYGUARD_SERVICE)).newKeyguardLock(KEYGUARD_SERVICE);
        lock.disableKeyguard();

        powerManager = ((PowerManager) getSystemService(Context.POWER_SERVICE));
        wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, getLocalClassName());

        wakeLock.acquire();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        mSensorManager.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        audioManager.setSpeakerphoneOn(true);

        bluetoothManager = StringeeBluetoothManager.create(this);
        bluetoothManager.start();

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String callId = extras.getString(Constant.PARAM_CALL_ID);
            isFromPush = extras.getBoolean(Constant.PARAM_FROM_PUSH);
            incomingCall = Common.callMap.get(callId);
        }

        if (incomingCall == null) {
            return;
        }
        phoneNumber = incomingCall.getFrom();
        isSpeaker = incomingCall.isVideoCall();

        List<Contact> deviceContacts = Utils.getContactsFromDevice(this);
        for (int i = 0; i < deviceContacts.size(); i++) {
            Contact contact = deviceContacts.get(i);
            if (Utils.formatPhone(phoneNumber).equals(Utils.formatPhone(contact.getPhone()))) {
                name = contact.getName();
                phoneNo = contact.getPhoneNo();
                break;
            }
        }

        tvName = (TextView) findViewById(R.id.tv_name);
        tvPhone = (TextView) findViewById(R.id.tv_phone);
        if (name != null) {
            tvName.setText(name);
            tvPhone.setText(phoneNumber);
        } else {
            tvName.setText(phoneNumber);
            tvPhone.setVisibility(View.GONE);
        }
        tvState = (TextView) findViewById(R.id.tv_status);

        vEnd = findViewById(R.id.v_end);
        vSpeaker = findViewById(R.id.v_speaker);

        btnSpeaker = (ImageButton) findViewById(R.id.btn_speaker);
        btnSpeaker.setOnClickListener(this);
        if (isSpeaker) {
            btnSpeaker.setImageResource(R.drawable.ic_speaker_on);
        } else {
            btnSpeaker.setImageResource(R.drawable.ic_speaker_off);
        }
        btnMute = (ImageButton) findViewById(R.id.btn_mute);
        btnMute.setOnClickListener(this);
        btnEndCall = (ImageButton) findViewById(R.id.btn_end_call);
        btnEndCall.setOnClickListener(this);

        ImageButton btnAnswer = (ImageButton) findViewById(R.id.btn_answer);
        btnAnswer.setOnClickListener(this);

        ImageButton btnReject = (ImageButton) findViewById(R.id.btn_reject);
        btnReject.setOnClickListener(this);

        View vVideo = findViewById(R.id.v_video);
        if (incomingCall.isPhoneToAppCall()) {
            vVideo.setVisibility(View.GONE);
        }

        btnVideo = (ImageButton) findViewById(R.id.btn_video);
        btnVideo.setOnClickListener(this);

        isVideoOn = incomingCall.isVideoCall();
        if (isVideoOn) {
            btnVideo.setImageResource(R.drawable.ic_video);
        } else {
            btnVideo.setImageResource(R.drawable.ic_video_off);
        }

        vBlack = findViewById(R.id.v_black);
        vBlack.setOnClickListener(null);

        imNetwork = (ImageView) findViewById(R.id.im_network);
        vBackground = (RelativeLayout) findViewById(R.id.v_background);
        vBottom = findViewById(R.id.v_bottom);

        vTop = findViewById(R.id.v_top);
        vMid = findViewById(R.id.v_mid);

        vLocal = (FrameLayout) findViewById(R.id.v_local);
        vRemote = (FrameLayout) findViewById(R.id.v_remote);

        btnSwitch = (ImageButton) findViewById(R.id.btn_switch);
        btnSwitch.setOnClickListener(this);
        if (isVideoOn) {
            btnSwitch.setVisibility(View.VISIBLE);
        }

        registerReceiver();

        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        ringtone = RingtoneManager.getRingtone(getApplicationContext(), notification);
        ringtone.play();

        endPlayer = MediaPlayer.create(this, R.raw.call_end);
        endPlayer.setVolume(1.0f, 1.0f);
        endPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        endPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                endPlayer.release();
                endPlayer = null;
            }
        });

        String content = name;
        if (name == null) {
            name = phoneNumber;
        }
        NotifyUtils.showCallNotify(this, content, new Intent(this, IncomingCallActivity.class), notificationId);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            List<String> lstPermissions = new ArrayList<>();

            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                lstPermissions.add(Manifest.permission.RECORD_AUDIO);
            }

            if (incomingCall.isVideoCall()) {
                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    lstPermissions.add(Manifest.permission.CAMERA);
                }
            }

            if (lstPermissions.size() > 0) {
                String[] permissions = new String[lstPermissions.size()];
                for (int i = 0; i < lstPermissions.size(); i++) {
                    permissions[i] = lstPermissions.get(i);
                }
                ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSION_CALLIN);
                return;
            }
        }
        saveCall();
        makeCall();
    }

    @Override
    public void onBackPressed() {
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Common.isInCall = false;
        unregisterReceiver();
        mSensorManager.unregisterListener(this);
        NotificationManager nm = (NotificationManager) getSystemService
                (NOTIFICATION_SERVICE);
        nm.cancel(notificationId);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        boolean isGranted = false;
        if (grantResults.length > 0) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    isGranted = false;
                    break;
                } else {
                    isGranted = true;
                }
            }
        }
        switch (requestCode) {
            case REQUEST_PERMISSION_CALLIN:
                if (!isGranted) {
                    Utils.reportMessage(this, R.string.recording_required);
                    if (ringtone != null && ringtone.isPlaying()) {
                        ringtone.stop();
                        ringtone = null;
                    }
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (isFromPush) {
                                NotifyUtils.notifyEndCall();
                            }
                            Common.isInCall = false;
                            endCall(0);
                        }
                    }, 1000);
                    return;
                } else {
                    saveCall();
                    makeCall();
                }
                break;

            case REQUEST_PERMISSION_CAMERA:
                if (isGranted) {
                    enableOrDisableVideo();
                }
                break;
            case REQUEST_PERMISSION_CAMERA_WHEN_ANSWER:
                if (isGranted) {
                    acceptCameraRequest();
                }
                break;
        }
    }

    private void makeCall() {
        incomingCall.setCallListener(new StringeeCall.StringeeCallListener() {
            @Override
            public void onSignalingStateChange(StringeeCall stringeeCall, final StringeeCall.SignalingState signalingState, String reason, int sipCode, String sipReason) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mState = signalingState;
                        if (mState == StringeeCall.SignalingState.ENDED) {
                            tvState.setText(R.string.call_ended);
                            long duration = 0;
                            if (startTime > 0) {
                                duration = System.currentTimeMillis() - startTime;
                            }
                            endCall(duration);
                        }
                    }
                });
            }

            @Override
            public void onError(StringeeCall stringeeCall, int i, String s) {
                Log.e("Stringee", s);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (incomingCall != null) {
                            Log.e("Stringee", "==== Loi thi end thoi");
                            endCall(0);
                        }
                    }
                });
            }

            @Override
            public void onHandledOnAnotherDevice(StringeeCall stringeeCall, final StringeeCall.SignalingState signalingState, String desc) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        switch (signalingState) {
                            case ANSWERED:
                            case BUSY:
                            case ENDED:
                                Utils.reportMessage(IncomingCallActivity.this, R.string.msg_handled_on_another_device);
                                endCall(0);
                                break;
                        }
                    }
                });

            }

            @Override
            public void onMediaStateChange(StringeeCall stringeeCall, final StringeeCall.MediaState mediaState) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mediaState == StringeeCall.MediaState.CONNECTED) {
                            if (mState == StringeeCall.SignalingState.ANSWERED) {
                                if (startTime > 0) {
                                    return;
                                }
                                startTime = System.currentTimeMillis();
                                // Connected
                                vibrator.vibrate(500);
                                timer = new Timer();
                                timerTask = new TimerTask() {

                                    @Override
                                    public void run() {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                tvState.setText(DateTimeUtils.getCallTime(System
                                                                .currentTimeMillis(),
                                                        startTime));
                                            }
                                        });
                                    }
                                };
                                timer.schedule(timerTask, 0, 1000);

                                statsTimer = new Timer();
                                statsTimerTask = new TimerTask() {

                                    @Override
                                    public void run() {

                                        incomingCall.getStats(new StringeeCall.CallStatsListener() {
                                            @Override
                                            public void onCallStats(final StringeeCall.StringeeCallStats statsReport) {
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        checkCallStats(statsReport);
                                                    }
                                                });
                                            }
                                        });
                                    }
                                };
                                statsTimer.schedule(statsTimerTask, 0, 2000);

                                if (incomingCall.isVideoCall()) {
                                    vMid.setVisibility(View.GONE);
                                    vTop.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            isShowControl = false;
                                            isCanHide = true;
                                            vTop.setVisibility(View.GONE);
                                            vSpeaker.setVisibility(View.GONE);
                                            vBottom.setVisibility(View.GONE);
                                        }
                                    }, 5000);
                                }
                            }
                        }
                    }
                });
            }

            @Override
            public void onLocalStream(final StringeeCall stringeeCall) {
                if (stringeeCall.isVideoCall()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            vLocal.addView(stringeeCall.getLocalView());
                            stringeeCall.renderLocalView(true);
                        }
                    });
                }
            }

            @Override
            public void onRemoteStream(final StringeeCall stringeeCall) {
                if (stringeeCall.isVideoCall()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            vRemote.addView(stringeeCall.getRemoteView());
                            stringeeCall.renderRemoteView(false);

                            stringeeCall.getRemoteView().setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    if (isCanHide) {
                                        isShowControl = !isShowControl;
                                        if (isShowControl) {
                                            vTop.setVisibility(View.VISIBLE);
                                            vSpeaker.setVisibility(View.VISIBLE);
                                            vBottom.setVisibility(View.VISIBLE);
                                        } else {
                                            vTop.setVisibility(View.GONE);
                                            vSpeaker.setVisibility(View.GONE);
                                            vBottom.setVisibility(View.GONE);
                                        }
                                    }
                                }
                            });
                        }
                    });
                }
            }

            @Override
            public void onCallInfo(StringeeCall stringeeCall, final JSONObject jsonObject) {
                try {
                    String type = jsonObject.getString("type");
                    if (type.equals("cameraRequest")) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                AlertDialog.Builder builder = new AlertDialog.Builder(IncomingCallActivity.this);
                                builder.setMessage("You have a camera request. Do you want to accept?");
                                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                        JSONObject answerObject = new JSONObject();
                                        try {
                                            answerObject.put("type", "answerCameraRequest");
                                            answerObject.put("accept", false);
                                            incomingCall.sendCallInfo(answerObject);

                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });
                                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        JSONObject answerObject = new JSONObject();
                                        try {
                                            answerObject.put("type", "answerCameraRequest");
                                            answerObject.put("accept", true);
                                            incomingCall.sendCallInfo(answerObject);

                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }

                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            if (ContextCompat.checkSelfPermission(IncomingCallActivity.this,
                                                    Manifest.permission.CAMERA)
                                                    != PackageManager.PERMISSION_GRANTED) {
                                                String[] permissions = {Manifest.permission.CAMERA};
                                                ActivityCompat.requestPermissions(IncomingCallActivity.this, permissions, REQUEST_PERMISSION_CAMERA_WHEN_ANSWER);
                                                return;
                                            }
                                        }

                                        acceptCameraRequest();
                                    }
                                });
                                AlertDialog dialog = builder.create();
                                dialog.show();
                            }
                        });
                    } else if (type.equals("answerCameraRequest")) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                boolean accept = false;
                                try {
                                    accept = jsonObject.getBoolean("accept");
                                    if (accept) {
                                        Utils.reportMessage(IncomingCallActivity.this, "Your camera request is accepted.");
                                    } else {
                                        Utils.reportMessage(IncomingCallActivity.this, "Your camera request is rejected.");
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        if (incomingCall != null) {
            incomingCall.initAnswer(this, Common.client);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_answer) {
            if (incomingCall != null) {
                mState = StringeeCall.SignalingState.ANSWERED;
                if (ringtone != null && ringtone.isPlaying()) {
                    ringtone.stop();
                    ringtone = null;
                }
                audioManager.setSpeakerphoneOn(false);
                tvState.setText(R.string.call_starting);
                vSpeaker.setVisibility(View.VISIBLE);
                vEnd.setVisibility(View.GONE);
                btnEndCall.setVisibility(View.VISIBLE);
                incomingCall.answer();
            }
        } else if (v.getId() == R.id.btn_reject) {
            tvState.setText(R.string.call_ended);
            endCall(0);
        } else if (v.getId() == R.id.btn_end_call) {
            tvState.setText(R.string.call_ended);
            long duration = 0;
            if (startTime > 0) {
                duration = System.currentTimeMillis() - startTime;
            }
            endCall(duration);
        } else if (v.getId() == R.id.btn_speaker) {
            isSpeaker = !isSpeaker;
            if (isSpeaker) {
                btnSpeaker.setImageResource(R.drawable.ic_speaker_on);
            } else {
                btnSpeaker.setImageResource(R.drawable.ic_speaker_off);
            }
            if (incomingCall != null) {
                incomingCall.setSpeakerphoneOn(isSpeaker);
            }
        } else if (v.getId() == R.id.btn_mute) {
            isMute = !isMute;
            if (isMute) {
                btnMute.setImageResource(R.drawable.ic_mute_on);
            } else {
                btnMute.setImageResource(R.drawable.ic_mute_off);
            }
            if (incomingCall != null) {
                incomingCall.mute(isMute);
            }
        } else if (v.getId() == R.id.btn_video) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    String[] permissions = {Manifest.permission.CAMERA};
                    ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSION_CAMERA);
                    return;
                }
            }
            enableOrDisableVideo();
        } else if (v.getId() == R.id.btn_switch) {
            if (incomingCall != null) {
                incomingCall.switchCamera(null);
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float value = event.values[0];
        if (value == 0) {
            vBlack.setVisibility(View.VISIBLE);
            Window window = getWindow();
            View decorView = window.getDecorView();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                window.setStatusBarColor(Color.parseColor("#000000")); // set dark color, the icon will auto change light
            }

            if (!wakeLock.isHeld()) {
                wakeLock.acquire();
            }
        } else {
            vBlack.setVisibility(View.GONE);
            Window window = getWindow();
            View decorView = window.getDecorView();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                window.setStatusBarColor(Color.parseColor("#007ce2")); // set dark color, the icon will auto change light
            }

            if (wakeLock.isHeld()) {
                wakeLock.release();
            }
        }
    }

    @Override

    public void onAccuracyChanged(Sensor sensor, int i) {

    }


    private void endCall(long duration) {
        if (incomingCall == null) {
            return;
        }
        btnEndCall.setVisibility(View.GONE);
        vSpeaker.setVisibility(View.GONE);
        if (!incomingCall.isVideoCall()) {
            vBackground.setBackgroundColor(Color.WHITE);
            tvState.setTextColor(Color.parseColor("#dc6456"));
            tvName.setTextColor(Color.parseColor("#4e4e4e"));
            tvPhone.setTextColor(Color.parseColor("#939393"));
        }
        imNetwork.setVisibility(View.GONE);
        vBottom.setVisibility(View.GONE);
        Window window = getWindow();
        View decorView = window.getDecorView();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            window.setStatusBarColor(Color.parseColor("#ffffff")); // set dark color, the icon will auto change light
        }

        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
            ringtone = null;
        }
        if (endPlayer != null && !endPlayer.isPlaying()) {
            endPlayer.start();
        }
        startTime = 0;

        if (timer != null) {
            timer.cancel();
        }
        if (statsTimer != null) {
            statsTimer.cancel();
        }

        if (bluetoothManager != null) {
            bluetoothManager.stop();
        }

        if (incomingCall != null) {
            incomingCall.hangup();
            incomingCall = null;
        }

        if (mMessage != null) {
            updateCall(mMessage, duration, Constant.MESSAGE_DELIVERED);
        }

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isFromPush) {
                    NotifyUtils.notifyEndCall();
                }
                Common.isInCall = false;
                finish();
            }
        }, 2000);
    }

    private void saveCall() {
        Object[] params = new Object[1];
        params[0] = ACTION_SAVE_CALL;
        DataHandler handler = new DataHandler(this, this);
        handler.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
    }

    private void doSaveCall() {
        SimpleDateFormat format = new SimpleDateFormat(Constant.DATETIME_FORMAT);
        Date date = new Date();
        String strTime = format.format(date);
        int type = Constant.TYPE_INCOMING_CALL;
        if (incomingCall != null && incomingCall.isPhoneToAppCall()) {
            type = Constant.TYPE_CALL_PHONE_TO_APP;
        }
        mMessage = new Message(Common.userId, 0, "00:00", strTime, type,
                Constant.CHAT_TYPE_PRIVATE);
        if (name != null) {
            mMessage.setFullname(name);
        }
        mMessage.setPhoneNumber(phoneNumber);
        if (phoneNo != null) {
            mMessage.setPhoneNo(phoneNo);
        } else {
            mMessage.setPhoneNo(phoneNumber);
        }
        mMessage.setMsgId(++Common.messageId);
        mMessage.setIsRead(Constant.MESSAGE_READ);
        mMessage.setState(Constant.MESSAGE_SENT);
        mMessage.setShortDate(DateTimeUtils.getTime(date));
        int id = Common.messageDb.insertMessage(mMessage);
        mMessage.setId(id);
    }

    private void updateCall(Message message, long duration, int state) {
        Object[] params = new Object[4];
        params[0] = ACTION_UPDATE_CALL;
        params[1] = message;
        params[2] = duration;
        params[3] = state;
        DataHandler handler = new DataHandler(this, this);
        handler.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
    }

    private void doUpdateCall(Message imMessage, long duration, int state) {
        imMessage.setText(DateTimeUtils.getCallTime(duration));
        imMessage.setState(state);
        Common.messageDb.updateCall(imMessage);
    }

    @Override
    public void start() {
    }

    @Override
    public void doWork(Object... params) {
        String strAction = (String) params[0];
        if (strAction.equals(ACTION_SAVE_CALL)) {
            doSaveCall();
        } else if (strAction.equals(ACTION_UPDATE_CALL)) {
            doUpdateCall((Message) params[1], (Long) params[2], (int) params[3]);
        }
    }

    @Override
    public void end(Object[] params) {
        String strAction = (String) params[0];
        if (strAction.equals(ACTION_SAVE_CALL)) {

        } else if (strAction.equals(ACTION_UPDATE_CALL)) {
            NotifyUtils.notifyUpdateRecents();
        }
    }

    private void checkCallStats(StringeeCall.StringeeCallStats stats) {
        double videoTimestamp = stats.timeStamp / 1000;

        //initialize values
        if (mPrevCallTimestamp == 0) {
            mPrevCallTimestamp = videoTimestamp;
            mPrevCallBytes = stats.callBytesReceived;
        } else {
            //calculate video bandwidth
            mCallBw = (long) ((8 * (stats.callBytesReceived - mPrevCallBytes)) / (videoTimestamp - mPrevCallTimestamp));
            mPrevCallTimestamp = videoTimestamp;
            mPrevCallBytes = stats.callBytesReceived;

            Log.e("Stringee", "Call bandwidth (bps): " + mCallBw);

            checkNetworkQuality();
        }
    }

    private void checkNetworkQuality() {
        if (mCallBw <= 0) {
            imNetwork.setImageResource(R.drawable.no_connect);
        } else {
            if (mCallBw < 15000) {
                imNetwork.setImageResource(R.drawable.poor);
            } else {
                if (mCallBw >= 35000) {
                    imNetwork.setImageResource(R.drawable.exellent);
                } else {
                    if (mCallBw >= 15000 && mCallBw <= 25000) {
                        imNetwork.setImageResource(R.drawable.average);
                    } else if (mCallBw >= 25000 && mCallBw < 35000) {
                        imNetwork.setImageResource(R.drawable.good);
                    }
                }
            }
        }
    }

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter(Notify.END_CALL_FROM_DIAL.getValue());
        endCallReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                tvState.setText(R.string.call_ended);
                long duration = 0;
                if (startTime > 0) {
                    duration = System.currentTimeMillis() - startTime;
                }
                endCall(duration);
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(endCallReceiver, filter);
    }

    private void unregisterReceiver() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(endCallReceiver);
    }

    private void enableOrDisableVideo() {
        btnSwitch.setVisibility(View.VISIBLE);
        isVideoOn = !isVideoOn;
        if (isVideoOn) {
            btnVideo.setImageResource(R.drawable.ic_video);
        } else {
            btnVideo.setImageResource(R.drawable.ic_video_off);
        }
        if (incomingCall != null) {
            if (!incomingCall.isVideoCall()) { // Send camera request
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("type", "cameraRequest");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                incomingCall.sendCallInfo(jsonObject);
            }
            incomingCall.enableVideo(isVideoOn);
        }
    }

    private void acceptCameraRequest() {
        btnSwitch.setVisibility(View.VISIBLE);
        isVideoOn = true;
        btnVideo.setImageResource(R.drawable.ic_video);
        incomingCall.enableVideo(true);
    }
}
