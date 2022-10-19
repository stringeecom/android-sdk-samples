package com.stringee.chat.ui.kit.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.stringee.chat.ui.kit.commons.Notify;
import com.stringee.chat.ui.kit.commons.utils.FileUtils;
import com.stringee.chat.ui.kit.commons.utils.FileUtils.FileType;
import com.stringee.chat.ui.kit.commons.utils.PermissionsUtils;
import com.stringee.chat.ui.kit.commons.utils.StringeePermissions;
import com.stringee.chat.ui.kit.fragment.AudioMessageFragment;
import com.stringee.chat.ui.kit.fragment.ChatFragment;
import com.stringee.chat.ui.kit.fragment.ConversationListFragment;
import com.stringee.chat.ui.kit.model.Contact;
import com.stringee.chat.ui.kit.model.DataItem;
import com.stringee.chat.ui.kit.model.Image;
import com.stringee.chat.ui.kit.model.Video;
import com.stringee.messaging.Conversation;
import com.stringee.messaging.Message;
import com.stringee.stringeechatuikit.BaseActivity;
import com.stringee.stringeechatuikit.R;
import com.stringee.stringeechatuikit.common.Utils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ConversationActivity extends BaseActivity {

    RelativeLayout childFragmentLayout;
    protected ActionBar mActionBar;
    private ChatFragment chatFragment;
    private ConversationListFragment conversationListFragment;
    private ImageButton closeBtn;
    private ImageButton deleteBtn;
    private TextView tvNo;
    private View vSelectedMenu;
    private Toolbar myToolbar;

    private InputMethodManager inputMethodManager;
    private int attachType;
    private File mediaFile;
    private int screen = 1; // 1: conversation 2: chat
    private boolean showSelectedMenu;

    public static final int REQUEST_CODE_LOCATION = 1;
    public static final int REQUEST_CODE_TAKE_PHOTO = 2;
    public static final int REQUEST_CODE_FILE = 3;
    public static final int REQUEST_CODE_CAPTURE_VIDEO = 4;
    public static final int REQUEST_CODE_CONTACT_SHARE = 5;
    public static final int REQUEST_CODE_GALLERY = 6;

    private BroadcastReceiver convAddedReceiver;
    private BroadcastReceiver convUpdatedReceiver;
    private BroadcastReceiver convDeletedReceiver;
    private BroadcastReceiver messageAddedReceiver;
    private BroadcastReceiver messageUpdatedReceiver;

    public ConversationActivity() {

    }

    private void addFragment(androidx.fragment.app.FragmentActivity fragmentActivity, Fragment fragmentToAdd, String fragmentTag, boolean addToBackStack) {
        FragmentManager supportFragmentManager = fragmentActivity.getSupportFragmentManager();

        FragmentTransaction fragmentTransaction = supportFragmentManager
                .beginTransaction();
        fragmentTransaction.replace(R.id.layout_child_activity, fragmentToAdd,
                fragmentTag);

        if (addToBackStack) {
            fragmentTransaction.addToBackStack(fragmentTag);
        }
        fragmentTransaction.commitAllowingStateLoss();
        supportFragmentManager.executePendingTransactions();
    }

    public Fragment getFragmentByTag(androidx.fragment.app.FragmentActivity activity, String tag) {
        FragmentManager supportFragmentManager = activity.getSupportFragmentManager();

        if (supportFragmentManager.getBackStackEntryCount() == 0) {
            return null;
        }
        return supportFragmentManager.findFragmentByTag(tag);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_conversation);
        myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        mActionBar = getSupportActionBar();
        mActionBar.setTitle(R.string.conversations);
        mActionBar.setDisplayHomeAsUpEnabled(true);
        mActionBar.setHomeButtonEnabled(true);

        inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);

        vSelectedMenu = findViewById(R.id.v_selected_menu);
        tvNo = findViewById(R.id.noTextView);
        closeBtn = findViewById(R.id.closeMenuBtn);
        closeBtn.setColorFilter(Color.parseColor("#666a6d"));
        closeBtn.setOnClickListener(this);
        deleteBtn = findViewById(R.id.deleteBtn);
        deleteBtn.setColorFilter(Color.parseColor("#666a6d"));
        deleteBtn.setOnClickListener(this);
        childFragmentLayout = (RelativeLayout) findViewById(R.id.layout_child_activity);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            Conversation conversation = (Conversation) extras.getSerializable("conversation");
            String convId = extras.getString("convId");
            if (conversation != null || convId != null) {
                chatFragment = new ChatFragment();
                chatFragment.setArguments(extras);
                addFragment(this, chatFragment, "ChatFragment", true);
            }
        } else if (savedInstanceState != null) {
            chatFragment = (ChatFragment) getSupportFragmentManager().getFragment(savedInstanceState, "ChatFragment");
            if (chatFragment != null) {
                addFragment(this, chatFragment, "ChatFragment", false);
            } else {
                conversationListFragment = new ConversationListFragment();
                addFragment(this, conversationListFragment, "ConversationFragment", false);
            }
        } else {
            conversationListFragment = new ConversationListFragment();
            addFragment(this, conversationListFragment, "ConversationFragment", true);
        }

        registerReceivers();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (chatFragment != null && chatFragment.isAdded()) {
            getSupportFragmentManager().putFragment(outState, "ChatFragment", chatFragment);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceivers();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        menu.findItem(R.id.menu_voice_call).setVisible(false);
        menu.findItem(R.id.menu_video_call).setVisible(false);
        menu.findItem(R.id.menu_info).setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        if (chatFragment != null && chatFragment.keyboardController != null) {
            if (chatFragment.keyboardController.isStickerShow()) {
                chatFragment.keyboardController.closeSticker();
                return;
            }
        }

        if (showSelectedMenu) {
            hideSelectedMenu();
            if (screen == 1 && conversationListFragment != null) {
                conversationListFragment.clearSelected();
            }

            if (screen == 2 && chatFragment != null) {
                chatFragment.clearSelected();
            }
            return;
        }

        if (getSupportFragmentManager().getBackStackEntryCount() == 1) {
            if (chatFragment != null) {
                chatFragment.endChat();
            }
            finish();
            return;
        }


        super.onBackPressed();
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (intent.getExtras() != null) {
            Fragment fragment = getFragmentByTag(this, "ChatFragment");
            if (fragment != null) {
                getSupportFragmentManager().popBackStack("ChatFragment", FragmentManager.POP_BACK_STACK_INCLUSIVE);
            }
            chatFragment = new ChatFragment();
            chatFragment.setArguments(intent.getExtras());
            addFragment(this, chatFragment, "ChatFragment", true);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            ChatFragment fragment = (ChatFragment) getFragmentByTag(ConversationActivity.this, "ChatFragment");
            if (fragment != null) {
                switch (requestCode) {
                    case REQUEST_CODE_LOCATION:
                        double latitude = data.getDoubleExtra("latitude", 0);
                        double longitude = data.getDoubleExtra("longitude", 0);
                        fragment.sendLocation(latitude, longitude);
                        break;
                    case REQUEST_CODE_TAKE_PHOTO:
                        if (mediaFile != null) {
                            fragment.sendPhoto(mediaFile.getAbsolutePath());
                        }
                        break;
                    case REQUEST_CODE_CAPTURE_VIDEO:
                        if (mediaFile != null) {
                            fragment.sendVideo(mediaFile.getAbsolutePath());
                        }
                        break;
                    case REQUEST_CODE_CONTACT_SHARE:
                        Contact contact = (Contact) data.getSerializableExtra("contact");
                        fragment.sendContact(contact);
                        break;
                    case REQUEST_CODE_FILE:
                        String path = data.getStringExtra("path");
                        fragment.sendFile(path);
                        break;
                    case REQUEST_CODE_GALLERY:
                        DataItem dataItem = (DataItem) data.getSerializableExtra("media");
                        if (dataItem instanceof Image) {
                            fragment.sendPhoto(FileUtils.copyFileToCache(this, Uri.parse(dataItem.getDataPath()), FileType.IMAGE));
                        } else if (dataItem instanceof Video) {
                            fragment.sendVideo(FileUtils.copyFileToCache(this, Uri.parse(dataItem.getDataPath()), FileType.VIDEO));
                        }
                        break;
                }
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.closeMenuBtn:
                hideSelectedMenu();
                if (screen == 1 && conversationListFragment != null) {
                    conversationListFragment.clearSelected();
                }

                if (screen == 2 && chatFragment != null) {
                    chatFragment.clearSelected();
                }
                break;
            case R.id.deleteBtn:
                if (screen == 1 && conversationListFragment != null) {
                    conversationListFragment.deleteChats();
                }

                if (screen == 2 && chatFragment != null) {
                    chatFragment.deleteMessages();
                }
                break;
        }
    }

    public void hideKeyboard(View v) {
        if (inputMethodManager.isActive()) {
            inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    public void setAttachType(int attachType) {
        this.attachType = attachType;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PermissionsUtils.REQUEST_CAMERA) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                captureImage(this);
            }
        } else if (requestCode == PermissionsUtils.REQUEST_CAMERA_AUDIO) {
            if (grantResults.length == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                captureVideo(this);
            }
        } else if (requestCode == PermissionsUtils.REQUEST_CONTACT) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent contactIntent = new Intent(this, ContactActivity.class);
                startActivityForResult(contactIntent, REQUEST_CODE_CONTACT_SHARE);
            }
        } else if (requestCode == PermissionsUtils.REQUEST_AUDIO_RECORD) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                FragmentManager supportFragmentManager = getSupportFragmentManager();
                DialogFragment fragment = AudioMessageFragment.newInstance();
                FragmentTransaction fragmentTransaction = supportFragmentManager
                        .beginTransaction().add(fragment, "AudioMessageFragment");

                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commitAllowingStateLoss();
            }
        } else if (requestCode == PermissionsUtils.REQUEST_STORAGE) {
            switch (attachType) {
                case 1:
                    processCamera(this);
                    break;
                case 2:
                    Intent intent = new Intent(this, SelectFileActivity.class);
                    startActivityForResult(intent, REQUEST_CODE_FILE);
                    break;
                case 3:
                    processAudio(this);
                    break;
                case 4:
                    processVideo(this);
                    break;
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void processCamera(Activity activity) {
        if (PermissionsUtils.isCameraPermissionGranted(activity)) {
            Log.d("Stringee", "L3");
            captureImage(activity);
        } else {
            if (Utils.hasMarshmallow() && PermissionsUtils.checkSelfForCameraPermission(activity)) {
                Log.d("Stringee", "L4");
                new StringeePermissions(activity).requestCameraPermission();
            } else {
                captureImage(activity);
            }
        }
    }

    public void processCameraAction(final Activity activity) {
        if (PermissionsUtils.isStoragePermissionGranted(activity)) {
            Log.d("Stringee", "L1");
            processCamera(activity);
        } else {
            Log.d("Stringee", "L2");
            if (Utils.hasMarshmallow() && PermissionsUtils.checkSelfForCameraPermission(activity)) {
                new StringeePermissions(activity).requestStoragePermissions();
            } else {
                processCamera(activity);
            }
        }
    }

    private void processVideo(Activity activity) {
        try {
            if (PermissionsUtils.isCameraPermissionGranted(activity) && PermissionsUtils.isAudioRecordingPermissionGranted(activity)) {
                captureVideo(activity);
            } else {
                if (Utils.hasMarshmallow() && PermissionsUtils.checkPermissionForCameraAndMicrophone(activity)) {
                    new StringeePermissions(activity).requestCameraAndRecordPermission();
                } else {
                    captureVideo(activity);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void processVideoAction(Activity activity) {
        if (PermissionsUtils.isStoragePermissionGranted(activity)) {
            processVideo(activity);
        } else {
            if (Utils.hasMarshmallow() && PermissionsUtils.checkSelfForCameraPermission(activity)) {
                new StringeePermissions(activity).requestStoragePermissions();
            } else {
                processVideo(activity);
            }
        }
    }

    private void captureImage(Activity activity) {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String imageFileName = "photo_" + timeStamp + ".jpeg";

            mediaFile = FileUtils.getFileCachePath(this, imageFileName, FileUtils.FileType.IMAGE);

            Uri capturedImageUri = FileProvider.getUriForFile(activity, getPackageName() + ".provider", mediaFile);

            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, capturedImageUri);

            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
                ClipData clip = ClipData.newUri(activity.getContentResolver(), "a Photo", capturedImageUri);
                cameraIntent.setClipData(clip);
            }
            cameraIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            cameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (cameraIntent.resolveActivity(activity.getApplicationContext().getPackageManager()) != null) {
                if (mediaFile != null) {
                    activity.startActivityForResult(cameraIntent, ConversationActivity.REQUEST_CODE_TAKE_PHOTO);
                }
            }
        } catch (Exception e) {
            Log.e("Stringee", e.getMessage());
            e.printStackTrace();
        }
    }


    private void captureVideo(Activity activity) {
        try {
            Intent videoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String videoFileName = "video_" + timeStamp + ".mp4";

            mediaFile = FileUtils.getFileCachePath(this, videoFileName, FileType.VIDEO);

            Uri videoFileUri = FileProvider.getUriForFile(activity, getPackageName() + ".provider", mediaFile);

            videoIntent.putExtra(MediaStore.EXTRA_OUTPUT, videoFileUri);

            if (VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
                ClipData clip = ClipData.newUri(activity.getContentResolver(), "a Video", videoFileUri);

                videoIntent.setClipData(clip);

            }
            videoIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            videoIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            videoIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);

            if (videoIntent.resolveActivity(activity.getApplicationContext().getPackageManager()) != null) {
                if (mediaFile != null) {
                    activity.startActivityForResult(videoIntent, REQUEST_CODE_CAPTURE_VIDEO);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processAudio(AppCompatActivity activity) {
        if (Utils.hasMarshmallow() && PermissionsUtils.checkSelfPermissionForAudioRecording(activity)) {
            new StringeePermissions(activity).requestAudio();
        } else if (PermissionsUtils.isAudioRecordingPermissionGranted(activity)) {
            FragmentManager supportFragmentManager = activity.getSupportFragmentManager();
            DialogFragment fragment = AudioMessageFragment.newInstance();
            FragmentTransaction fragmentTransaction = supportFragmentManager
                    .beginTransaction().add(fragment, "AudioMessageFragment");
            fragmentTransaction.commitAllowingStateLoss();
        }
    }

    public void processAudioAction(AppCompatActivity activity) {
        if (PermissionsUtils.isStoragePermissionGranted(activity)) {
            processAudio(activity);
        } else {
            if (Utils.hasMarshmallow() && PermissionsUtils.checkSelfForCameraPermission(activity)) {
                new StringeePermissions(activity).requestStoragePermissions();
            } else {
                processAudio(activity);
            }
        }
    }

    public void processContactAction(Activity activity) {
        if (Utils.hasMarshmallow() && PermissionsUtils.checkSelfForContactPermission(activity)) {
            new StringeePermissions(activity).requestContactPermission();
        } else {
            Intent contactIntent = new Intent(this, ContactActivity.class);
            startActivityForResult(contactIntent, REQUEST_CODE_CONTACT_SHARE);
        }
    }

    public void processFileAction(Activity activity) {
        if (Utils.hasMarshmallow() && PermissionsUtils.checkSelfForStoragePermission(activity)) {
            new StringeePermissions(activity).requestStoragePermissions();
        } else {
            Intent intent = new Intent(this, SelectFileActivity.class);
            activity.startActivityForResult(intent, REQUEST_CODE_FILE);
        }
    }

    public void processGalleryAction(Activity activity) {
        if (Utils.hasMarshmallow() && PermissionsUtils.checkSelfForStoragePermission(activity)) {
            new StringeePermissions(activity).requestStoragePermissions();
        } else {
            Intent intent = new Intent(this, ChooseGalleryFolderActivity.class);
            activity.startActivityForResult(intent, REQUEST_CODE_GALLERY);
        }
    }

    private void registerReceivers() {
        IntentFilter filter1 = new IntentFilter(Notify.CONVERSATION_ADDED.getValue());
        convAddedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Conversation conversation = (Conversation) intent.getSerializableExtra("conversation");
                ConversationListFragment fragment = (ConversationListFragment) getFragmentByTag(ConversationActivity.this, "ConversationFragment");
                if (fragment != null) {
                    fragment.onAddConversation(conversation);
                }
            }
        };
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).registerReceiver(convAddedReceiver, filter1);

        IntentFilter filter2 = new IntentFilter(Notify.CONVERSATION_UPDATED.getValue());
        convUpdatedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Conversation conversation = (Conversation) intent.getSerializableExtra("conversation");
                ConversationListFragment fragment = (ConversationListFragment) getFragmentByTag(ConversationActivity.this, "ConversationFragment");
                if (fragment != null) {
                    fragment.onUpdateConversation(conversation);
                }
            }
        };
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).registerReceiver(convUpdatedReceiver, filter2);

        IntentFilter filter5 = new IntentFilter(Notify.CONVERSATION_DELETED.getValue());
        convDeletedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                finish();
            }
        };
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).registerReceiver(convDeletedReceiver, filter5);


        IntentFilter filter3 = new IntentFilter(Notify.MESSAGE_ADDED.getValue());
        messageAddedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Message message = (Message) intent.getSerializableExtra("message");
                ChatFragment fragment = (ChatFragment) getFragmentByTag(ConversationActivity.this, "ChatFragment");
                if (fragment != null) {
                    fragment.onAddMessage(message);
                }
            }
        };
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).registerReceiver(messageAddedReceiver, filter3);


        IntentFilter filter4 = new IntentFilter(Notify.MESSAGE_UPDATED.getValue());
        messageUpdatedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Message message = (Message) intent.getSerializableExtra("message");
                ChatFragment fragment = (ChatFragment) getFragmentByTag(ConversationActivity.this, "ChatFragment");
                if (fragment != null) {
                    fragment.onUpdateMessage(message);
                }
            }
        };
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).registerReceiver(messageUpdatedReceiver, filter4);
    }

    public void unregisterReceivers() {
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).unregisterReceiver(convAddedReceiver);
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).unregisterReceiver(convUpdatedReceiver);
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).unregisterReceiver(convDeletedReceiver);
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).unregisterReceiver(messageAddedReceiver);
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).unregisterReceiver(messageUpdatedReceiver);
    }

    public void hideSelectedMenu() {
        myToolbar.setVisibility(View.VISIBLE);
        vSelectedMenu.setVisibility(View.GONE);
        showSelectedMenu = false;
    }

    public void showSelectedMenu(int screen) {
        this.screen = screen;
        showSelectedMenu = true;
        tvNo.setText("1");
        myToolbar.setVisibility(View.GONE);
        vSelectedMenu.setVisibility(View.VISIBLE);
    }

    public void showSelectedNo(int number) {
        tvNo.setText(String.valueOf(number));
    }
}
