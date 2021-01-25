package com.stringee.chat.ui.kit.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.stringee.chat.ui.kit.commons.LocalImageLoader;
import com.stringee.chat.ui.kit.model.DataItem;
import com.stringee.chat.ui.kit.model.Image;
import com.stringee.chat.ui.kit.model.MediaFolder;
import com.stringee.chat.ui.kit.model.Video;
import com.stringee.stringeechatuikit.R;

import java.util.List;

public class GalleyFileAdapter extends BaseAdapter {
    private Context context;
    private List<DataItem> mediaList;
    private MediaFolder folder;


    public GalleyFileAdapter(Context context, MediaFolder folder) {
        this.context = context;
        this.folder = folder;
        mediaList = folder.getListData();
    }

    @Override
    public int getCount() {
        return mediaList.size();
    }

    @Override
    public Object getItem(int i) {
        return mediaList.get(i);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }


    @NonNull
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = LayoutInflater.from(context).inflate(R.layout.adapter_galleryfile, parent, false);
            viewHolder.iv_image = convertView.findViewById(R.id.iv_image);
            viewHolder.vidDur = convertView.findViewById(R.id.tv_duration_grid);
            viewHolder.infoVideo = convertView.findViewById(R.id.info_video);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        DataItem dataItem = mediaList.get(position);

        if (dataItem instanceof Image) {
            viewHolder.infoVideo.setVisibility(View.GONE);
            LocalImageLoader.getInstance().displayImage("file://" + dataItem.getDataPath(), viewHolder.iv_image);
        }
        if (dataItem instanceof Video) {
            LocalImageLoader.getInstance().displayImage("file://" + dataItem.getDataPath(), viewHolder.iv_image);
            try {
                viewHolder.vidDur.setText(dataItem.getDuration());
            } catch (Exception e) {
                Log.e("Exception::", "Exception", e);
            }
        }
        return convertView;
    }

    private static class ViewHolder {
        TextView vidDur;
        ImageView iv_image;
        RelativeLayout infoVideo;
    }
}
