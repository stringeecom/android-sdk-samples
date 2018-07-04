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
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.MotionEvent;
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
import com.stringee.softphone.common.PrefUtils;
import com.stringee.softphone.common.StringeeBluetoothManager;
import com.stringee.softphone.common.Utils;
import com.stringee.softphone.fragment.DialFragment;
import com.stringee.softphone.model.Message;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.SurfaceViewRenderer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by luannguyen on 7/11/2017.
 */

public class OutgoingCallActivity extends MActivity implements SensorEventListener {

    private TextView tvName;
    private TextView tvPhone;
    private TextView tvStatus;
    private ImageButton btnMute;
    private ImageButton btnSpeaker;
    private ImageButton btnVideo;
    private View vBlack;
    private ImageButton btnEndcall;
    private View vControl;
    private RelativeLayout vBackground;
    private ImageView imNetwork;
    private FrameLayout vLocal;
    private FrameLayout vRemote;
    private View vTop;
    private View vMid;
    private ImageButton btnSwitch;

    private boolean isMute = false;
    private boolean isSpeaker = false;
    private boolean isVideoOn = false;
    private String phone;
    private String phoneNo;
    private String name;
    private Message mMessage;
    private long startTime;
    private Vibrator vibrator;
    private SensorManager mSensorManager;
    private Sensor mProximity;
    private TimerTask timerTask;
    private Timer timer;
    private Handler handler = new Handler();
    private Timer statsTimer;
    private TimerTask statsTimerTask;

    private MediaPlayer endPlayer;
    private MediaPlayer ringtonePlayer;

    public static StringeeCall outgoingCall;

    private double mPrevCallTimestamp = 0;
    private long mPrevCallBytes = 0;
    private long mCallBw = 0;

    private final String ACTION_SAVE_CALL = "save_call";
    private final String ACTION_UPDATE_CALL = "update_call";

    public static final int REQUEST_PERMISSION_CALLOUT = 1;
    public static final int REQUEST_PERMISSION_CAMERA = 2;
    public static final int REQUEST_PERMISSION_CAMERA_WHEN_ANSWER = 3;

    private boolean isCallOut;

    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;
    private int field = 0x00000020;
    private int notificationId = 25061987;

    private BroadcastReceiver endCallReceiver;

    private boolean isVideoCall;
    private boolean isShowControl = true;
    private boolean isCanHide = false;

    private StringeeCall.SignalingState mSignalingState;
    private StringeeCall.MediaState mMediaState;

    private StringeeBluetoothManager bluetoothManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        setContentView(R.layout.outgoing_call);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window w = getWindow();
            w.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            w.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        } else {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        Common.isInCall = true;
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        mSensorManager.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        bluetoothManager = StringeeBluetoothManager.create(this);
        bluetoothManager.start();

        name = getIntent().getStringExtra(Constant.PARAM_NAME);
        phone = getIntent().getStringExtra(Constant.PARAM_PHONE);
        isCallOut = getIntent().getBooleanExtra(Constant.PARAM_CALLOUT, false);
        phoneNo = getIntent().getStringExtra(Constant.PARAM_PHONE_NO);
        isVideoCall = getIntent().getBooleanExtra(Constant.PARAM_VIDEO_CALL, false);

        isSpeaker = isVideoCall;
        isVideoOn = isVideoCall;

        initViews();

        registerReceiver();

        KeyguardManager.KeyguardLock lock = ((KeyguardManager) getSystemService(KEYGUARD_SERVICE)).newKeyguardLock(KEYGUARD_SERVICE);
        lock.disableKeyguard();

        try {
            // Yeah, this is hidden field.
            field = PowerManager.class.getField("SOFTPHONE_PROXIMITY_SCREEN_OFF_WAKE_LOCK").getInt(null);
        } catch (Throwable ignored) {
        }

        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(field, getLocalClassName());

        String content = name;
        if (name == null) {
            name = phone;
        }
        NotifyUtils.showCallNotify(this, content, new Intent(this, OutgoingCallActivity.class), notificationId);
        // Check permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            List<String> lstPermissions = new ArrayList<>();
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                lstPermissions.add(Manifest.permission.RECORD_AUDIO);
            }

            if (isVideoCall) {
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
                ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSION_CALLOUT);
                return;
            }
        }
        saveCall();
        startCall();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver();
        Common.isInCall = false;
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
            case REQUEST_PERMISSION_CALLOUT:
                if (!isGranted) {
                    Utils.reportMessage(this, R.string.recording_required);
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Common.isInCall = false;
                            endCall(0);
                        }
                    }, 1000);
                    return;
                } else {
                    saveCall();
                    startCall();
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

    @Override
    public void onBackPressed() {
        Fragment dialFragment = getSupportFragmentManager().findFragmentByTag("DIAL_IN_CALL");
        if (dialFragment != null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.setCustomAnimations(R.anim.slide_in_up, R.anim.slide_out_up);
            ft.remove(dialFragment).commit();
        }
    }

    private void initViews() {
        tvName = (TextView) findViewById(R.id.tv_name);
        tvPhone = (TextView) findViewById(R.id.tv_phone);
        if (name != null) {
            tvName.setText(name);
            if (isCallOut) {
                tvPhone.setText(phoneNo);
            }
        } else {
            tvPhone.setVisibility(View.GONE);
            tvName.setText(phoneNo);
        }
        tvStatus = (TextView) findViewById(R.id.tv_status);

        btnMute = (ImageButton) findViewById(R.id.btn_mute);
        btnMute.setOnClickListener(this);
        btnSpeaker = (ImageButton) findViewById(R.id.btn_speaker);
        btnSpeaker.setOnClickListener(this);
        if (isSpeaker) {
            btnSpeaker.setImageResource(R.drawable.ic_speaker_on);
        } else {
            btnSpeaker.setImageResource(R.drawable.ic_speaker_off);
        }
        btnEndcall = (ImageButton) findViewById(R.id.btn_end_call);
        btnEndcall.setOnClickListener(this);

        View vVideo = findViewById(R.id.v_video);
        if (isCallOut) {
            vVideo.setVisibility(View.GONE);
        }

        btnVideo = (ImageButton) findViewById(R.id.btn_video);
        btnVideo.setOnClickListener(this);
        if (isVideoCall) {
            btnVideo.setImageResource(R.drawable.ic_video);
        } else {
            btnVideo.setImageResource(R.drawable.ic_video_off);
        }

        vBlack = findViewById(R.id.v_black);
        vBlack.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return true;
            }
        });

        vControl = findViewById(R.id.v_control);
        vBackground = (RelativeLayout) findViewById(R.id.v_background);
        imNetwork = (ImageView) findViewById(R.id.im_network);

        View vDial = findViewById(R.id.v_dial);
        if (!isCallOut) {
            vDial.setVisibility(View.GONE);
        }

        ImageButton btnDial = (ImageButton) findViewById(R.id.btn_dial);
        btnDial.setOnClickListener(this);

        vLocal = (FrameLayout) findViewById(R.id.v_local);
        vRemote = (FrameLayout) findViewById(R.id.v_remote);

        vTop = findViewById(R.id.v_top);
        vMid = findViewById(R.id.v_mid);

        btnSwitch = (ImageButton) findViewById(R.id.btn_switch);
        btnSwitch.setOnClickListener(this);
        if (isVideoCall) {
            btnSwitch.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_mute) {
            isMute = !isMute;
            if (isMute) {
                btnMute.setImageResource(R.drawable.ic_mute_on);
            } else {
                btnMute.setImageResource(R.drawable.ic_mute_off);
            }
            if (outgoingCall != null) {
                outgoingCall.mute(isMute);
            }
        } else if (v.getId() == R.id.btn_speaker) {
            isSpeaker = !isSpeaker;
            if (isSpeaker) {
                btnSpeaker.setImageResource(R.drawable.ic_speaker_on);
            } else {
                btnSpeaker.setImageResource(R.drawable.ic_speaker_off);
            }
            if (outgoingCall != null) {
                outgoingCall.setSpeakerphoneOn(isSpeaker);
            }
        } else if (v.getId() == R.id.btn_end_call) {
            tvStatus.setText(R.string.call_ended);
            long duration = 0;
            if (startTime > 0) {
                duration = System.currentTimeMillis() - startTime;
            }
            endCall(duration);
        } else if (v.getId() == R.id.btn_dial) {
            Fragment fragment = new DialFragment();
            Bundle args = new Bundle();
            args.putBoolean("isInCall", true);
            args.putBoolean("outgoing", true);
            fragment.setArguments(args);
            FragmentTransaction ft1 = getSupportFragmentManager().beginTransaction();
            ft1.setCustomAnimations(R.anim.slide_in_up, R.anim.slide_out_up);
            ft1.add(R.id.v_dialing, fragment, "DIAL_IN_CALL").commit();
        } else if (v.getId() == R.id.btn_video) {
            if (mMediaState == StringeeCall.MediaState.CONNECTED && mSignalingState == StringeeCall.SignalingState.ANSWERED) {
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
            }
        } else if (v.getId() == R.id.btn_switch) {
            if (outgoingCall != null) {
                outgoingCall.switchCamera(null);
            }
        }
    }

    private void startCall() {
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


        //do chuong
        if (ringtonePlayer == null) {
            ringtonePlayer = MediaPlayer
                    .create(OutgoingCallActivity.this, R.raw.call_ringing);
            ringtonePlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            ringtonePlayer.setVolume(1.0f, 1.0f);
            ringtonePlayer.setLooping(true);
        }
        if (!ringtonePlayer.isPlaying()) {
            ringtonePlayer.start();
        }

        if (isCallOut) {
            outgoingCall = new StringeeCall(this, Common.client, PrefUtils.getInstance(this).getString(Constant.PREF_SELECTED_NUMBER, ""), Utils.formatPhone(phone));
        } else {
            outgoingCall = new StringeeCall(this, Common.client, PrefUtils.getInstance(this).getString(Constant.PREF_USER_ID, ""), Utils.formatPhone(phone));
        }
        outgoingCall.setVideoCall(isVideoCall);
        outgoingCall.setCallListener(new StringeeCall.StringeeCallListener() {
            @Override
            public void onSignalingStateChange(StringeeCall stringeeCall, final StringeeCall.SignalingState signalingState, String reason, int sipCode, String sipReason) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mSignalingState = signalingState;
                        if (mSignalingState == StringeeCall.SignalingState.CALLING) {
                            tvStatus.setText(R.string.calling);
                        } else if (mSignalingState == StringeeCall.SignalingState.RINGING) {
                            tvStatus.setText(R.string.ringing);
                            if (isCallOut) {
                                if (ringtonePlayer != null && ringtonePlayer.isPlaying()) {
                                    ringtonePlayer.stop();
                                    ringtonePlayer.release();
                                    ringtonePlayer = null;
                                }
                            }
                        } else if (mSignalingState == StringeeCall.SignalingState.ANSWERED) {
                            if (mMediaState == StringeeCall.MediaState.CONNECTED) {
                                if (startTime > 0) {
                                    return;
                                }

                                if (ringtonePlayer != null && ringtonePlayer.isPlaying()) {
                                    ringtonePlayer.stop();
                                    ringtonePlayer.release();
                                    ringtonePlayer = null;
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
                                                tvStatus.setText(DateTimeUtils.getCallTime(System
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
                                        if (outgoingCall != null) {
                                            outgoingCall.getStats(new StringeeCall.CallStatsListener() {
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
                                    }
                                };
                                statsTimer.schedule(statsTimerTask, 0, 2000);

                                if (isVideoCall) {
                                    vMid.setVisibility(View.GONE);
                                    vTop.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            isShowControl = false;
                                            isCanHide = true;
                                            vTop.setVisibility(View.GONE);
                                            vControl.setVisibility(View.GONE);
                                            btnEndcall.setVisibility(View.GONE);
                                        }
                                    }, 5000);
                                }
                            }
                        } else if (mSignalingState == StringeeCall.SignalingState.BUSY) {
                            tvStatus.setText(R.string.busy);
                            endCall(0);
                        } else if (mSignalingState == StringeeCall.SignalingState.ENDED) {
                            long duration = 0;
                            if (startTime > 0) {
                                duration = System.currentTimeMillis() - startTime;
                            }
                            tvStatus.setText(R.string.call_ended);
                            endCall(duration);
                        }
                    }
                });
            }

            @Override
            public void onError(final StringeeCall stringeeCall, int i, final String s) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Utils.reportMessage(OutgoingCallActivity.this, s);
                                endCall(0);
                            }
                        }, 1000);
                    }
                });
            }

            @Override
            public void onHandledOnAnotherDevice(StringeeCall stringeeCall, final StringeeCall.SignalingState signalingState, String desc) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        switch (signalingState) {
                            case RINGING:
                                break;
                            case ANSWERED:
                                Utils.reportMessage(OutgoingCallActivity.this, "This call is answered from another device.");
                                endCall(0);
                                break;
                            case BUSY:
                                Utils.reportMessage(OutgoingCallActivity.this, "This call is rejected from another device.");
                                endCall(0);
                                break;
                            case ENDED:
                                Utils.reportMessage(OutgoingCallActivity.this, "This call is ended from another device.");
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
                        mMediaState = mediaState;
                        if (mediaState == StringeeCall.MediaState.CONNECTED) {
                            if (mSignalingState == StringeeCall.SignalingState.ANSWERED) {
                                if (startTime > 0) {
                                    return;
                                }

                                if (ringtonePlayer != null && ringtonePlayer.isPlaying()) {
                                    ringtonePlayer.stop();
                                    ringtonePlayer.release();
                                    ringtonePlayer = null;
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
                                                tvStatus.setText(DateTimeUtils.getCallTime(System
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
                                        if (outgoingCall != null) {
                                            outgoingCall.getStats(new StringeeCall.CallStatsListener() {
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
                                    }
                                };
                                statsTimer.schedule(statsTimerTask, 0, 2000);

                                if (isVideoCall) {
                                    vMid.setVisibility(View.GONE);
                                    vTop.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            isShowControl = false;
                                            isCanHide = true;
                                            vTop.setVisibility(View.GONE);
                                            vControl.setVisibility(View.GONE);
                                            btnEndcall.setVisibility(View.GONE);
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
                            SurfaceViewRenderer view = stringeeCall.getLocalView();
                            vLocal.addView(view);
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
                                            vControl.setVisibility(View.VISIBLE);
                                            btnEndcall.setVisibility(View.VISIBLE);
                                        } else {
                                            vTop.setVisibility(View.GONE);
                                            vControl.setVisibility(View.GONE);
                                            btnEndcall.setVisibility(View.GONE);
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
                                AlertDialog.Builder builder = new AlertDialog.Builder(OutgoingCallActivity.this);
                                builder.setMessage("You have a camera request. Do you want to accept?");
                                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                        JSONObject answerObject = new JSONObject();
                                        try {
                                            answerObject.put("type", "answerCameraRequest");
                                            answerObject.put("accept", false);
                                            outgoingCall.sendCallInfo(answerObject);

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
                                            outgoingCall.sendCallInfo(answerObject);

                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }

                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            if (ContextCompat.checkSelfPermission(OutgoingCallActivity.this,
                                                    Manifest.permission.CAMERA)
                                                    != PackageManager.PERMISSION_GRANTED) {
                                                String[] permissions = {Manifest.permission.CAMERA};
                                                ActivityCompat.requestPermissions(OutgoingCallActivity.this, permissions, REQUEST_PERMISSION_CAMERA_WHEN_ANSWER);
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
                                        Utils.reportMessage(OutgoingCallActivity.this, "Your camera request is accepted.");
                                    } else {
                                        Utils.reportMessage(OutgoingCallActivity.this, "Your camera request is rejected.");
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

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("app-to-phone", isCallOut);
            outgoingCall.setCustom(jsonObject.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        outgoingCall.makeCall();
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
        int type = Constant.TYPE_OUTGOING_CALL;
        if (isCallOut) {
            type = Constant.TYPE_CALL_OUT;
        }
        mMessage = new Message(Common.userId, 0, "00:00", strTime, type,
                Constant.CHAT_TYPE_PRIVATE);
        if (name != null) {
            mMessage.setFullname(name);
        }
        mMessage.setPhoneNumber(phone);
        mMessage.setPhoneNo(phoneNo);
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

            if (wakeLock != null && !wakeLock.isHeld()) {
                wakeLock.acquire();
            }
        } else {
            vBlack.setVisibility(View.GONE);
            Window window = getWindow();
            View decorView = window.getDecorView();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                window.setStatusBarColor(Color.parseColor("#333333")); // set dark color, the icon will auto change light
            }

            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private void endCall(long duration) {
        if (outgoingCall == null) {
            return;
        }
        if (ringtonePlayer != null && ringtonePlayer.isPlaying()) {
            ringtonePlayer.stop();
            ringtonePlayer.release();
            ringtonePlayer = null;
        }
        btnEndcall.setVisibility(View.INVISIBLE);
        vControl.setVisibility(View.GONE);
        if (!outgoingCall.isVideoCall()) {
            vBackground.setBackgroundColor(Color.WHITE);
            tvStatus.setTextColor(Color.parseColor("#dc6456"));
            tvName.setTextColor(Color.parseColor("#4e4e4e"));
            tvPhone.setTextColor(Color.parseColor("#939393"));
        }
        imNetwork.setVisibility(View.GONE);

        Window window = getWindow();
        View decorView = window.getDecorView();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            window.setStatusBarColor(Color.parseColor("#ffffff")); // set dark color, the icon will auto change light
        }

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
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

        if (mMessage != null) {
            updateCall(mMessage, duration, Constant.MESSAGE_DELIVERED);
        }
        if (outgoingCall != null) {
            outgoingCall.hangup();
            outgoingCall = null;
        }

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Common.isInCall = false;
                LocalBroadcastManager.getInstance(OutgoingCallActivity.this).sendBroadcast(new Intent(Notify.CHECK_BALANCE.getValue()));
                finish();
            }
        }, 2000);
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
                tvStatus.setText(R.string.call_ended);
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
        if (outgoingCall != null) {
            if (!outgoingCall.isVideoCall()) { // Send camera request
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("type", "cameraRequest");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                outgoingCall.sendCallInfo(jsonObject);
            }
            outgoingCall.enableVideo(isVideoOn);
        }
    }

    private void acceptCameraRequest() {
        btnSwitch.setVisibility(View.VISIBLE);
        isVideoOn = true;
        btnVideo.setImageResource(R.drawable.ic_video);
        outgoingCall.enableVideo(true);
    }
}
