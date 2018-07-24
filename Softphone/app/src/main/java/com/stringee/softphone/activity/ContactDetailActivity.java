package com.stringee.softphone.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.stringee.softphone.R;
import com.stringee.softphone.adapter.CallAdapter;
import com.stringee.softphone.common.Common;
import com.stringee.softphone.common.Constant;
import com.stringee.softphone.common.DataHandler;
import com.stringee.softphone.common.Notify;
import com.stringee.softphone.common.PrefUtils;
import com.stringee.softphone.common.Utils;
import com.stringee.softphone.model.Contact;
import com.stringee.softphone.model.Message;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Created by luannguyen on 7/20/2017.
 */

public class ContactDetailActivity extends MActivity {

    private ListView lvHistory;
    private View vVoiceCall;
    private View vVideoCall;

    private Contact contact;
    private List<Message> messages = new ArrayList<>();
    private CallAdapter adapter;

    private BroadcastReceiver updateRecentsReceiver;

    private final String ACTION_GET_RECENTS = "get_recents";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contact_detail);

        contact = getIntent().getParcelableExtra(Constant.PARAM_CONTACT);

        initActionBar();
        initViews();

        registerReceiver();

        getRecents();

        checkPhone();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(Constant.PARAM_CONTACT, contact);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return true;
    }

    private void initActionBar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(0xffffffff);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(R.string.contact_detail);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_back_white);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    private void initViews() {
        ImageView imAvatar = (ImageView) findViewById(R.id.im_avatar);
        TextView tvName = (TextView) findViewById(R.id.tv_name);
        String name = contact.getName();
        if (name != null) {
            tvName.setText(name);
            Random r = new Random();
            int index = r.nextInt(5);
            Utils.displayAvatar(this, contact.getAvatar(), name, imAvatar, index);
        }

        TextView tvPhone = (TextView) findViewById(R.id.tv_phone);
        String phone = contact.getPhoneNo();
        if (phone != null) {
            tvPhone.setText(phone);
        } else {
            tvPhone.setVisibility(View.GONE);
        }

        lvHistory = (ListView) findViewById(R.id.lv_history);

        View header = ((LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.contact_detail_header, null);
        lvHistory.addHeaderView(header);

        vVoiceCall = header.findViewById(R.id.v_voice_call);
        vVoiceCall.setOnClickListener(this);

        View vCallout = header.findViewById(R.id.v_call_out);
        vCallout.setOnClickListener(this);

        vVideoCall = header.findViewById(R.id.v_video_call);
        vVideoCall.setOnClickListener(this);
    }

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter(Notify.UPDATE_RECENTS.getValue());
        updateRecentsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                getRecents();
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(updateRecentsReceiver, filter);
    }

    private void unregisterReceiver() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(updateRecentsReceiver);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.v_voice_call:
                makecall(false, false);
                break;

            case R.id.v_call_out:
                makecall(true, false);
                break;

            case R.id.v_video_call:
                makecall(false, true);
                break;

        }
    }

    private void makecall(boolean isAppToPhone, boolean isVideoCall) {
        String phone = contact.getPhone();
        if (phone != null && phone.length() > 0 && !phone.equalsIgnoreCase("null")) {
            phone = Utils.formatPhone(phone);
            if (Utils.isPhoneNumber(phone)) {
//                if (Utils.isNetworkAvailable(this)) {
//                    if (Common.client.isConnected()) {
                Intent intent = new Intent(this, OutgoingCallActivity.class);
                intent.putExtra(Constant.PARAM_NAME, contact.getName());
                intent.putExtra(Constant.PARAM_PHONE, phone);
                intent.putExtra(Constant.PARAM_CALLOUT, isAppToPhone);
                intent.putExtra(Constant.PARAM_VIDEO_CALL, isVideoCall);
                intent.putExtra(Constant.PARAM_PHONE_NO, contact.getPhoneNo());
                startActivity(intent);
//                    } else {
//                        Utils.reportMessage(this, R.string.stringee_not_connect);
//                    }
//                } else {
//                    Utils.reportMessage(this, R.string.network_required);
//                }
            }
        }
    }

    private void getRecents() {
        Object[] params = new Object[1];
        params[0] = ACTION_GET_RECENTS;
        DataHandler handler = new DataHandler(this, this);
        handler.execute(params);
    }

    private void doGetRecents() {
        String phone = contact.getPhone();
        if (phone == null || phone.equalsIgnoreCase("null")) {
            phone = "";
        }
        messages = Common.messageDb.getMessageCallByPhone(Utils.formatPhone(phone));
    }

    private void doneGetRecents() {
        adapter = new CallAdapter(this, messages);
        lvHistory.setAdapter(adapter);
    }

    private void checkPhone() {
        String url = Constant.URL_BASE + Constant.URL_CHECK_PHONEBOOK;
        RequestQueue queue = Volley.newRequestQueue(this);
        final String token = PrefUtils.getInstance(this).getString(Constant.PREF_TOKEN, "");
        JSONObject contactObject = new JSONObject();
        try {
            contactObject.put(Utils.formatPhone(contact.getPhone()), contact.getName());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Map<String, Object> params = new HashMap();
        params.put("token", token);
        params.put("phonebook", contactObject);
        JSONObject jsonObject = new JSONObject(params);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url
                , jsonObject, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                dismissProgressDialog();
                try {
                    int status = response.getInt("status");
                    if (status == 200) {
                        JSONObject dataObject = response.getJSONObject("data");
                        int count = dataObject.getInt("countPhoneExisted");
                        if (count > 0) {
                            vVideoCall.setVisibility(View.VISIBLE);
                            vVoiceCall.setVisibility(View.VISIBLE);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                dismissProgressDialog();
                Utils.reportMessage(ContactDetailActivity.this, R.string.error_occured);
            }
        });
        queue.add(request);
    }

    @Override
    public void start() {
    }

    @Override
    public void doWork(Object... params) {
        String strAction = (String) params[0];
        if (strAction.equals(ACTION_GET_RECENTS)) {
            doGetRecents();
        }
    }

    @Override
    public void end(Object[] params) {
        String strAction = (String) params[0];
        if (strAction.equals(ACTION_GET_RECENTS)) {
            doneGetRecents();
        }
    }
}
