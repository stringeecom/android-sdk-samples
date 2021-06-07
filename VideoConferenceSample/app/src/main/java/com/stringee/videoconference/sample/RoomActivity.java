package com.stringee.videoconference.sample;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.stringee.common.StringeeAudioManager;
import com.stringee.exception.StringeeError;
import com.stringee.listener.StatusListener;
import com.stringee.listener.StringeeRoomListener;
import com.stringee.video.RemoteParticipant;
import com.stringee.video.StringeeRoom;
import com.stringee.video.StringeeVideo;
import com.stringee.video.StringeeVideoTrack;
import com.stringee.videoconference.videoconference.sample.R;

import org.json.JSONObject;
import org.webrtc.RendererCommon;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class RoomActivity extends AppCompatActivity implements View.OnClickListener {
    private TextView tvRoomName;
    private TextView tvNoParticipant;

    private ImageView btnBack;
    private ImageView btnSwitch;
    private ImageView btnCam;
    private ImageView btnMic;
    private ImageView btnLeave;
    private ImageView btnParticipant;
    private ImageView btnVisibility;

    public static FrameLayout mainView;
    private FrameLayout view1;
    private FrameLayout view2;
    private FrameLayout view3;
    private LinearLayout v1;
    private LinearLayout v2;
    private LinearLayout v3;
    private LinearLayout vOtherCamera;
    private View vAction;

    private RecyclerView rvParticipant;
    private ParticipantAdapter adapter;

    private String roomToken;
    private boolean isMute;
    private boolean isCamOn;
    private boolean mirror;

    private StringeeRoom mStringeeRoom;
    private StringeeVideoTrack localVideoTrack;

    private List<VideoTrack> videoTrackList;

    public static VideoTrack mainTrack;
    private VideoTrack firstTrack;
    private VideoTrack secondTrack;
    private VideoTrack thirdTrack;

    private StringeeAudioManager audioManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_room);

        // Keep screen active
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (getIntent() != null) {
            roomToken = getIntent().getStringExtra("room_token");
        }

        initView();

        audioManager = StringeeAudioManager.create(this);
        audioManager.start(new StringeeAudioManager.AudioManagerEvents() {
            @Override
            public void onAudioDeviceChanged(StringeeAudioManager.AudioDevice selectedAudioDevice, Set<StringeeAudioManager.AudioDevice> availableAudioDevices) {
            }
        });
        audioManager.setSpeakerphoneOn(true);

        startConference();
    }

    @Override
    public void onBackPressed() {
        return;
    }

    private void initView() {
        tvRoomName = findViewById(R.id.tv_room_name);
        tvRoomName.setSelected(true);
        tvNoParticipant = findViewById(R.id.tv_no_participant);

        btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(this);
        btnSwitch = findViewById(R.id.btn_switch);
        btnSwitch.setOnClickListener(this);
        btnCam = findViewById(R.id.btn_cam);
        btnCam.setOnClickListener(this);
        btnMic = findViewById(R.id.btn_mic);
        btnMic.setOnClickListener(this);
        btnLeave = findViewById(R.id.btn_leave);
        btnLeave.setOnClickListener(this);
        btnParticipant = findViewById(R.id.btn_participant);
        btnParticipant.setOnClickListener(this);
        btnVisibility = findViewById(R.id.btn_visibility);
        btnVisibility.setOnClickListener(this);

        videoTrackList = new ArrayList<>();
        rvParticipant = findViewById(R.id.rv_participant);
        rvParticipant.setHasFixedSize(true);
        rvParticipant.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ParticipantAdapter(this, this, videoTrackList);
        rvParticipant.setAdapter(adapter);

        mainView = findViewById(R.id.main_view);
        mainView.setOnClickListener(this);
        view1 = findViewById(R.id.view_1);
        view1.setOnClickListener(this);
        view2 = findViewById(R.id.view_2);
        view2.setOnClickListener(this);
        view3 = findViewById(R.id.view_3);
        view3.setOnClickListener(this);

        v1 = findViewById(R.id.v1);
        v2 = findViewById(R.id.v2);
        v3 = findViewById(R.id.v3);
        vOtherCamera = findViewById(R.id.v_other_camera);
        vAction = findViewById(R.id.v_action);

        mainTrack = firstTrack = secondTrack = thirdTrack = null;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.main_view:
                if (vAction.getVisibility() == View.VISIBLE) {
                    vAction.setVisibility(View.GONE);
                } else {
                    vAction.setVisibility(View.VISIBLE);
                }
                break;
            case R.id.view_1:
                changeLayoutWithMain((FrameLayout) view);
                break;
            case R.id.view_2:
                changeLayoutWithMain((FrameLayout) view);
                break;
            case R.id.view_3:
                changeLayoutWithMain((FrameLayout) view);
                break;
            case R.id.btn_back:
                onBackPressed();
                break;
            case R.id.btn_switch:
                if (localVideoTrack != null) {
                    localVideoTrack.switchCamera(null);
                    if (mirror) {
                        mirror = false;
                    } else {
                        mirror = true;
                    }
                    localVideoTrack.getView(this).setMirror(mirror);
                }
                break;
            case R.id.btn_mic:
                if (isMute) {
                    btnMic.setImageResource(R.drawable.ic_mic_on);
                } else {
                    btnMic.setImageResource(R.drawable.ic_mic_off);
                }
                isMute = !isMute;
                localVideoTrack.mute(isMute);
                break;
            case R.id.btn_cam:
                if (isCamOn) {
                    btnCam.setImageResource(R.drawable.ic_cam_off);
                } else {
                    btnCam.setImageResource(R.drawable.ic_cam_on);
                }
                isCamOn = !isCamOn;
                localVideoTrack.enableVideo(isCamOn);
                break;
            case R.id.btn_leave:
                leaveRoom();
                break;
            case R.id.btn_participant:
                showParticipant();
                break;
            case R.id.btn_visibility:
                if (vOtherCamera.getVisibility() == View.GONE) {
                    vOtherCamera.setVisibility(View.VISIBLE);
                    btnVisibility.setImageResource(R.drawable.ic_visibility);
                } else {
                    vOtherCamera.setVisibility(View.GONE);
                    btnVisibility.setImageResource(R.drawable.ic_visibility_off);
                }
                break;
        }
    }

    private void showParticipant() {
        if (videoTrackList.size() > 0) {
            tvNoParticipant.setVisibility(View.GONE);
            if (rvParticipant.getVisibility() == View.GONE) {
                rvParticipant.setVisibility(View.VISIBLE);
            } else {
                rvParticipant.setVisibility(View.GONE);
            }
        } else {
            rvParticipant.setVisibility(View.GONE);
            if (tvNoParticipant.getVisibility() == View.GONE) {
                tvNoParticipant.setVisibility(View.VISIBLE);
            } else {
                tvNoParticipant.setVisibility(View.GONE);
            }
        }
    }

    private void startConference() {
        //create option for localView
        StringeeVideoTrack.Options options = new StringeeVideoTrack.Options();
        options.audio(true);
        options.video(true);
        options.screen(false);

        isMute = false;
        isCamOn = true;
        mirror = true;

        //create localView
        localVideoTrack = StringeeVideo.createLocalVideoTrack(this, options);
        localVideoTrack.getView(this).setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT, RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        mainView.removeAllViews();
        mainView.addView(localVideoTrack.getView(this));
        localVideoTrack.renderView(false);
        localVideoTrack.getView(this).setMirror(mirror);

        //set videoTrack in mainView
        mainTrack = new VideoTrack(localVideoTrack, mainView);
        videoTrackList.add(mainTrack);

        //connect to room
        mStringeeRoom = StringeeVideo.connect(MainActivity.client, roomToken, new StringeeRoomListener() {
            @Override
            public void onConnected(StringeeRoom stringeeRoom) {
                Log.d("StringeeVideo", "=========Room connected=========");
                //publish localVideoTrack to room
                stringeeRoom.publish(localVideoTrack, new StatusListener() {
                    @Override
                    public void onSuccess() {
                        Log.d("StringeeVideo", "=========Publish local success=========");
                    }

                    @Override
                    public void onError(StringeeError errorInfo) {
                        super.onError(errorInfo);
                        Log.d("StringeeVideo", "=========Publish local error=========");
                        Log.d("StringeeVideo", "error: " + errorInfo.getMessage());
                    }
                });

                //get Participant videoTrack
                for (RemoteParticipant remoteParticipant : stringeeRoom.getRemoteParticipants()) {
                    for (StringeeVideoTrack videoTrack : remoteParticipant.getVideoTracks()) {
                        //create option, view for participant videoTrack
                        videoTrack.setListener(videoTrackListener(videoTrack));
                        StringeeVideoTrack.Options opt = new StringeeVideoTrack.Options();
                        opt.audio(true);
                        opt.video(true);
                        stringeeRoom.subscribe(videoTrack, opt, new StatusListener() {
                            @Override
                            public void onSuccess() {
                                Log.d("StringeeVideo", "=========Subcrise success=========");
                            }

                            @Override
                            public void onError(StringeeError errorInfo) {
                                super.onError(errorInfo);
                                Log.d("StringeeVideo", "=========Subcrise error=========");
                                Log.d("StringeeVideo", "error: " + errorInfo.getMessage());
                            }
                        });
                    }
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String roomName = Utils.getRoomName(stringeeRoom.getLocalParticipant().getId(), stringeeRoom.getRemoteParticipants());
                        tvRoomName.setText(roomName);
                    }
                });
            }

            @Override
            public void onDisconnected(StringeeRoom stringeeRoom) {
                Log.d("StringeeVideo", "=========onDisconnected=========");
            }

            @Override
            public void onError(StringeeRoom stringeeRoom, StringeeError stringeeError) {
                Log.d("StringeeVideo", "=========onError=========");
                Log.d("StringeeVideo", "error: " + stringeeError.getMessage());
            }

            @Override
            public void onParticipantConnected(StringeeRoom stringeeRoom, RemoteParticipant remoteParticipant) {
                Log.d("StringeeVideo", "=========onParticipantConnected=========");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        List<RemoteParticipant> remoteParticipants = new ArrayList<>();
                        remoteParticipants.add(remoteParticipant);
                        String roomName = Utils.getNewRoomName(tvRoomName.getText().toString(), remoteParticipant.getId(), "add");
                        tvRoomName.setText(roomName);
                    }
                });
            }

            @Override
            public void onParticipantDisconnected(StringeeRoom stringeeRoom, RemoteParticipant remoteParticipant) {
                Log.d("StringeeVideo", "=========onParticipantDisconnected=========");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String roomName = Utils.getNewRoomName(tvRoomName.getText().toString(), remoteParticipant.getId(), "remove");
                        tvRoomName.setText(roomName);
                    }
                });
            }

            @Override
            public void onVideoTrackAdded(StringeeRoom stringeeRoom, StringeeVideoTrack stringeeVideoTrack) {
                Log.d("StringeeVideo", "=========onVideoTrackAdded=========");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!stringeeVideoTrack.getUserId().equals(MainActivity.client.getUserId())) {
                            //create option, view for added videoTrack
                            stringeeVideoTrack.setListener(videoTrackListener(stringeeVideoTrack));
                            StringeeVideoTrack.Options opt = new StringeeVideoTrack.Options();
                            opt.audio(true);
                            opt.video(true);
                            stringeeRoom.subscribe(stringeeVideoTrack, opt, new StatusListener() {
                                @Override
                                public void onSuccess() {
                                    Log.d("StringeeVideo", "=========Subcrise success=========");
                                }

                                @Override
                                public void onError(StringeeError errorInfo) {
                                    super.onError(errorInfo);
                                    Log.d("StringeeVideo", "=========Subcrise error=========");
                                    Log.d("StringeeVideo", "error: " + errorInfo.getMessage());
                                }
                            });
                        }
                    }
                });

            }

            @Override
            public void onVideoTrackRemoved(StringeeRoom stringeeRoom, StringeeVideoTrack stringeeVideoTrack) {
//                Log.d("StringeeVideo", "=========VideoTrack removed=========");
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        for (Iterator<VideoTrack> videoTrackIterator = videoTrackList.iterator(); videoTrackIterator.hasNext(); ) {
//                            VideoTrack videoTrack = videoTrackIterator.next();
//                            if (videoTrack.getStringeeVideoTrack().getId().equals(stringeeVideoTrack.getId())) {
//                                if (videoTrack.getLayout() != null) {
//                                    //remove videoTrack parentView
//                                    FrameLayout trackLayout = (FrameLayout) videoTrack.getLayout();
//                                    trackLayout.removeAllViews();
//
//                                    StringeeVideoTrack track = videoTrack.getStringeeVideoTrack();
//
//                                    if (track.getView(RoomActivity.this).getParent() != null) {
//                                        ((FrameLayout) track.getView(RoomActivity.this).getParent()).removeView(track.getView(RoomActivity.this));
//                                    }
//
//                                    //if any track havent has parent yet, replace to this view
//                                    if (trackLayout.equals(mainView)) {
//                                        mainTrack = null;
//                                        addOtherVideoTrack(mainView);
//                                    }
//                                    if (trackLayout.equals(view1)) {
//                                        firstTrack = null;
//                                        addOtherVideoTrack(view1);
//                                    }
//                                    if (trackLayout.equals(view2)) {
//                                        secondTrack = null;
//                                        addOtherVideoTrack(view2);
//                                    }
//                                    if (trackLayout.equals(view3)) {
//                                        thirdTrack = null;
//                                        addOtherVideoTrack(view3);
//                                    }
//                                }
//                                videoTrackIterator.remove();
//                            }
//                        }
//                        if (firstTrack == null) {
//                            v1.setVisibility(View.GONE);
//                        }
//                        if (secondTrack == null) {
//                            v2.setVisibility(View.GONE);
//                        }
//                        if (thirdTrack == null) {
//                            v3.setVisibility(View.GONE);
//                        }
//
//                        adapter.notifyDataSetChanged();
//                        if (videoTrackList.size() == 0) {
//                            if (rvParticipant.getVisibility() == View.VISIBLE) {
//                                rvParticipant.setVisibility(View.GONE);
//                                tvNoParticipant.setVisibility(View.VISIBLE);
//                            }
//                        }
//                    }
//                });
            }

            @Override
            public void onMessage(StringeeRoom stringeeRoom, JSONObject jsonObject, RemoteParticipant remoteParticipant) {

            }
        });
    }

    private void addOtherVideoTrack(FrameLayout view) {
        for (int i = 0; i < videoTrackList.size(); i++) {
            VideoTrack videoTrack = videoTrackList.get(i);
            if (videoTrackList.get(i).getLayout() == null) {
                if (mainTrack == null) {
                    mainTrack = videoTrack;
                }
                if (firstTrack == null) {
                    firstTrack = videoTrack;
                }
                if (secondTrack == null) {
                    secondTrack = videoTrack;
                }
                if (thirdTrack == null) {
                    thirdTrack = videoTrack;
                }

                view.removeAllViews();
                videoTrack.getStringeeVideoTrack().getView(this).setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT, RendererCommon.ScalingType.SCALE_ASPECT_FIT);
                view.addView(videoTrack.getStringeeVideoTrack().getView(this));
                videoTrack.getStringeeVideoTrack().renderView(true);
                if (videoTrack.getStringeeVideoTrack().equals(localVideoTrack)) {
                    videoTrack.getStringeeVideoTrack().getView(this).setMirror(mirror);
                }
                videoTrack.setLayout(view);
            } else {
                //if videoTrack in mainView was removed, replace localVideoTrack to mainView
                if (videoTrack.getStringeeVideoTrack().equals(localVideoTrack)) {
                    if (videoTrack.getLayout().equals(view1)) {
                        firstTrack = null;
                        view1.removeAllViews();
                    }
                    if (videoTrack.getLayout().equals(view2)) {
                        secondTrack = null;
                        view2.removeAllViews();
                    }
                    if (videoTrack.getLayout().equals(view3)) {
                        thirdTrack = null;
                        view3.removeAllViews();
                    }
                    mainTrack = videoTrack;
                    videoTrack.setLayout(mainView);

                    videoTrack.getStringeeVideoTrack().getView(this).setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT, RendererCommon.ScalingType.SCALE_ASPECT_FIT);
                    mainView.removeAllViews();
                    if (videoTrack.getStringeeVideoTrack().getView(this).getParent() != null) {
                        ((FrameLayout) videoTrack.getStringeeVideoTrack().getView(this).getParent()).removeView(videoTrack.getStringeeVideoTrack().getView(this));
                    }
                    mainView.addView(videoTrack.getStringeeVideoTrack().getView(this));
                    videoTrack.getStringeeVideoTrack().renderView(false);
                    if (videoTrack.getStringeeVideoTrack().equals(localVideoTrack)) {
                        videoTrack.getStringeeVideoTrack().getView(this).setMirror(mirror);
                    }
                }
            }
        }
    }

    private StringeeVideoTrack.Listener videoTrackListener(StringeeVideoTrack stringeeVideoTrack) {
        return new StringeeVideoTrack.Listener() {
            @Override
            public void onMediaAvailable() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("StringeeVideo", "stringeeVideoTrack: " + stringeeVideoTrack.getId());
                        //choose view where to add a videoTrack
                        if (firstTrack == null) {
                            v1.setVisibility(View.VISIBLE);
                            view1.removeAllViews();
                            stringeeVideoTrack.getView(RoomActivity.this).setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT, RendererCommon.ScalingType.SCALE_ASPECT_FIT);
                            view1.addView(stringeeVideoTrack.getView(RoomActivity.this));
                            stringeeVideoTrack.renderView(true);
                            stringeeVideoTrack.getView(RoomActivity.this).setMirror(false);
                            firstTrack = new VideoTrack(stringeeVideoTrack, view1);
                            videoTrackList.add(firstTrack);
                        } else if (secondTrack == null) {
                            v2.setVisibility(View.VISIBLE);
                            view2.removeAllViews();
                            stringeeVideoTrack.getView(RoomActivity.this).setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT, RendererCommon.ScalingType.SCALE_ASPECT_FIT);
                            view2.addView(stringeeVideoTrack.getView(RoomActivity.this));
                            stringeeVideoTrack.renderView(true);
                            stringeeVideoTrack.getView(RoomActivity.this).setMirror(false);
                            secondTrack = new VideoTrack(stringeeVideoTrack, view2);
                            videoTrackList.add(secondTrack);
                        } else if (thirdTrack == null) {
                            v3.setVisibility(View.VISIBLE);
                            view3.removeAllViews();
                            stringeeVideoTrack.getView(RoomActivity.this).setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT, RendererCommon.ScalingType.SCALE_ASPECT_FIT);
                            view3.addView(stringeeVideoTrack.getView(RoomActivity.this));
                            stringeeVideoTrack.renderView(true);
                            stringeeVideoTrack.getView(RoomActivity.this).setMirror(false);
                            thirdTrack = new VideoTrack(stringeeVideoTrack, view3);
                            videoTrackList.add(thirdTrack);
                        } else {
                            videoTrackList.add(new VideoTrack(stringeeVideoTrack, null));
                        }

                        adapter.notifyDataSetChanged();

                        if (tvNoParticipant.getVisibility() == View.VISIBLE) {
                            tvNoParticipant.setVisibility(View.GONE);
                            rvParticipant.setVisibility(View.VISIBLE);
                        }
                    }
                });
            }

            @Override
            public void onMediaStateChange(StringeeVideoTrack.MediaState mediaState) {

            }
        };
    }

    private void changeLayoutWithMain(FrameLayout view) {
        VideoTrack trackInMainView = mainTrack;
        VideoTrack trackInView = new VideoTrack();

        if (view.equals(view1)) {
            trackInView = firstTrack;
            firstTrack = mainTrack;
        }
        if (view.equals(view2)) {
            trackInView = secondTrack;
            secondTrack = mainTrack;
        }
        if (view.equals(view3)) {
            trackInView = thirdTrack;
            thirdTrack = mainTrack;
        }

        mainTrack = trackInView;
        mainView.removeAllViews();
        view.removeAllViews();

        //add videoTrack to clickedView
        trackInView.getStringeeVideoTrack().getView(this).setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT, RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        mainView.addView(trackInView.getStringeeVideoTrack().getView(this));
        trackInView.getStringeeVideoTrack().renderView(false);
        if (trackInView.getStringeeVideoTrack().equals(localVideoTrack)) {
            trackInView.getStringeeVideoTrack().getView(this).setMirror(mirror);
        }
        trackInView.setLayout(mainView);

        //add videoTrack to mainView
        trackInMainView.getStringeeVideoTrack().getView(RoomActivity.this).setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT, RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        view.addView(trackInMainView.getStringeeVideoTrack().getView(RoomActivity.this));
        trackInMainView.getStringeeVideoTrack().renderView(true);
        if (trackInMainView.getStringeeVideoTrack().equals(localVideoTrack)) {
            trackInMainView.getStringeeVideoTrack().getView(RoomActivity.this).setMirror(mirror);
        }
        trackInMainView.setLayout(view);
    }

    private void leaveRoom() {
        if (mStringeeRoom != null) {
            //release localVideoTrack
            mStringeeRoom.unpublish(localVideoTrack, new StatusListener() {
                @Override
                public void onSuccess() {
                    localVideoTrack.release();
                }
            });

            //leaveRoom
            mStringeeRoom.leave(true, new StatusListener() {
                @Override
                public void onSuccess() {
                    StringeeVideo.release(mStringeeRoom);
                }
            });
        }
        if (audioManager != null) {
            audioManager.stop();
            audioManager = null;
        }
        finish();
    }
}
