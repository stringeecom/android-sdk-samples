package com.stringee.chat.ui.kit.adapter;

import android.R.id;
import android.R.layout;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.stringee.messaging.Queue;

import java.util.List;

public class QueueAdapter extends ArrayAdapter<Queue> {
    private List<Queue> items;

    public QueueAdapter(@NonNull Context context, List<Queue> items) {
        super(context, layout.simple_spinner_dropdown_item, id.text1, items);
        this.items = items;
    }

    @Override
    public View getDropDownView(int position, @androidx.annotation.Nullable View convertView, @NonNull ViewGroup parent) {
        TextView v = (TextView) super.getView(position, convertView, parent);
        Queue queue = getItem(position);
        v.setText(queue.getName());
        return v;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            v = LayoutInflater.from(getContext()).inflate(layout.simple_spinner_item, null);
        }
        TextView lbl = v.findViewById(id.text1);
        lbl.setText(getItem(position).getName());
        return v;
    }

}
