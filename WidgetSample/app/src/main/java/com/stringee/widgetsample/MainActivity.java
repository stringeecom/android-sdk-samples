package com.stringee.widgetsample;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import androidx.lifecycle.ViewModelProvider;

import com.stringee.widgetsample.common.Constant;
import com.stringee.widgetsample.common.PermissionsUtils;
import com.stringee.widgetsample.common.PrefUtils;
import com.stringee.widgetsample.common.Utils;
import com.stringee.widgetsample.databinding.ActivityMainBinding;

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
                    mainViewModel.logout(MainActivity.this);
                } else {
                    moveTaskToBack(true);
                }
            }
        });
    }

    private void setUpViewModel() {
        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);
        mainViewModel.getMsg().observe(this, msg -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show());
        mainViewModel.getNeedRequestPermission().observe(this, needRequestPermission -> {
            if (needRequestPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PermissionsUtils.getInstance().requestNotificationPermissions(this);
            }
        });
        mainViewModel.getMainState().observe(this, mainState -> {
            if (getCurrentFocus() != null) {
                if (getSystemService(Context.INPUT_METHOD_SERVICE) != null) {
                    InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (inputMethodManager != null && getCurrentFocus() != null) {
                        inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.RESULT_UNCHANGED_SHOWN);
                        getCurrentFocus().clearFocus();
                    }
                }
            }
        });
        binding.setMainViewModel(mainViewModel);
        binding.setLifecycleOwner(this);

        String token = PrefUtils.getInstance(this).getString(Constant.PREF_TOKEN, "");
        if (!Utils.isStringEmpty(token)) {
            mainViewModel.getAccessToken().setValue(token);
            mainViewModel.connect(this);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !PermissionsUtils.getInstance().checkSelfNotificationPermission(this)) {
            mainViewModel.getNeedRequestPermission().setValue(true);
        }
    }

    private void setUpUI() {
        binding.btnConnect.setOnClickListener(v -> {
            if (mainViewModel.getMainState().getValue() == MainViewModel.MainState.CONNECTED) {
                mainViewModel.logout(this);
            } else {
                mainViewModel.connect(this);
            }
        });
        binding.btnVoiceCall.setOnClickListener(v -> mainViewModel.makeCall(this, false));
        binding.btnVideoCall.setOnClickListener(v -> mainViewModel.makeCall(this, true));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean isGranted = PermissionsUtils.getInstance().verifyPermissions(grantResults);
        if (requestCode == PermissionsUtils.REQUEST_NOTIFICATION_PERMISSION && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!isGranted) {
                if (PermissionsUtils.getInstance().shouldRequestNotificationPermissionRationale(this)) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(R.string.app_name);
                    builder.setMessage("Permissions must be granted for show incoming call notification");
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
            }
        }
    }
}