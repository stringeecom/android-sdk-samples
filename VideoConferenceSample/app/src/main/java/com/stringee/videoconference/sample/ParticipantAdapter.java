package com.stringee.videoconference.sample;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.stringee.videoconference.videoconference.sample.R;

import org.webrtc.RendererCommon;

import java.util.Iterator;
import java.util.List;

public class ParticipantAdapter extends RecyclerView.Adapter {
    private Context context;
    private Activity activity;
    private List<VideoTrack> videoTrackList;

    public ParticipantAdapter(Context context, Activity activity, List<VideoTrack> videoTrackList) {
        this.context = context;
        this.activity = activity;
        this.videoTrackList = videoTrackList;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.participant_row, parent, false);
        return new ParticipantHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ParticipantHolder participantHolder = (ParticipantHolder) holder;
        VideoTrack videoTrack = videoTrackList.get(position);

        if (videoTrack.getStringeeVideoTrack().getUserId() != null) {
            participantHolder.tvPaticipant.setText(videoTrack.getStringeeVideoTrack().getUserId());
        } else {
            participantHolder.tvPaticipant.setText(MainActivity.client.getUserId());
        }

        if (videoTrack.getLayout() != null) {
            participantHolder.ivStatus.setImageResource(R.drawable.ic_casting);
        } else {
            participantHolder.ivStatus.setImageResource(R.drawable.ic_cast);
        }

        participantHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (videoTrack.getLayout() == null) {
                    addView(videoTrack);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return videoTrackList.size();
    }

    public static class ParticipantHolder extends RecyclerView.ViewHolder {
        private TextView tvPaticipant;
        private ImageView ivStatus;

        public ParticipantHolder(@NonNull View itemView) {
            super(itemView);
            tvPaticipant = itemView.findViewById(R.id.tv_participant);
            ivStatus = itemView.findViewById(R.id.iv_status);
        }
    }

    private void addView(VideoTrack videoTrack) {
        VideoTrack trackInMainView = RoomActivity.mainTrack;

        RoomActivity.mainTrack = videoTrack;

        RoomActivity.mainView.removeAllViews();
        videoTrack.getStringeeVideoTrack().getView(context).setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT, RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        RoomActivity.mainView.addView(videoTrack.getStringeeVideoTrack().getView(context));
        videoTrack.getStringeeVideoTrack().renderView(false);
        videoTrack.setLayout(RoomActivity.mainView);

        for (Iterator<VideoTrack> videoTrackIterator = videoTrackList.iterator(); videoTrackIterator.hasNext(); ) {
            VideoTrack track = videoTrackIterator.next();
            if (track == trackInMainView) {
                track.setLayout(null);
            }
        }
        notifyDataSetChanged();
    }
}
