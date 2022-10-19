package com.stringee.chat.ui.kit.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.stringee.chat.ui.kit.commons.LocalImageLoader;
import com.stringee.chat.ui.kit.model.MediaFolder;
import com.stringee.stringeechatuikit.R;

import java.util.List;

public class GalleryFolderAdapter extends BaseAdapter {
    private Context context;
    private List<MediaFolder> data;


    public GalleryFolderAdapter(Context context, List<MediaFolder> folders) {
        this.data = folders;
        this.context = context;
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public MediaFolder getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }


    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = LayoutInflater.from(context).inflate(R.layout.adapter_galleryfolder, parent, false);
            viewHolder.tvFolderName = convertView.findViewById(R.id.tv_folder_name);
            viewHolder.tvFolderSize = convertView.findViewById(R.id.tv_folder_size);
            viewHolder.folderImageView = convertView.findViewById(R.id.iv_imageFolder);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.tvFolderName.setText(data.get(position).getName());
        viewHolder.tvFolderSize.setText(data.get(position).getListData().size() + "");
        LocalImageLoader.getInstance().displayImage(data.get(position).getListData().get(0).getDataPath(), viewHolder.folderImageView);
        return convertView;
    }

    private static class ViewHolder {
        TextView tvFolderName, tvFolderSize;
        ImageView folderImageView;
    }
}
