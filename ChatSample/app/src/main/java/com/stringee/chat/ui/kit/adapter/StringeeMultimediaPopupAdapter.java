package com.stringee.chat.ui.kit.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.stringee.stringeechatuikit.R;

import java.util.List;

/**
 * Created by reytum on 18/3/16.
 */
public class StringeeMultimediaPopupAdapter extends BaseAdapter {
    private Context context;
    private TypedArray multimediaIcons;
    private List<String> multimediaText;

    public StringeeMultimediaPopupAdapter(Context context, TypedArray multimediaIcons, List<String> multimediaText) {
        this.context = context;
        this.multimediaIcons = multimediaIcons;
        this.multimediaText = multimediaText;
    }

    @Override
    public int getCount() {
        return multimediaText.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint({"ViewHolder", "InflateParams"})
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        convertView = inflater.inflate(R.layout.stringee_multimedia_option_item, null);
        ImageView img = convertView.findViewById(R.id.iv_item);
        TextView text = convertView.findViewById(R.id.tv_mult_imedia);
        img.setImageResource(multimediaIcons.getResourceId(position, -1));
        text.setText(multimediaText.get(position));
        return convertView;
    }

}
