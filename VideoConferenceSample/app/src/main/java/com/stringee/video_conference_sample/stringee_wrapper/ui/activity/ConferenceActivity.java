package com.stringee.video_conference_sample.stringee_wrapper.ui.activity;

import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.stringee.video.StringeeVideoTrack;
import com.stringee.video_conference_sample.R;
import com.stringee.video_conference_sample.databinding.ActivityConferenceBinding;
import com.stringee.video_conference_sample.stringee_wrapper.adapter.ViewPagerAdapter;
import com.stringee.video_conference_sample.stringee_wrapper.common.NotificationUtils;
import com.stringee.video_conference_sample.stringee_wrapper.common.PermissionsUtils;
import com.stringee.video_conference_sample.stringee_wrapper.common.listener.OnPageNeedData;
import com.stringee.video_conference_sample.stringee_wrapper.common.listener.OnTrackViewClick;
import com.stringee.video_conference_sample.stringee_wrapper.service.MyMediaProjectionService;
import com.stringee.video_conference_sample.stringee_wrapper.ui.activity.view_model.ConferenceViewModel;
import com.stringee.video_conference_sample.stringee_wrapper.ui.fragment.TrackListFragment;

import java.util.ArrayList;
import java.util.List;

public class ConferenceActivity extends AppCompatActivity implements OnPageNeedData, OnTrackViewClick {
    private ConferenceViewModel conferenceViewModel;
    private ActivityConferenceBinding binding;
    private ViewPagerAdapter adapter;
    private ActivityResultLauncher<Intent> activityResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityConferenceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setUpUI();
        setUpViewModel();

        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == AppCompatActivity.RESULT_OK && result.getData() != null) {
                Intent intent = new Intent(this, MyMediaProjectionService.class);
                intent.setAction(NotificationUtils.ACTION_START_FOREGROUND_SERVICE);
                intent.putExtras(result.getData());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent);
                } else {
                    startService(intent);
                }
            }
        });
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {

            }
        });

        initRoom();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean isGranted = PermissionsUtils.getInstance().verifyPermissions(grantResults);
        if (requestCode == PermissionsUtils.REQUEST_NOTIFICATION_PERMISSION) {
            if (!isGranted) {
                if (PermissionsUtils.getInstance().shouldRequestConferencePermissionRationale(this)) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(R.string.app_name);
                    builder.setMessage("Permissions must be granted for start share screen");
                    builder.setPositiveButton("Ok", (dialogInterface, id) -> dialogInterface.cancel());
                    builder.setNegativeButton("Settings", (dialogInterface, id) -> {
                        dialogInterface.cancel();
                        // open app setting
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivity(intent);
                    });
                    builder.create().show();
                }
            } else {
                conferenceViewModel.prepareShareScreen(this, activityResultLauncher, getSystemService(MediaProjectionManager.class));
            }
        }
    }

    private void setUpViewModel() {
        conferenceViewModel = new ViewModelProvider(this).get(ConferenceViewModel.class);
        conferenceViewModel.getFinishActivity().observe(this, shouldFinish -> {
            if (Boolean.TRUE.equals(shouldFinish)) {
                finish();
            }
        });
        conferenceViewModel.getPageAddTrack().observe(this, pageAddTrack -> {
            if (pageAddTrack == binding.vp.getCurrentItem() + 1) {
                TrackListFragment currentFragment = (TrackListFragment) adapter.getFragment(pageAddTrack - 1);
                currentFragment.onResume();
            }
        });
        conferenceViewModel.getPageRemoveTrack().observe(this, pageRemoveTrack -> {
            if (pageRemoveTrack <= binding.vp.getCurrentItem() + 1) {
                TrackListFragment currentFragment = (TrackListFragment) adapter.getFragment(binding.vp.getCurrentItem());
                currentFragment.onResume();
            }
        });
        conferenceViewModel.getAddPage().observe(this, page -> {
            adapter.addFragment(new TrackListFragment().setPageNumber(page).setOnPageNeedData(this).setOnTrackViewClick(this));
            adapter.notifyItemInserted(page);
        });
        conferenceViewModel.getRemovePage().observe(this, page -> {
            adapter.removeFragment(page);
            adapter.notifyItemRemoved(page);
        });
        binding.setConferenceViewModel(conferenceViewModel);
        binding.setLifecycleOwner(this);
    }

    private void setUpUI() {
        adapter = new ViewPagerAdapter(this);
        adapter.addFragment(new TrackListFragment().setPageNumber(1).setOnPageNeedData(this).setOnTrackViewClick(this));
        binding.vp.setAdapter(adapter);
        binding.ci.setViewPager(binding.vp);
        adapter.registerAdapterDataObserver(binding.ci.getAdapterDataObserver());
        binding.flControl.setOnClickListener(v -> conferenceViewModel.changeControlDisplay());
        binding.imbSwitch.setOnClickListener(v -> conferenceViewModel.switchCamera(this));
        binding.imbMic.setOnClickListener(v -> conferenceViewModel.enableMic(this));
        binding.imbVideo.setOnClickListener(v -> conferenceViewModel.enableCamera(this));
        binding.imbShare.setOnClickListener(v -> {
            if (Boolean.TRUE.equals(conferenceViewModel.getIsSharing().getValue())) {
                conferenceViewModel.stopSharing(this);
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (!PermissionsUtils.getInstance().checkSelfNotificationPermission(this)) {
                        PermissionsUtils.getInstance().requestNotificationPermissions(this);
                        return;
                    }
                }
                conferenceViewModel.prepareShareScreen(this, activityResultLauncher, getSystemService(MediaProjectionManager.class));
            }
        });
        binding.imbLeave.setOnClickListener(v -> conferenceViewModel.leave(this));
    }

    private void initRoom() {
        conferenceViewModel.initRoom(this);
        conferenceViewModel.displayLocalTrack(this, binding.flLocal);
        if (getIntent() != null) {
            conferenceViewModel.getIsMicOn().setValue(getIntent().getBooleanExtra("is_mic_on", true));
            conferenceViewModel.getIsVideoOn().setValue(getIntent().getBooleanExtra("is_video_on", true));
        }
    }

    @Override
    public void onPage(int pageNumber) {
        List<StringeeVideoTrack> videoTracks = new ArrayList<>();
        for (int i = (pageNumber - 1) * 6; i < pageNumber * 6; i++) {
            if (i >= conferenceViewModel.getVideoTracks().size()) {
                break;
            }
            videoTracks.add(conferenceViewModel.getVideoTracks().get(i));
        }
        TrackListFragment fragment = (TrackListFragment) adapter.getFragment(pageNumber - 1);
        fragment.initAdapter(videoTracks);
    }

    @Override
    public void onClick() {
        conferenceViewModel.changeControlDisplay();
    }
}