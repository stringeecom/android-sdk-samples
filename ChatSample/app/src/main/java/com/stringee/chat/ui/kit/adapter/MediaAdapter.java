package com.stringee.chat.ui.kit.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import com.stringee.chat.ui.kit.activity.ConversationActivity;
import com.stringee.chat.ui.kit.commons.LocalImageLoader;
import com.stringee.chat.ui.kit.fragment.ChatFragment;
import com.stringee.chat.ui.kit.model.DataItem;
import com.stringee.chat.ui.kit.model.Image;
import com.stringee.chat.ui.kit.model.Video;
import com.stringee.stringeechatuikit.R;

import java.io.File;
import java.util.List;

public class MediaAdapter extends Adapter<ViewHolder> {
    private Context mContext;
    private List<DataItem> mObjects;
    private ChatFragment mFragment;

    private static final int IMAGE = 0;
    private static final int VIDEO = 1;
    private static final int CAMERA = 2;


    public MediaAdapter(Context context, ChatFragment fragment, List<DataItem> objects) {
        mContext = context;
        mObjects = objects;
        mFragment = fragment;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return CAMERA;
        }
        if (mObjects.get(position - 1) instanceof Image)
            return IMAGE;
        else if (mObjects.get(position - 1) instanceof Video)
            return VIDEO;
        return -1;

    }

    @androidx.annotation.NonNull
    @Override
    public ViewHolder onCreateViewHolder(@androidx.annotation.NonNull ViewGroup parent, int viewType) {
        LayoutInflater li = LayoutInflater.from(mContext);
        switch (viewType) {
            case CAMERA:
                View itemView2 = li.inflate(R.layout.item_camera, parent, false);
                return new CameraViewHolder(itemView2);
            case IMAGE:
                View itemView0 = li.inflate(R.layout.item_image, parent, false);
                return new ImageViewHolder(itemView0);
            case VIDEO:
                View itemView1 = li.inflate(R.layout.item_video, parent, false);
                return new VideoViewHolder(itemView1);
            default:
                break;
        }
        return null;
    }

    @Override
    public void onBindViewHolder(@androidx.annotation.NonNull ViewHolder holder, int position) {
        switch (getItemViewType(position)) {
            case CAMERA:
                break;
            case IMAGE:
                final Image img = (Image) mObjects.get(position - 1);
                ImageViewHolder imageViewHolder = (ImageViewHolder) holder;
                ((ImageViewHolder) holder).rootView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mFragment.closeDialog();
                        mFragment.sendPhoto(img.getDataPath());
                    }
                });
                LocalImageLoader.getInstance().displayImage("file://" + img.getDataPath(), imageViewHolder.imvImage);
                break;
            case VIDEO:
                final Video video = (Video) mObjects.get(position - 1);
                VideoViewHolder videoHolder = (VideoViewHolder) holder;
                videoHolder.duration.setText(video.getDuration());
                videoHolder.rootView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mFragment.closeDialog();
                        mFragment.sendVideo(new File(video.getDataPath()));
                    }
                });
                LocalImageLoader.getInstance().displayImage("file://" + video.getDataPath(), videoHolder.thumbnail);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return mObjects.size() + 1;
    }

    public class VideoViewHolder extends ViewHolder {
        ImageView thumbnail;
        TextView duration;
        View rootView;

        VideoViewHolder(View itemView) {
            super(itemView);
            rootView = itemView;
            duration = itemView.findViewById(R.id.videoduration);
            thumbnail = itemView.findViewById(R.id.videothumbnail);
        }
    }


    public class ImageViewHolder extends ViewHolder {
        ImageView imvImage;
        View rootView;

        ImageViewHolder(View itemView) {
            super(itemView);
            rootView = itemView;
            imvImage = itemView.findViewById(R.id.imgHinh);
        }
    }


    public class CameraViewHolder extends ViewHolder {

        CameraViewHolder(View itemView) {
            super(itemView);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ((ConversationActivity) mContext).processCameraAction((ConversationActivity) mContext);
                    mFragment.closeDialog();
                }
            });
        }
    }
}

