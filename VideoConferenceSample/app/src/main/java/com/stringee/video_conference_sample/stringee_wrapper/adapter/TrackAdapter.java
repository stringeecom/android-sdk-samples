package com.stringee.video_conference_sample.stringee_wrapper.adapter;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.flexbox.FlexboxLayoutManager;
import com.stringee.video.StringeeVideoTrack;
import com.stringee.video.TextureViewRenderer;
import com.stringee.video_conference_sample.stringee_wrapper.common.Utils;
import com.stringee.video_conference_sample.databinding.ListItemTrackBinding;
import com.stringee.video_conference_sample.stringee_wrapper.common.listener.OnTrackViewClick;

import org.webrtc.RendererCommon;

import java.util.List;

public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.TrackHolder>{
    private final Context context;
    private final List<StringeeVideoTrack> videoTracks;
    private final OnTrackViewClick onTrackViewClick;

    public TrackAdapter(Context context, List<StringeeVideoTrack> videoTracks, OnTrackViewClick onTrackViewClick) {
        this.context = context.getApplicationContext();
        this.videoTracks = videoTracks;
        this.onTrackViewClick = onTrackViewClick;
    }

    @NonNull
    @Override
    public TrackHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new TrackHolder(ListItemTrackBinding.inflate(LayoutInflater.from(context), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull TrackHolder holder, int position) {
        StringeeVideoTrack videoTrack = videoTracks.get(position);
        int rows;
        int cols;
        int size = videoTracks.size();

        if (size <= 3) {
            rows = size;
            cols = 1;
        } else {
            rows = (size + 1) / 2;
            cols = 2;
        }

        int screenHeight = context.getResources().getDisplayMetrics().heightPixels;
        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;

        int itemHeight = screenHeight / rows;
        int itemWidth = screenWidth / cols;

        holder.binding.getRoot().setLayoutParams(new FlexboxLayoutManager.LayoutParams(itemWidth, itemHeight));
        if (position == videoTracks.size() - 1) {
            FlexboxLayoutManager.LayoutParams layoutParams = (FlexboxLayoutManager.LayoutParams) holder.binding.getRoot().getLayoutParams();
            layoutParams.setFlexGrow(1.0f);
            holder.binding.getRoot().setLayoutParams(layoutParams);
        }

        holder.binding.tvUserId.setText(videoTrack.getUserId());
        Utils.runOnUiThread(() -> {
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.gravity = Gravity.CENTER;
            TextureViewRenderer view = videoTrack.getView2(context);
            if (view.getParent() != null) {
                ((FrameLayout) view.getParent()).removeView(view);
            }
            holder.binding.flTrack.addView(view, layoutParams);
            videoTrack.renderView2(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        });
        holder.binding.getRoot().setOnClickListener(v -> {
            if (onTrackViewClick != null) {
                onTrackViewClick.onClick();
            }
        });
        holder.binding.flTrack.setOnClickListener(v -> {
            if (onTrackViewClick != null) {
                onTrackViewClick.onClick();
            }
        });
    }

    @Override
    public int getItemCount() {
        return videoTracks.size();
    }

    public static class TrackHolder extends RecyclerView.ViewHolder {
        public ListItemTrackBinding binding;

        public TrackHolder(@NonNull ListItemTrackBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
