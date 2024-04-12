package com.stringee.video_conference_sample.stringee_wrapper.ui.activity.view_model;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.view.Gravity;
import android.widget.FrameLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.lifecycle.MutableLiveData;

import com.google.android.flexbox.FlexboxLayoutManager;
import com.stringee.video.StringeeVideoTrack;
import com.stringee.video.TextureViewRenderer;
import com.stringee.video_conference_sample.stringee_wrapper.common.Utils;
import com.stringee.video_conference_sample.stringee_wrapper.ui.base.MyViewModel;
import com.stringee.video_conference_sample.stringee_wrapper.wrapper.StringeeWrapper;
import com.stringee.video_conference_sample.stringee_wrapper.wrapper.listener.ConferenceListener;

import java.util.ArrayList;
import java.util.List;

public class ConferenceViewModel extends MyViewModel {
    private final MutableLiveData<Boolean> isShowControl = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> isVideoOn = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> isMicOn = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> isSharing = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> finishActivity = new MutableLiveData<>();
    private final MutableLiveData<Integer> pageAddTrack = new MutableLiveData<>();
    private final MutableLiveData<Integer> pageRemoveTrack = new MutableLiveData<>();
    private final MutableLiveData<Integer> addPage = new MutableLiveData<>();
    private final MutableLiveData<Integer> removePage = new MutableLiveData<>();
    private final MutableLiveData<Integer> totalPages = new MutableLiveData<>(1);
    private final List<StringeeVideoTrack> videoTracks = new ArrayList<>();

    public MutableLiveData<Boolean> getIsVideoOn() {
        return isVideoOn;
    }

    public MutableLiveData<Boolean> getIsShowControl() {
        return isShowControl;
    }

    public MutableLiveData<Boolean> getIsSharing() {
        return isSharing;
    }

    public MutableLiveData<Boolean> getIsMicOn() {
        return isMicOn;
    }

    public MutableLiveData<Boolean> getFinishActivity() {
        return finishActivity;
    }

    public MutableLiveData<Integer> getPageAddTrack() {
        return pageAddTrack;
    }

    public MutableLiveData<Integer> getPageRemoveTrack() {
        return pageRemoveTrack;
    }

    public MutableLiveData<Integer> getAddPage() {
        return addPage;
    }

    public MutableLiveData<Integer> getRemovePage() {
        return removePage;
    }

    public List<StringeeVideoTrack> getVideoTracks() {
        return videoTracks;
    }

    public MutableLiveData<Integer> getTotalPages() {
        return totalPages;
    }

    public void enableMic(Context context) {
        boolean isOn = Boolean.FALSE.equals(isMicOn.getValue());
        StringeeWrapper.getInstance(context).getConferenceWrapper().enableMic(isOn);
        isMicOn.setValue(isOn);
    }

    public void enableCamera(Context context) {
        boolean isOn = Boolean.FALSE.equals(isVideoOn.getValue());
        StringeeWrapper.getInstance(context).getConferenceWrapper().enableVideo(isOn);
        isVideoOn.setValue(isOn);
    }

    public void switchCamera(Context context) {
        StringeeWrapper.getInstance(context).getConferenceWrapper().switchCamera();
    }

    public void changeControlDisplay() {
        isShowControl.setValue(Boolean.FALSE.equals(isShowControl.getValue()));
    }

    public void stopSharing(Context context) {
        if (Boolean.TRUE.equals(isSharing.getValue())) {
            StringeeWrapper.getInstance(context).getConferenceWrapper().stopSharing();
        }
    }

    public void prepareShareScreen(Activity activity, ActivityResultLauncher<Intent> activityResultLauncher, MediaProjectionManager manager) {
        if (Boolean.FALSE.equals(isSharing.getValue())) {
            StringeeWrapper.getInstance(activity).getConferenceWrapper().prepareShareScreen(activity);
            activityResultLauncher.launch(manager.createScreenCaptureIntent());
        }
    }

    public void displayLocalTrack(Context context, FrameLayout flPreview) {
        StringeeWrapper.getInstance(context).getConferenceWrapper().displayLocalTrack(flPreview);
    }

    public void leave(Context context) {
        new AlertDialog.Builder(context)
                .setTitle("Leave the room")
                .setMessage("Leave the room at this device or all devices")
                .setPositiveButton("Leave the room on this device", (dialog, which) -> {
                    StringeeWrapper.getInstance(context).getConferenceWrapper().leaveRoom(false);
                    dialog.dismiss();
                })
                .setNegativeButton("Leave the room on all devices", (dialog, which) -> {
                    StringeeWrapper.getInstance(context).getConferenceWrapper().leaveRoom(true);
                    dialog.dismiss();
                })
                .show();
    }

    public void initRoom(Context context) {
        StringeeWrapper.getInstance(context).getConferenceWrapper().connectRoom();
        StringeeWrapper.getInstance(context).getConferenceWrapper().setConferenceListener(new ConferenceListener() {
            @Override
            public void onTrackAdded(StringeeVideoTrack stringeeVideoTrack) {
                Utils.runOnUiThread(() -> {
                    videoTracks.add(stringeeVideoTrack);
                    int newTotalPage = getTotalPage();
                    if (newTotalPage > totalPages.getValue()) {
                        addPage.setValue(newTotalPage);
                    }
                    pageAddTrack.setValue(newTotalPage);
                });
            }

            @Override
            public void onTrackRemoved(StringeeVideoTrack stringeeVideoTrack) {
                Utils.runOnUiThread(() -> {
                    if (videoTracks.isEmpty()) {
                        return;
                    }
                    int pageRemoveTrack = -1;
                    for (int i = 0; i < videoTracks.size(); i++) {
                        if (videoTracks.get(i).getId().equals(stringeeVideoTrack.getId())) {
                            pageRemoveTrack = i / 6 + 1;
                            videoTracks.remove(i);
                            break;
                        }
                    }
                    ConferenceViewModel.this.pageRemoveTrack.setValue(pageRemoveTrack);
                    int newTotalPage = getTotalPage();
                    if (newTotalPage < totalPages.getValue()) {
                        removePage.setValue(newTotalPage);
                    }
                });
            }

            @Override
            public void onLeaveRoom() {
                Utils.runOnUiThread(() -> {
                    StringeeWrapper.getInstance(context).releaseRoom();
                    finishActivity.setValue(true);
                });
            }

            @Override
            public void onSharingScreen(boolean onSharing) {
                Utils.runOnUiThread(() -> isSharing.setValue(onSharing));
            }
        });
    }

    private int getTotalPage() {
        return videoTracks.size() % 6 == 0 ? videoTracks.size() / 6 : videoTracks.size() / 6 + 1;
    }
}