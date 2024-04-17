package com.stringee.video_conference_sample.stringee_wrapper.ui.activity;

import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.stringee.video_conference_sample.databinding.ActivityCheckDeviceBinding;
import com.stringee.video_conference_sample.stringee_wrapper.ui.activity.view_model.CheckDeviceViewModel;

public class CheckDeviceActivity extends AppCompatActivity {
    private CheckDeviceViewModel checkDeviceViewModel;
    private ActivityCheckDeviceBinding binding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCheckDeviceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setUpUI();
        setUpViewModel();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                checkDeviceViewModel.releaseTrack(CheckDeviceActivity.this);
                finish();
            }
        });
    }

    private void setUpViewModel() {
        checkDeviceViewModel = new ViewModelProvider(this).get(CheckDeviceViewModel.class);
        checkDeviceViewModel.displayLocalTrack(this, binding.flPreview);
        binding.setCheckDeviceViewModel(checkDeviceViewModel);
        binding.setLifecycleOwner(this);
    }

    private void setUpUI() {
        binding.btnStart.setOnClickListener(v -> {
            checkDeviceViewModel.start(this);
            finish();
        });
        binding.imbBack.setOnClickListener(v -> {
            checkDeviceViewModel.releaseTrack(this);
            finish();
        });
        binding.imbSwitch.setOnClickListener(v -> checkDeviceViewModel.switchCamera(this));
        binding.imbMic.setOnClickListener(v -> checkDeviceViewModel.enableMic(this));
        binding.imbVideo.setOnClickListener(v -> checkDeviceViewModel.enableCamera(this));
    }
}