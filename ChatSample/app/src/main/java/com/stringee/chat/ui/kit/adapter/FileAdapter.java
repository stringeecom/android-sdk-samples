package com.stringee.chat.ui.kit.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.stringee.chat.ui.kit.commons.LocalImageLoader;
import com.stringee.chat.ui.kit.model.StringeeFile;
import com.stringee.stringeechatuikit.R;

import java.util.List;

public class FileAdapter extends BaseAdapter {

    private Context mContext;
    private LayoutInflater mInflater;
    private List<StringeeFile> files;

    public FileAdapter(Context context, List<StringeeFile> files) {
        mContext = context;
        mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.files = files;
    }

    @Override
    public int getCount() {
        return files.size();
    }

    @Override
    public Object getItem(int position) {
        return files.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void setFiles(List<StringeeFile> files) {
        this.files = files;
        notifyDataSetChanged();
    }

    @SuppressLint("ViewHolder")
    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        ViewHolder holder;
        convertView = mInflater.inflate(R.layout.file_row, null);
        holder = new ViewHolder();
        holder.tvFileName = (TextView) convertView.findViewById(R.id.tv_file_name);
        holder.tvFileSum = (TextView) convertView.findViewById(R.id.tv_file_sum);
        holder.imFileType = (ImageView) convertView.findViewById(R.id.im_type_file);
        holder.imFileType.setColorFilter(Color.parseColor("#999999"), PorterDuff.Mode.SRC_IN);
        holder.cbSelect = (CheckBox) convertView.findViewById(R.id.cb_select);
        holder.imFile = (ImageView) convertView.findViewById(R.id.im_file);

        StringeeFile file = files.get(position);
        holder.tvFileName.setText(file.getName());
        if (file.getSize() > 0) {
            holder.tvFileSum.setText(file.getSum());
        } else {
            if (file.getLevel() == 0) {
                holder.tvFileSum.setText(file.getPath());
            } else {
                if (file.getType() == StringeeFile.TYPE_BACK) {
                    if (file.getLevel() <= 1) {
                        holder.tvFileSum.setText(R.string.folder);
                    } else {
                        holder.tvFileSum.setText(file.getPath());
                    }
                } else {
                    holder.tvFileSum.setText(R.string.folder);
                }
            }
        }

        holder.cbSelect.setChecked(file.isChecked());

        switch (file.getType()) {
            case StringeeFile.TYPE_BACK:
            case StringeeFile.TYPE_DIRECTORY: {
                holder.imFileType.setImageResource(R.drawable.ic_type_folder);
            }
            break;
            case StringeeFile.TYPE_DOCUMENT: {
                holder.imFileType.setImageResource(R.drawable.ic_type_document);
            }
            break;
            case StringeeFile.TYPE_IMAGE: {
                holder.imFileType.setVisibility(View.GONE);
                holder.imFile.setVisibility(View.VISIBLE);
                LocalImageLoader.getInstance().displayImage("file:///" + file.getPath(), holder.imFile);
            }
            break;
            case StringeeFile.TYPE_VIDEO: {
                holder.imFileType.setImageResource(R.drawable.ic_type_image);
            }
            break;
            case StringeeFile.TYPE_MEDIA: {
                holder.imFileType.setImageResource(R.drawable.ic_type_audio);
            }
            break;
            case StringeeFile.TYPE_ZIP: {
                holder.imFileType.setImageResource(R.drawable.ic_type_zip);
            }
            break;
            default: {
                holder.imFileType.setImageResource(R.drawable.ic_type_other);
            }
            break;
        }
        return convertView;
    }

    private class ViewHolder {
        TextView tvFileName;
        TextView tvFileSum;
        ImageView imFileType;
        CheckBox cbSelect;
        ImageView imFile;
    }

}
