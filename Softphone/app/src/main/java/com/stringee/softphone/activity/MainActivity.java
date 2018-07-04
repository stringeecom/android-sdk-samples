package com.stringee.softphone.activity;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.iid.FirebaseInstanceId;
import com.stringee.StringeeClient;
import com.stringee.call.StringeeCall;
import com.stringee.exception.StringeeError;
import com.stringee.listener.StringeeConnectionListener;
import com.stringee.softphone.R;
import com.stringee.softphone.common.Common;
import com.stringee.softphone.common.Constant;
import com.stringee.softphone.common.DataHandler;
import com.stringee.softphone.common.DateTimeUtils;
import com.stringee.softphone.common.Notify;
import com.stringee.softphone.common.NotifyUtils;
import com.stringee.softphone.common.PrefUtils;
import com.stringee.softphone.common.Utils;
import com.stringee.softphone.fragment.ContactsFragment;
import com.stringee.softphone.fragment.DialFragment;
import com.stringee.softphone.fragment.HistoryFragment;
import com.stringee.softphone.fragment.SearchFragment;
import com.stringee.softphone.fragment.SettingsFragment;
import com.stringee.softphone.model.Contact;
import com.stringee.softphone.model.Message;
import com.stringee.softphone.service.CheckAppInBackgroundThread;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends MActivity {

    public ViewPager viewPager;
    private ViewPagerAdapter viewPagerAdapter;
    public TabLayout tabLayout;
    private View btnDial;
    private LinearLayout vConnect;
    private TextView tvNoConnect;
    private ProgressBar prLoading;

    private final int TAB_HISTORY = 0;
    private final int TAB_CONTACTS = 1;
    private final int TAB_SETTINGS = 2;

    private boolean isFromPush;

    private final String ACTION_SAVE_CALL = "save_call";

    private BroadcastReceiver endCallReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        isFromPush = getIntent().getBooleanExtra(Constant.PARAM_FROM_PUSH, false);

        initViews();
        initViewPager();
        initStringee();

        long expiredTime = 1000 * PrefUtils.getInstance(this).getLong(Constant.PREF_EXPIRED_TIME, 0);
        long currentTime = System.currentTimeMillis();
        if (currentTime > expiredTime) {
            getTokenAndConnect(this);
        } else {
            Common.isConnecting = true;
            Common.client.connect(PrefUtils.getInstance(this).getString(Constant.PREF_ACCESS_TOKEN, ""));
        }

        if (isFromPush) {
            registerReceiver();
        }

        NotificationManager nm = (NotificationManager) getSystemService
                (NOTIFICATION_SERVICE);
        nm.cancel(25061987);
        nm.cancel(10021993);

        Common.checkAppInBackgroundThread = new CheckAppInBackgroundThread();
        if (!Common.checkAppInBackgroundThread.isRunning()) {
            Common.checkAppInBackgroundThread.start();
        }

    }

    @Override
    public void onBackPressed() {
        Fragment searchFragment = getSupportFragmentManager().findFragmentByTag("SEARCH");
        if (searchFragment != null) {
            getSupportFragmentManager().beginTransaction().remove(searchFragment).commit();
        } else {
            Fragment dialFragment = getSupportFragmentManager().findFragmentByTag("DIAL");
            if (dialFragment != null) {
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.setCustomAnimations(R.anim.slide_in_up, R.anim.slide_out_up);
                ft.remove(dialFragment).commit();
            } else {
                moveTaskToBack(true);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver();

        NotificationManager nm = (NotificationManager) getSystemService
                (NOTIFICATION_SERVICE);
        nm.cancel(25061987);
        nm.cancel(10021993);
    }

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter(Notify.END_CALL.getValue());
        endCallReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Common.client != null) {
                    Common.client.disconnect();
                    Common.client = null;
                }
                finish();
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(endCallReceiver, filter);
    }

    private void unregisterReceiver() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(endCallReceiver);
    }

    private void initViews() {
        vConnect = (LinearLayout) findViewById(R.id.v_connecting);
        tvNoConnect = (TextView) findViewById(R.id.tv_no_connection);
        prLoading = (ProgressBar) findViewById(R.id.pr_loading);
        tabLayout = (TabLayout) findViewById(R.id.tabs);
        viewPager = (ViewPager) findViewById(R.id.v_pager);
        btnDial = findViewById(R.id.btn_dial);

        View menuSearch = findViewById(R.id.menu_search);
        menuSearch.setOnClickListener(this);

        FloatingActionButton btnDial = (FloatingActionButton) findViewById(R.id.btn_dial);
        btnDial.setOnClickListener(this);
    }

    private void initViewPager() {
        viewPager.setOffscreenPageLimit(2);
        viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        viewPagerAdapter.addFragment(new HistoryFragment());
        viewPagerAdapter.addFragment(new ContactsFragment());
        viewPagerAdapter.addFragment(new SettingsFragment());

        viewPager.setAdapter(viewPagerAdapter);
        tabLayout.setupWithViewPager(viewPager);
        viewPager.setCurrentItem(0);

        View tabHistory = LayoutInflater.from(this).inflate(R.layout.tab, null);
        ImageView imTabHistory = (ImageView) tabHistory.findViewById(R.id.im_tab_icon);
        imTabHistory.setImageResource(R.drawable.tab_history_selector);
        tabLayout.getTabAt(TAB_HISTORY).setCustomView(tabHistory);

        View tabContacts = LayoutInflater.from(this).inflate(R.layout.tab, null);
        ImageView imTabContacts = (ImageView) tabContacts.findViewById(R.id.im_tab_icon);
        imTabContacts.setImageResource(R.drawable.tab_contacts_selector);
        tabLayout.getTabAt(TAB_CONTACTS).setCustomView(tabContacts);

        View tabSettings = LayoutInflater.from(this).inflate(R.layout.tab, null);
        ImageView imTabSettings = (ImageView) tabSettings.findViewById(R.id.im_tab_icon);
        imTabSettings.setImageResource(R.drawable.tab_settings_selector);
        tabLayout.getTabAt(TAB_SETTINGS).setCustomView(tabSettings);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.menu_search:
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.add(R.id.v_search, new SearchFragment(), "SEARCH").commit();
                break;
            case R.id.btn_dial:
                FragmentTransaction ft1 = getSupportFragmentManager().beginTransaction();
                ft1.setCustomAnimations(R.anim.slide_in_up, R.anim.slide_out_up);
                ft1.add(R.id.v_dial, new DialFragment(), "DIAL").commit();
                break;
        }
    }

    private class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<Fragment>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment) {
            mFragmentList.add(fragment);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return null;
        }

        public Fragment getFragment(int position) {
            return mFragmentList.get(position);
        }
    }

    private void initStringee() {
        Common.client = new StringeeClient(this);
        Common.client.setConnectionListener(new StringeeConnectionListener() {
            @Override
            public void onConnectionConnected(final StringeeClient stringeeClient, boolean isReconnecting) {
                Common.isConnecting = false;
                Common.lastTime = System.currentTimeMillis();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Common.alreadyConnected = true;
                        prLoading.setVisibility(View.GONE);
                        vConnect.setBackgroundColor(Color.parseColor("#4ccc1f"));
                        tvNoConnect.setText(R.string.softphone_connected);
                        vConnect.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                vConnect.setVisibility(View.GONE);
                            }
                        }, 2000);
                    }
                });

                if (!PrefUtils.getInstance(MainActivity.this).getBoolean(Constant.PREF_TOKEN_REGISTERED, false)) {
                    final String refreshedToken = FirebaseInstanceId.getInstance().getToken();
                    Common.client.registerPushToken(refreshedToken, new StringeeClient.RegisterPushTokenListener() {
                        @Override
                        public void onPushTokenRegistered(boolean success, String desc) {
                            if (success) {
                                PrefUtils.getInstance(MainActivity.this).putBoolean(Constant.PREF_TOKEN_REGISTERED, true);
                                PrefUtils.getInstance(MainActivity.this).putString(Constant.PREF_FIREBASE_TOKEN, refreshedToken);
                            }
                        }

                        @Override
                        public void onPushTokenUnRegistered(boolean success, String desc) {
                        }
                    });
                }
            }

            @Override
            public void onConnectionDisconnected(StringeeClient stringeeClient, final boolean isReconnecting) {
                Common.isConnecting = false;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        vConnect.setVisibility(View.VISIBLE);
                        prLoading.setVisibility(View.VISIBLE);
                        vConnect.setBackgroundColor(Color.parseColor("#ff9b31"));
                        tvNoConnect.setText(R.string.softphone_connecting);
                    }
                });
            }

            @Override
            public void onIncomingCall(final StringeeCall stringeeCall) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Common.callMap.put(stringeeCall.getCallId(), stringeeCall);
                        String from = stringeeCall.getFrom();
                        if (Common.isInCall) {
                            stringeeCall.reject();
                            saveCall(from);
                        } else {
                            Intent intent = new Intent(MainActivity.this, IncomingCallActivity.class);
                            intent.putExtra(Constant.PARAM_PHONE, from);
                            intent.putExtra(Constant.PARAM_CALL_ID, stringeeCall.getCallId());
                            intent.putExtra(Constant.PARAM_FROM_PUSH, isFromPush);
                            startActivity(intent);
                        }
                    }
                });
            }

            @Override
            public void onConnectionError(StringeeClient stringeeClient, StringeeError stringeeError) {

            }

            @Override
            public void onRequestNewToken(StringeeClient stringeeClient) {
                getTokenAndConnect(MainActivity.this);
            }
        });
    }

    public static void getTokenAndConnect(final Context context) {
        String url = Constant.URL_BASE + Constant.URL_GET_ACCESS_TOKEN;
        Map<String, String> params = new HashMap();
        params.put("token", PrefUtils.getInstance(context).getString(Constant.PREF_TOKEN, ""));
        JSONObject jsonObject = new JSONObject(params);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url
                , jsonObject, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    int status = response.getInt("status");
                    if (status == 200) {
                        JSONObject dataObject = response.getJSONObject("data");
                        String token = dataObject.getString("access_token");
                        long expiredTime = dataObject.getLong("expire_time");
                        PrefUtils.getInstance(context).putString(Constant.PREF_ACCESS_TOKEN, token);
                        PrefUtils.getInstance(context).putLong(Constant.PREF_EXPIRED_TIME, expiredTime);
                        Common.isConnecting = true;
                        Common.client.connect(token);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Common.isConnecting = true;
                Common.client.connect(PrefUtils.getInstance(context).getString(Constant.PREF_ACCESS_TOKEN, ""));
            }
        });
        RequestQueue queue = Volley.newRequestQueue(context);
        queue.add(request);
    }

    private void saveCall(String from) {
        Object[] params = new Object[2];
        params[0] = ACTION_SAVE_CALL;
        params[1] = from;
        DataHandler handler = new DataHandler(this, this);
        handler.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
    }

    private void doSaveCall(String from) {
        SimpleDateFormat format = new SimpleDateFormat(Constant.DATETIME_FORMAT);
        Date date = new Date();
        String strTime = format.format(date);
        Message mMessage = new Message(Common.userId, 0, "00:00", strTime, Constant.TYPE_MISSED_CALL,
                Constant.CHAT_TYPE_PRIVATE);
        String name = "";
        List<Contact> contacts;
        mMessage.setPhoneNumber(from);
        contacts = Utils.getContactsFromDevice(this);
        for (int i = 0; i < contacts.size(); i++) {
            Contact contact = contacts.get(i);
            if (Utils.formatPhone(from).equals(Utils.formatPhone(contact.getPhone()))) {
                name = contact.getName();
                break;
            }
        }
        mMessage.setFullname(name);
        mMessage.setMsgId(++Common.messageId);
        mMessage.setIsRead(Constant.MESSAGE_READ);
        mMessage.setState(Constant.MESSAGE_SENT);
        mMessage.setShortDate(DateTimeUtils.getTime(date));
        int id = Common.messageDb.insertMessage(mMessage);
        mMessage.setId(id);
        NotifyUtils.showNotification(this, mMessage);
    }

    @Override
    public void start() {
    }

    @Override
    public void doWork(Object... params) {
        String strAction = (String) params[0];
        if (strAction.equals(ACTION_SAVE_CALL)) {
            doSaveCall((String) params[1]);
        }
    }

    @Override
    public void end(Object[] params) {
        String strAction = (String) params[0];
        if (strAction.equals(ACTION_SAVE_CALL)) {
            NotifyUtils.notifyUpdateRecents();
        }
    }
}
