package com.stringee.conferencecallsample.activity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import com.stringee.conference.StringeeRoom;
import com.stringee.conference.StringeeStream;
import com.stringee.conferencecallsample.R;
import com.stringee.exception.StringeeError;
import com.stringee.listener.StringeeRoomListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by luannguyen on 10/27/2017.
 */

public class ConferenceCallActivity extends AppCompatActivity implements View.OnClickListener {

    private StringeeRoom mStringeeRoom;
    private StringeeStream localStream;
    private String action;
    private int roomId;
    private boolean isMute = false;
    private boolean isSpeaker = true;
    private Map<String, StringeeStream> streamMap = new HashMap<>();

    private FrameLayout mLocalViewContainer;
    private FrameLayout mRemoteViewContainer;
    private FrameLayout mRemoteViewContainer2;
    private TextView tvRoomId;
    private ImageButton btnMute;
    private ImageButton btnSpeaker;

    public static final int REQUEST_PERMISSION_CALL = 1;

    private AudioManager audioManager;
    private BroadcastReceiver disconnectReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conference_call);

        action = getIntent().getStringExtra("action");
        roomId = getIntent().getIntExtra("room_id", 0);

        tvRoomId = (TextView) findViewById(R.id.tv_room_id);
        if (roomId > 0) {
            tvRoomId.setText("Room id: " + roomId);
        }

        mLocalViewContainer = (FrameLayout) findViewById(R.id.v_local);
        mRemoteViewContainer = (FrameLayout) findViewById(R.id.v_remote);
        mRemoteViewContainer2 = (FrameLayout) findViewById(R.id.v_remote2);

        btnMute = (ImageButton) findViewById(R.id.btn_mute);
        btnMute.setOnClickListener(this);
        btnSpeaker = (ImageButton) findViewById(R.id.btn_speaker);
        btnSpeaker.setOnClickListener(this);

        ImageButton btnEnd = (ImageButton) findViewById(R.id.btn_end);
        btnEnd.setOnClickListener(this);

        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        disconnectReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                streamMap.clear();
                mRemoteViewContainer.removeAllViews();
                mRemoteViewContainer2.removeAllViews();
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(disconnectReceiver, new IntentFilter("disconnect"));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            List<String> lstPermissions = new ArrayList<>();

            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                lstPermissions.add(Manifest.permission.RECORD_AUDIO);
            }

            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                lstPermissions.add(Manifest.permission.CAMERA);
            }

            if (lstPermissions.size() > 0) {
                String[] permissions = new String[lstPermissions.size()];
                for (int i = 0; i < lstPermissions.size(); i++) {
                    permissions[i] = lstPermissions.get(i);
                }
                ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSION_CALL);
                return;
            }
        }

        startConference();
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
            case REQUEST_PERMISSION_CALL:
                if (!isGranted) {
                    finish();
                } else {
                    startConference();
                }
                break;
        }
    }


    private void startConference() {
        if (action.equals("make")) {
            mStringeeRoom = new StringeeRoom(MainActivity.client);
        } else if (action.equals("join")) {
            mStringeeRoom = new StringeeRoom(MainActivity.client, roomId);
        }
        mStringeeRoom.setRoomListener(new StringeeRoomListener() {
            @Override
            public void onRoomConnected(final StringeeRoom stringeeRoom) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvRoomId.setText("Room id: " + stringeeRoom.getId());
                    }
                });

                localStream = new StringeeStream(ConferenceCallActivity.this);
                localStream.setStreamListener(new StringeeStream.StringeeStreamListener() {
                    @Override
                    public void onStreamMediaAvailable(final StringeeStream stringeeStream) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mLocalViewContainer.removeAllViews();
                                mLocalViewContainer.addView(stringeeStream.getView());
                                stringeeStream.renderView(true);
                            }
                        });
                    }
                });
                mStringeeRoom.publish(localStream);
            }

            @Override
            public void onRoomDisconnected(StringeeRoom stringeeRoom) {

            }

            @Override
            public void onRoomError(StringeeRoom stringeeRoom, StringeeError stringeeError) {

            }

            @Override
            public void onStreamAdded(StringeeStream stringeeStream) {
                stringeeStream.setStreamListener(new StringeeStream.StringeeStreamListener() {
                    @Override
                    public void onStreamMediaAvailable(final StringeeStream stringeeStream) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                StringeeStream stream1 = streamMap.get("remote1");
                                if (stream1 == null || stream1.getUserId().equals(stringeeStream.getUserId())) {
                                    streamMap.put("remote1", stringeeStream);
                                    mRemoteViewContainer.removeAllViews();
                                    mRemoteViewContainer.addView(stringeeStream.getView());
                                    stringeeStream.renderView(false);
                                } else {
                                    streamMap.put("remote2", stringeeStream);
                                    mRemoteViewContainer2.removeAllViews();
                                    mRemoteViewContainer2.addView(stringeeStream.getView());
                                    stringeeStream.renderView(false);
                                }
                            }
                        });
                    }
                });
                mStringeeRoom.subscribe(stringeeStream);
            }

            @Override
            public void onStreamRemoved(final StringeeStream stringeeStream) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        StringeeStream stream = streamMap.get("remote1");
                        if (stream != null && stream.getUserId().equals(stringeeStream.getUserId())) {
                            streamMap.remove("remote1");
                            mRemoteViewContainer.removeAllViews();
                        } else {
                            stream = streamMap.get("remote2");
                            if (stream != null && stream.getUserId().equals(stringeeStream.getUserId())) {
                                streamMap.remove("remote2");
                                mRemoteViewContainer2.removeAllViews();
                            }
                        }
                    }
                });
            }

            @Override
            public void onStreamPublished(StringeeStream stringeeStream, boolean b) {
            }

            @Override
            public void onStreamPublishError(StringeeStream stringeeStream, StringeeError stringeeError, boolean b) {

            }

            @Override
            public void onStreamUnPublished(StringeeStream stringeeStream) {

            }

            @Override
            public void onStreamUnPublishError(StringeeStream stringeeStream, StringeeError stringeeError) {

            }

            @Override
            public void onStreamSubscribed(StringeeStream stringeeStream, boolean b) {

            }

            @Override
            public void onStreamUnSubscribed(StringeeStream stringeeStream) {

            }

            @Override
            public void onStreamSubscribeError(StringeeStream stringeeStream, StringeeError stringeeError, boolean b) {

            }

            @Override
            public void onStreamUnSubscribeError(StringeeStream stringeeStream, StringeeError stringeeError) {

            }
        });
        if (action.equals("make")) {
            mStringeeRoom.makeRoom();
        } else if (action.equals("join")) {
            mStringeeRoom.joinRoom();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_mute:
                isMute = !isMute;
                if (isMute) {
                    btnMute.setImageResource(R.drawable.ic_mute);
                } else {
                    btnMute.setImageResource(R.drawable.ic_mic);
                }
                if (localStream != null) {
                    localStream.mute(isMute);
                }
                break;
            case R.id.btn_speaker:
                isSpeaker = !isSpeaker;
                if (isSpeaker) {
                    btnSpeaker.setImageResource(R.drawable.ic_speaker_on);
                } else {
                    btnSpeaker.setImageResource(R.drawable.ic_speaker_off);
                }
                if (audioManager != null) {
                    audioManager.setSpeakerphoneOn(isSpeaker);
                }
                break;
            case R.id.btn_end:
                if (mStringeeRoom != null) {
                    mStringeeRoom.leaveRoom();
                }
                finish();
                break;


        }
    }
}
