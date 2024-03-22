package com.stringee.stringeechatuikit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.stringee.chat.ui.kit.activity.ConversationActivity;
import com.stringee.chat.ui.kit.activity.LiveChatActivity;
import com.stringee.chat.ui.kit.commons.Notify;
import com.stringee.chat.ui.kit.fragment.ChatWithDialogFragment;
import com.stringee.chat.ui.kit.fragment.CreateGroupFragment;
import com.stringee.listener.StatusListener;
import com.stringee.stringeechatuikit.common.Common;
import com.stringee.stringeechatuikit.common.Constant;
import com.stringee.stringeechatuikit.common.PrefUtils;

public class MainChatActivity extends BaseActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    private BroadcastReceiver connectReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_chat);

        NavigationDrawerFragment mNavigationDrawerFragment = (NavigationDrawerFragment) getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);

        // Set up the drawer.
        if (mNavigationDrawerFragment != null) {
            mNavigationDrawerFragment.setUp(R.id.navigation_drawer, findViewById(R.id.drawer_layout));
        }

        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.container, new MainChatFragment()).commit();

        connectReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(PrefUtils.getString(Constant.PREF_NAME, Common.client.getUserId()));
                }
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(connectReceiver, new IntentFilter(Notify.CONNECTION_CONNECTED.getValue()));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getSupportActionBar() != null) {
            if (Common.client != null && Common.client.isConnected()) {
                getSupportActionBar().setTitle(PrefUtils.getString(Constant.PREF_NAME, Common.client.getUserId()));
            } else {
                getSupportActionBar().setTitle(R.string.connecting);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(connectReceiver);
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        switch (position) {
            case 0:
                if (Common.client != null) {
                    Intent intent = new Intent(this, ConversationActivity.class);
                    startActivity(intent);
                }
                break;
            case 1:
                String pushToken = PrefUtils.getString(Constant.PREF_PUSH_TOKEN, "");
                PrefUtils.clear();
                if (Common.client != null) {
                    Common.client.clearDb();
                    Common.client.unregisterPushToken(pushToken, new StatusListener() {
                        @Override
                        public void onSuccess() {

                        }
                    });
                    Common.client.disconnect();
                    Common.client = null;
                }
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(R.string.connecting);
                }
                finish();
                break;
        }
    }

    public void goToConversations(View v) {
        Intent intent = new Intent(this, ConversationActivity.class);
        startActivity(intent);
    }

    public void chatWith(View v) {
        FragmentManager supportFragmentManager = getSupportFragmentManager();
        DialogFragment fragment = new ChatWithDialogFragment();
        FragmentTransaction fragmentTransaction = supportFragmentManager.beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentByTag("ChatWithDialogFragment");
        if (prev != null) {
            fragmentTransaction.remove(prev);
        }
        fragmentTransaction.addToBackStack(null);
        fragment.show(fragmentTransaction, "ChatWithDialogFragment");
    }

    public void createGroup(View v) {
        FragmentManager supportFragmentManager = getSupportFragmentManager();
        DialogFragment fragment = new CreateGroupFragment();
        FragmentTransaction fragmentTransaction = supportFragmentManager.beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentByTag("CreateGroupFragment");
        if (prev != null) {
            fragmentTransaction.remove(prev);
        }
        fragmentTransaction.addToBackStack(null);
        fragment.show(fragmentTransaction, "CreateGroupFragment");
    }

    public void liveChat(View v) {
        Intent intent = new Intent(this, LiveChatActivity.class);
        startActivity(intent);
    }
}
