package com.stringee.softphone.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.stringee.softphone.R;
import com.stringee.softphone.common.Constant;
import com.stringee.softphone.common.Utils;
import com.stringee.softphone.model.Recent;

import java.util.List;

public class RecentAdapter extends BaseAdapter {
    private List<Recent> data;
    private Context context;
    private LayoutInflater inflater;

    public RecentAdapter(Context context, List<Recent> data) {
        this.context = context;
        this.data = data;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int pos) {
        return data.get(pos);
    }

    @Override
    public long getItemId(int pos) {
        return pos;
    }

    @Override
    public View getView(int pos, View view, ViewGroup arg2) {
        RecentHolder holder;
        Recent recent = data.get(pos);
        if (view == null) {
            view = inflater.inflate(R.layout.history_row, null);
            holder = new RecentHolder();
            holder.vHistory = view.findViewById(R.id.v_history);
            holder.vHeader = view.findViewById(R.id.v_header);
            holder.imAvatar = (ImageView) view.findViewById(R.id.im_avatar);
            holder.tvName = (TextView) view.findViewById(R.id.tv_name);
            holder.tvTime = (TextView) view.findViewById(R.id.tv_time);
            holder.tvDuration = (TextView) view.findViewById(R.id.tv_duration);
            holder.tvHeader = (TextView) view.findViewById(R.id.tv_header);
            holder.imCall = (ImageView) view.findViewById(R.id.im_call);
            view.setTag(holder);
        } else {
            holder = (RecentHolder) view.getTag();
        }

        if (recent.getType() == Constant.TYPE_TIME_HEADER) {
            holder.vHistory.setVisibility(View.GONE);
            holder.vHeader.setVisibility(View.VISIBLE);
            holder.tvHeader.setText(recent.getText());
        } else {
            holder.vHistory.setVisibility(View.VISIBLE);
            holder.vHeader.setVisibility(View.GONE);
            String name = recent.getName();
            if (name == null) {
                if (recent.getType() == Constant.TYPE_OUTGOING_CALL || recent.getType() == Constant.TYPE_INCOMING_CALL) {
                    name = recent.getAgentId();
                } else {
                    name = recent.getPhoneNumber();
                }
            }
            holder.tvName.setText(name + " (" + recent.getNumCall() + ")");
            holder.tvDuration.setText(recent.getText());
            holder.tvTime.setText(recent.getShortDate());
            Utils.displayAvatar(context, recent.getAvatar(), name, holder.imAvatar, pos);
            if (recent.getType() == Constant.TYPE_CALL_OUT || recent.getType() == Constant.TYPE_OUTGOING_CALL) {
                holder.imCall.setImageResource(R.drawable.outgoing_call);
            } else if (recent.getType() == Constant.TYPE_CALL_PHONE_TO_APP) {
                holder.imCall.setImageResource(R.drawable.incoming_call);
            } else if (recent.getType() == Constant.TYPE_MISSED_CALL) {
                holder.imCall.setImageResource(R.drawable.miss_call);
            }
        }
        return view;
    }

    private class RecentHolder {
        ImageView imAvatar;
        ImageView imCall;
        TextView tvName;
        TextView tvTime;
        TextView tvDuration;
        TextView tvHeader;
        View vHistory;
        View vHeader;
    }
}
