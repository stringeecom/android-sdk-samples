package com.stringee.stringeechatuikit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;

import com.stringee.chat.ui.kit.activity.ConversationActivity;
import com.stringee.chat.ui.kit.commons.Notify;
import com.stringee.chat.ui.kit.fragment.ChatWithDialogFragment;
import com.stringee.chat.ui.kit.fragment.CreateGroupFragment;
import com.stringee.listener.StatusListener;
import com.stringee.stringeechatuikit.common.Common;
import com.stringee.stringeechatuikit.common.Constant;
import com.stringee.stringeechatuikit.common.PrefUtils;

public class MainChatActivity extends BaseActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    private NavigationDrawerFragment mNavigationDrawerFragment;
    private BroadcastReceiver connectReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_chat);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (androidx.drawerlayout.widget.DrawerLayout) findViewById(R.id.drawer_layout));

        androidx.fragment.app.FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, new MainChatFragment())
                .commit();

        connectReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                getSupportActionBar().setTitle(PrefUtils.getString(Constant.PREF_NAME, Common.client.getUserId()));
            }
        };
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).registerReceiver(connectReceiver, new IntentFilter(Notify.CONNECTION_CONNECTED.getValue()));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (Common.client != null && Common.client.isConnected()) {
            getSupportActionBar().setTitle(PrefUtils.getString(Constant.PREF_NAME, Common.client.getUserId()));
        } else {
            getSupportActionBar().setTitle(R.string.connecting);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).unregisterReceiver(connectReceiver);
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        switch (position) {
            case 0:
                Intent intent = new Intent(this, ConversationActivity.class);
                startActivity(intent);
                break;
            case 1:
                String pushToken = PrefUtils.getString(Constant.PREF_PUSH_TOKEN, "");
                PrefUtils.clear();
                Common.client.clearDb();
                Common.client.unregisterPushToken(pushToken, new StatusListener() {
                    @Override
                    public void onSuccess() {
                        Common.client.disconnect();
                        Common.client = null;
                    }
                });
                finish();
                break;
        }
    }

    public void goToConversations(View v) {
        Intent intent = new Intent(this, ConversationActivity.class);
        startActivity(intent);
    }

    public void chatWith(View v) {
        androidx.fragment.app.FragmentManager supportFragmentManager = getSupportFragmentManager();
        androidx.fragment.app.DialogFragment fragment = new ChatWithDialogFragment();
        androidx.fragment.app.FragmentTransaction fragmentTransaction = supportFragmentManager
                .beginTransaction();
        androidx.fragment.app.Fragment prev = getSupportFragmentManager().findFragmentByTag("ChatWithDialogFragment");
        if (prev != null) {
            fragmentTransaction.remove(prev);
        }
        fragmentTransaction.addToBackStack(null);
        fragment.show(fragmentTransaction, "ChatWithDialogFragment");
    }

    public void createGroup(View v) {
        androidx.fragment.app.FragmentManager supportFragmentManager = getSupportFragmentManager();
        androidx.fragment.app.DialogFragment fragment = new CreateGroupFragment();
        androidx.fragment.app.FragmentTransaction fragmentTransaction = supportFragmentManager
                .beginTransaction();
        androidx.fragment.app.Fragment prev = getSupportFragmentManager().findFragmentByTag("CreateGroupFragment");
        if (prev != null) {
            fragmentTransaction.remove(prev);
        }
        fragmentTransaction.addToBackStack(null);
        fragment.show(fragmentTransaction, "CreateGroupFragment");
    }
}
