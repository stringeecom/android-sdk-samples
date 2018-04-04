package com.stringee.softphone.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.stringee.softphone.R;
import com.stringee.softphone.activity.ContactDetailActivity;
import com.stringee.softphone.adapter.RecentAdapter;
import com.stringee.softphone.common.CallBack;
import com.stringee.softphone.common.Common;
import com.stringee.softphone.common.Constant;
import com.stringee.softphone.common.DataHandler;
import com.stringee.softphone.common.Notify;
import com.stringee.softphone.common.Utils;
import com.stringee.softphone.model.Contact;
import com.stringee.softphone.model.Recent;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by luannguyen on 7/10/2017.
 */

public class HistoryFragment extends Fragment implements CallBack {

    private ListView lvHistory;
    private TextView tvNoResult;

    private RecentAdapter adapter;
    private List<Recent> recents = new ArrayList<Recent>();

    private BroadcastReceiver updateRecentsReceiver;

    private static final String ACTION_GET_RECENT = "get_recents";

    public HistoryFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_history, container, false);

        tvNoResult = (TextView) layout.findViewById(R.id.tv_no_recents);
        lvHistory = (ListView) layout.findViewById(R.id.lv_history);
        lvHistory.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Recent recent = recents.get(i);
                if (recent.getType() != Constant.TYPE_TIME_HEADER) {
                    Contact contact = new Contact();
                    contact.setName(recent.getName());
                    contact.setPhone(recent.getPhoneNumber());
                    contact.setPhoneNo(recent.getPhoneNo());
                    Intent intent = new Intent(getActivity(), ContactDetailActivity.class);
                    intent.putExtra(Constant.PARAM_CONTACT, contact);
                    startActivity(intent);
                }
            }
        });

        registerReceiver();

        getRecents();

        return layout;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver();
    }

    private void getRecents() {
        if (getActivity() != null) {
            Object[] params = new Object[1];
            params[0] = ACTION_GET_RECENT;
            DataHandler handler = new DataHandler(getActivity(), this);
            handler.execute(params);
        }
    }

    private void doGetRecents() {
        recents = Utils.genHistoryHeader(Common.messageDb.getRecents(), getActivity());
    }

    private void doneGetRecents() {
        if (getActivity() != null) {
            if (recents.size() == 0) {
                lvHistory.setVisibility(View.GONE);
                tvNoResult.setVisibility(View.VISIBLE);
            } else {
                lvHistory.setVisibility(View.VISIBLE);
                adapter = new RecentAdapter(getActivity(), recents);
                lvHistory.setAdapter(adapter);
                tvNoResult.setVisibility(View.GONE);
            }
        }
    }

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter(Notify.UPDATE_RECENTS.getValue());
        updateRecentsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                getRecents();
            }
        };
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(updateRecentsReceiver, filter);
    }

    private void unregisterReceiver() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(updateRecentsReceiver);
    }

    @Override
    public void start() {

    }

    @Override
    public void doWork(Object... params) {
        String strAction = (String) params[0];
        if (strAction.equals(ACTION_GET_RECENT)) {
            doGetRecents();
        }
    }

    @Override
    public void end(Object[] params) {
        String strAction = (String) params[0];
        if (strAction.equals(ACTION_GET_RECENT)) {
            doneGetRecents();
        }
    }
}
