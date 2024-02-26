package com.stringee.chat.ui.kit.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.stringee.chat.ui.kit.commons.Notify;
import com.stringee.chat.ui.kit.commons.utils.FileUtils;
import com.stringee.chat.ui.kit.commons.utils.FileUtils.FileType;
import com.stringee.chat.ui.kit.commons.utils.PermissionsUtils;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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
        menu.findItem(R.id.menu_info).setVisible(false);
        menu.findItem(com.stringee.stringeechatuikit.R.id.menu_end_chat).setVisible(false);
        menu.findItem(com.stringee.stringeechatuikit.R.id.menu_rate).setVisible(false);
        menu.findItem(com.stringee.stringeechatuikit.R.id.menu_email_chat_transcript).setVisible(false);
        menu.findItem(com.stringee.stringeechatuikit.R.id.menu_edit_info).setVisible(false);
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
                        Uri uri = data.getData();
                        String filePath = FileUtils.copyFileToCache(ConversationActivity.this, uri, FileType.OTHER);
                        Message message = new Message(Message.Type.FILE);
                        FileUtils.getFileInfoFromUri(ConversationActivity.this, uri, message);
                        message.setFilePath(filePath);
                        fragment.sendFile(message);
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PermissionsUtils.REQUEST_CAPTURE_IMAGE) {
            if (PermissionsUtils.verifyPermissions(grantResults)) {
                captureImage(this);
            }
        } else if (requestCode == PermissionsUtils.REQUEST_CAPTURE_VIDEO) {
            if (PermissionsUtils.verifyPermissions(grantResults)) {
                captureVideo(this);
            }
        } else if (requestCode == PermissionsUtils.REQUEST_RECORD_AUDIO) {
            if (PermissionsUtils.verifyPermissions(grantResults)) {
                processAudio(this);
            }
        } else if (requestCode == PermissionsUtils.REQUEST_CONTACT) {
            if (PermissionsUtils.verifyPermissions(grantResults)) {
                progressContact(this);
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public void processCameraAction(final Activity activity) {
        if (PermissionsUtils.getInstance().checkSelfForCaptureImagePermission(activity)) {
            captureImage(activity);
        } else {
            PermissionsUtils.getInstance().requestPermissions(activity, PermissionsUtils.PERMISSION_CAPTURE_IMAGE, PermissionsUtils.REQUEST_CAPTURE_IMAGE);
        }
    }

    private void captureImage(Activity activity) {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String imageFileName = "photo_" + timeStamp + ".jpeg";

            mediaFile = FileUtils.getFileCachePath(this, imageFileName, FileUtils.FileType.IMAGE);

            Uri capturedImageUri = FileProvider.getUriForFile(activity, getPackageName() + ".provider", mediaFile);

            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, capturedImageUri);
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

    public void processVideoAction(Activity activity) {
        if (PermissionsUtils.getInstance().checkSelfForCaptureVideoPermission(activity)) {
            captureVideo(activity);
        } else {
            PermissionsUtils.getInstance().requestPermissions(activity, PermissionsUtils.PERMISSION_CAPTURE_VIDEO, PermissionsUtils.REQUEST_CAPTURE_VIDEO);
        }
    }

    private void captureVideo(Activity activity) {
        try {
            Intent videoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String videoFileName = "video_" + timeStamp + ".mp4";

            mediaFile = FileUtils.getFileCachePath(this, videoFileName, FileType.VIDEO);

            Uri videoFileUri = FileProvider.getUriForFile(activity, getPackageName() + ".provider", mediaFile);

            videoIntent.putExtra(MediaStore.EXTRA_OUTPUT, videoFileUri);
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
        FragmentManager supportFragmentManager = activity.getSupportFragmentManager();
        DialogFragment fragment = AudioMessageFragment.newInstance();
        FragmentTransaction fragmentTransaction = supportFragmentManager
                .beginTransaction().add(fragment, "AudioMessageFragment");
        fragmentTransaction.commitAllowingStateLoss();
    }

    public void processAudioAction(AppCompatActivity activity) {
        if (Utils.hasMarshmallow() && PermissionsUtils.getInstance().checkSelfForRecordAudioPermission(activity)) {
            processAudio(activity);
        } else {
            PermissionsUtils.getInstance().requestPermissions(activity, PermissionsUtils.PERMISSIONS_RECORD_AUDIO, PermissionsUtils.REQUEST_RECORD_AUDIO);
        }
    }

    public void processContactAction(Activity activity) {
        if (Utils.hasMarshmallow() && PermissionsUtils.getInstance().checkSelfForContactPermission(activity)) {
            progressContact(activity);
        } else {
            PermissionsUtils.getInstance().requestPermissions(activity, PermissionsUtils.PERMISSION_CONTACT, PermissionsUtils.REQUEST_CONTACT);
        }
    }

    private void progressContact(Activity activity) {
        Intent contactIntent = new Intent(activity, ContactActivity.class);
        startActivityForResult(contactIntent, REQUEST_CODE_CONTACT_SHARE);
    }

    public void processFileAction(Activity activity) {
        Intent intent = new Intent().setType("*/*").setAction(Intent.ACTION_GET_CONTENT);
        activity.startActivityForResult(intent, REQUEST_CODE_FILE);
    }

    public void processGalleryAction(Activity activity) {
        Intent intent = new Intent(this, ChooseGalleryFolderActivity.class);
        activity.startActivityForResult(intent, REQUEST_CODE_GALLERY);
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
        LocalBroadcastManager.getInstance(this).registerReceiver(convAddedReceiver, filter1);

        IntentFilter filter2 = new IntentFilter(Notify.CONVERSATION_UPDATED.getValue());
        convUpdatedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Conversation conversation = (Conversation) intent.getSerializableExtra("conversation");
                ConversationListFragment fragment = (ConversationListFragment) getFragmentByTag(ConversationActivity.this, "ConversationFragment");
                if (fragment != null) {
                    fragment.onUpdateConversation(conversation);
                }
                ChatFragment chatFragment1 = (ChatFragment) getFragmentByTag(ConversationActivity.this, "ChatFragment");
                if (chatFragment1 != null) {
                    chatFragment1.onUpdateConversation(conversation);
                }
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(convUpdatedReceiver, filter2);

        IntentFilter filter5 = new IntentFilter(Notify.CONVERSATION_DELETED.getValue());
        convDeletedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Conversation conversation = (Conversation) intent.getSerializableExtra("conversation");
                ConversationListFragment fragment = (ConversationListFragment) getFragmentByTag(ConversationActivity.this, "ConversationFragment");
                if (fragment != null) {
                    fragment.onConversationDeleted(conversation);
                }
                finish();
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(convDeletedReceiver, filter5);


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
        LocalBroadcastManager.getInstance(this).registerReceiver(messageAddedReceiver, filter3);


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
        LocalBroadcastManager.getInstance(this).registerReceiver(messageUpdatedReceiver, filter4);
    }

    public void unregisterReceivers() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(convAddedReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(convUpdatedReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(convDeletedReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageAddedReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageUpdatedReceiver);
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
