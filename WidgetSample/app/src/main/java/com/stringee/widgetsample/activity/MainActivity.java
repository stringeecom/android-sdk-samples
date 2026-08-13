package com.stringee.widgetsample.activity;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.splashscreen.SplashScreen;
import androidx.lifecycle.ViewModelProvider;

import com.stringee.widgetsample.databinding.ActivityMainBinding;
import com.stringee.widgetsample.R;
import com.stringee.widgetsample.manager.MainViewModel;
import com.stringee.widget.common.PermissionsUtils;

/**
 * Host activity for the Widget sample.
 *
 * <p>The activity only renders connection state, collects call parameters, and delegates all
 * Stringee operations to {@link MainViewModel}. The Stringee Widget lifecycle remains outside the
 * UI layer.</p>
 */
public class MainActivity extends AppCompatActivity {
    private MainViewModel mainViewModel;
    private ActivityMainBinding binding;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setUpUI();
        setUpViewModel();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (mainViewModel.getMainState().getValue() == MainViewModel.MainState.CONNECTED) {
                    mainViewModel.disconnect();
                } else {
                    moveTaskToBack(true);
                }
            }
        });
    }

    private void setUpViewModel() {
        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);
        mainViewModel.getMsg().observe(this, msg -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show());
        mainViewModel.getMainState().observe(this, mainState -> {
            if (getCurrentFocus() != null) {
                if (getSystemService(Context.INPUT_METHOD_SERVICE) != null) {
                    InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (inputMethodManager != null && getCurrentFocus() != null) {
                        inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                        getCurrentFocus().clearFocus();
                    }
                }
            }
            renderConnectionState(mainState);
        });
        binding.setMainViewModel(mainViewModel);
        binding.setLifecycleOwner(this);

        mainViewModel.initialize(this);
    }

    private void setUpUI() {
        binding.btnConnect.setOnClickListener(v -> mainViewModel.connect());
        binding.btnDisconnect.setOnClickListener(v -> mainViewModel.disconnect());
        binding.btnVoiceCall.setOnClickListener(v -> mainViewModel.makeCall(false));
        binding.btnVideoCall.setOnClickListener(v -> mainViewModel.makeCall(true));
        binding.btnNotificationPermission.setOnClickListener(v -> PermissionsUtils.getInstance().requestNotificationPermission(this));
        binding.btnFullScreenPermission.setOnClickListener(v -> PermissionsUtils.getInstance().openFullScreenIntentSettings(this));
    }

    private void renderConnectionState(MainViewModel.MainState state) {
        int background;
        int textColor;
        if (state == MainViewModel.MainState.CONNECTED) {
            background = R.drawable.bg_stringee_status_success;
            textColor = R.color.stringee_green_dark;
        } else if (state == MainViewModel.MainState.ERROR) {
            background = R.drawable.bg_stringee_status_error;
            textColor = R.color.stringee_error;
        } else {
            background = R.drawable.bg_stringee_status_info;
            textColor = R.color.stringee_blue;
        }
        binding.layoutConnectionStatus.setBackgroundResource(background);
        binding.tvConnectStatus.setTextColor(ContextCompat.getColor(this, textColor));
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderIncomingCallAccess();
    }

    private void renderIncomingCallAccess() {
        PermissionsUtils permissions = PermissionsUtils.getInstance();
        boolean notifications = permissions.isNotificationPermissionGranted(this);
        boolean fullScreen = permissions.canUseFullScreenIntent(this);
        binding.layoutIncomingCallAccess.setVisibility(notifications && fullScreen ? android.view.View.GONE : android.view.View.VISIBLE);
        binding.tvIncomingCallAccess.setText(getString(R.string.incoming_access_state, getString(notifications ? R.string.granted : R.string.not_granted), getString(fullScreen ? R.string.granted : R.string.not_granted)));
        binding.btnNotificationPermission.setVisibility(notifications ? android.view.View.GONE : android.view.View.VISIBLE);
        binding.btnFullScreenPermission.setVisibility(fullScreen ? android.view.View.GONE : android.view.View.VISIBLE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean isGranted = PermissionsUtils.getInstance().verifyPermissions(grantResults);
        if (requestCode == PermissionsUtils.REQUEST_NOTIFICATION_PERMISSION && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!isGranted) {
                if (PermissionsUtils.getInstance().shouldOpenSettings(this, PermissionsUtils.getInstance().getMissingNotificationPermissions(this))) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(R.string.app_name);
                    builder.setMessage("Permissions must be granted for show incoming call notification");
                    builder.setPositiveButton("Ok", (dialogInterface, id) -> dialogInterface.cancel());
                    builder.setNegativeButton("Settings", (dialogInterface, id) -> {
                        dialogInterface.cancel();
                        PermissionsUtils.getInstance().openAppSettings(this);
                    });
                    builder.create().show();
                }
            }
            renderIncomingCallAccess();
        }
    }
}
