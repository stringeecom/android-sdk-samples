package com.stringee.softphone.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.stringee.StringeeClient;
import com.stringee.softphone.R;
import com.stringee.softphone.activity.LoginActivity;
import com.stringee.softphone.adapter.NumberAdapter;
import com.stringee.softphone.common.Common;
import com.stringee.softphone.common.Constant;
import com.stringee.softphone.common.Notify;
import com.stringee.softphone.common.PrefUtils;
import com.stringee.softphone.common.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Created by luannguyen on 7/10/2017.
 */

public class SettingsFragment extends Fragment implements View.OnClickListener {

    private ListView lvNumber;
    private TextView tvAmount;

    private NumberAdapter numberAdapter;
    private List<String> data = new ArrayList<>();

    private BroadcastReceiver checkBalanceReceiver;

    public SettingsFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_settings, container, false);

        View vLogout = layout.findViewById(R.id.v_logout);
        vLogout.setOnClickListener(this);

        tvAmount = (TextView) layout.findViewById(R.id.tv_balance);

        ImageView imAvatar = (ImageView) layout.findViewById(R.id.im_avatar);
        Random r = new Random();
        int index = r.nextInt(5);
        Utils.displayAvatar(getActivity(), null, PrefUtils.getInstance(getActivity()).getString(Constant.PREF_USER_ID, ""), imAvatar, index);
        TextView tvName = (TextView) layout.findViewById(R.id.tv_name);
        tvName.setText(PrefUtils.getInstance(getActivity()).getString(Constant.PREF_USER_ID, ""));

        TextView tvVersion = (TextView) layout.findViewById(R.id.tv_version);
        try {
            PackageInfo pInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
            String version = pInfo.versionName;
            tvVersion.setText(version);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        String numbers = PrefUtils.getInstance(getActivity()).getString(Constant.PREF_SIP_NUMBERS, "");
        int selected = 0;
        String selectedNumber = PrefUtils.getInstance(getActivity()).getString(Constant.PREF_SELECTED_NUMBER, "");
        try {
            JSONArray jsonArray = new JSONArray(numbers);
            for (int i = 0; i < jsonArray.length(); i++) {
                String str = jsonArray.getString(i);
                if (str.equals(selectedNumber)) {
                    selected = i;
                }
                data.add(str);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        numberAdapter = new NumberAdapter(getActivity(), data);
        numberAdapter.setSelected(selected);

        lvNumber = (ListView) layout.findViewById(R.id.lv_numbers);
        lvNumber.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                numberAdapter.setSelected(i);
                PrefUtils.getInstance(getActivity()).putString(Constant.PREF_SELECTED_NUMBER, (String) numberAdapter.getItem(i));
            }
        });
        lvNumber.setAdapter(numberAdapter);

        checkBalance();

        registerReceivers();
        return layout;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceivers();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.v_logout:
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(R.string.logout_confirm);
                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (Common.client != null) {
                            Common.client.unregisterPushToken(PrefUtils.getInstance(getActivity()).getString(Constant.PREF_FIREBASE_TOKEN, ""), new StringeeClient.RegisterPushTokenListener() {
                                @Override
                                public void onPushTokenRegistered(boolean b, String s) {

                                }

                                @Override
                                public void onPushTokenUnRegistered(boolean b, String s) {
                                    Log.e("Stringee", "+++++++++++++ unregister token success");
                                    PrefUtils.getInstance(getActivity()).clearData();
                                    Common.messageDb.clearData();
                                    Common.client.disconnect();
                                    Common.client = null;
                                }
                            });
                        }
                        if (Common.checkAppInBackgroundThread != null) {
                            Common.checkAppInBackgroundThread.setRunning(false);
                            Common.checkAppInBackgroundThread = null;
                        }

                        startActivity(new Intent(getActivity(), LoginActivity.class));
                        getActivity().finish();
                    }
                });
                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
                break;
        }
    }

    private void checkBalance() {
        String url = Constant.URL_BASE + Constant.URL_CHECK_BALANCE;
        RequestQueue queue = Volley.newRequestQueue(getActivity());
        final String phone = Utils.formatPhone(PrefUtils.getInstance(getActivity()).getString(Constant.PREF_PHONE_NUMBER, ""));

        Map<String, String> params = new HashMap();
        params.put("phone", phone);
        JSONObject jsonObject = new JSONObject(params);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url
                , jsonObject, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    int status = response.getInt("status");
                    if (status == 200) {
                        JSONObject dataObject = response.getJSONObject("data");
                        String amount = dataObject.getString("amount");
                        tvAmount.setText("$ " + amount);
                    } else {
                        Utils.reportMessage(getActivity(), R.string.error_occured);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Utils.reportMessage(getActivity(), R.string.error_occured);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Utils.reportMessage(getActivity(), R.string.error_occured);
            }
        });
        queue.add(request);
    }

    private void registerReceivers() {
        IntentFilter filter = new IntentFilter(Notify.CHECK_BALANCE.getValue());
        checkBalanceReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                checkBalance();
            }
        };
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(checkBalanceReceiver, filter);
    }

    private void unregisterReceivers() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(checkBalanceReceiver);
    }
}
