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
import com.stringee.softphone.common.DateTimeUtils;
import com.stringee.softphone.model.Message;

import java.util.List;

/**
 * Created by luannguyen on 7/27/2017.
 */

public class CallAdapter extends BaseAdapter {

    private List<Message> data;
    private Context context;
    private LayoutInflater inflater;

    public CallAdapter(Context context, List<Message> data) {
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
        CallHolder holder;
        Message message = data.get(pos);
        if (view == null) {
            view = inflater.inflate(R.layout.call_row, null);
            holder = new CallHolder();
            holder.tvName = (TextView) view.findViewById(R.id.tv_name);
            holder.tvDuration = (TextView) view.findViewById(R.id.tv_duration);
            holder.imCall = (ImageView) view.findViewById(R.id.im_call);
            view.setTag(holder);
        } else {
            holder = (CallHolder) view.getTag();
        }

        holder.tvName.setText(DateTimeUtils.getHeaderTime(message.getDatetime(), context) + "  " + message.getShortDate());
        holder.tvDuration.setText(message.getText());
        if (message.getType() == Constant.TYPE_CALL_OUT || message.getType() == Constant.TYPE_OUTGOING_CALL) {
            holder.imCall.setImageResource(R.drawable.outgoing_call);
        } else if (message.getType() == Constant.TYPE_CALL_PHONE_TO_APP) {
            holder.imCall.setImageResource(R.drawable.incoming_call);
        } else if (message.getType() == Constant.TYPE_MISSED_CALL) {
            holder.imCall.setImageResource(R.drawable.miss_call);
        }
        return view;
    }

    private class CallHolder {
        ImageView imCall;
        TextView tvName;
        TextView tvDuration;
    }
}
