package com.stringee.chat.ui.kit.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.stringee.chat.ui.kit.commons.LocalImageLoader;
import com.stringee.chat.ui.kit.model.Sticker;
import com.stringee.stringeechatuikit.R;

import java.util.List;

public class StickerAdapter extends BaseAdapter {

    private List<Sticker> stickers;
    private Context mContext;
    private LayoutInflater mInflater;

    public StickerAdapter(Context context, List<Sticker> data) {
        mContext = context;
        mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        stickers = data;
    }

    @Override
    public int getCount() {
        return stickers.size();
    }

    @Override
    public Object getItem(int i) {
        return stickers.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        StickerHolder holder;
        if (view == null) {
            view = mInflater.inflate(R.layout.sticker_cell, null);
            holder = new StickerHolder();
            holder.imSticker = view.findViewById(R.id.im_sticker);
            view.setTag(holder);
        } else {
            holder = (StickerHolder) view.getTag();
        }

        Sticker sticker = stickers.get(i);
        LocalImageLoader.getInstance().displayImage(sticker.getPath(), holder.imSticker);

        return view;
    }

    class StickerHolder {
        ImageView imSticker;
    }
}
