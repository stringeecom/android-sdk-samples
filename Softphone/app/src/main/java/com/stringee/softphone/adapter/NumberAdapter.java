package com.stringee.softphone.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.stringee.softphone.R;

import java.util.List;

/**
 * Created by luannguyen on 3/21/2018.
 */

public class NumberAdapter extends BaseAdapter {

    private List<String> data;
    private LayoutInflater mInflater;
    private int selected;

    public NumberAdapter(Context context, List<String> data) {
        this.data = data;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int i) {
        return data.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    public void setSelected(int selected) {
        this.selected = selected;
        notifyDataSetChanged();
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder holder;
        if (view == null) {
            holder = new ViewHolder();
            view = mInflater.inflate(R.layout.number_row, null);
            holder.tvNumber = (TextView) view.findViewById(R.id.tv_number);
            holder.imSelect = (ImageView) view.findViewById(R.id.im_select);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }
        String number = data.get(i);
        holder.tvNumber.setText(number);
        if (i == selected) {
            holder.imSelect.setVisibility(View.VISIBLE);
        } else {
            holder.imSelect.setVisibility(View.INVISIBLE);
        }
        return view;
    }

    private class ViewHolder {
        TextView tvNumber;
        ImageView imSelect;
    }
}
