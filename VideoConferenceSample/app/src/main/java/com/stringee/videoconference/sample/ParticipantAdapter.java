package com.stringee.videoconference.sample;

import static com.stringee.videoconference.sample.ParticipantAdapter.ParticipantHolder;

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

import java.util.List;

public class ParticipantAdapter extends RecyclerView.Adapter<ParticipantHolder> {
    private final Context context;
    private List<VideoTrack> videoTrackList;

    public ParticipantAdapter(Context context, List<VideoTrack> videoTrackList) {
        this.context = context.getApplicationContext();
        this.videoTrackList = videoTrackList;
    }

    @NonNull
    @Override
    public ParticipantHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.participant_row, parent, false);
        return new ParticipantHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ParticipantHolder holder, int position) {
        VideoTrack videoTrack = videoTrackList.get(position);

        if (videoTrack.getStringeeVideoTrack().getUserId() != null) {
            holder.tvParticipant.setText(videoTrack.getStringeeVideoTrack().getUserId());
        } else {
            holder.tvParticipant.setText(MainActivity.client.getUserId());
        }

        if (videoTrack.getLayout() != null) {
            holder.ivStatus.setImageResource(R.drawable.ic_casting);
        } else {
            holder.ivStatus.setImageResource(R.drawable.ic_cast);
        }

        holder.itemView.setOnClickListener(view -> {
            if (videoTrack.getLayout() == null) {
                addView(videoTrack);
            }
        });
    }

    @Override
    public int getItemCount() {
        return videoTrackList.size();
    }

    public static class ParticipantHolder extends RecyclerView.ViewHolder {
        private final TextView tvParticipant;
        private final ImageView ivStatus;

        public ParticipantHolder(@NonNull View itemView) {
            super(itemView);
            tvParticipant = itemView.findViewById(R.id.tv_participant);
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

        for (VideoTrack track : videoTrackList) {
            if (track == trackInMainView) {
                track.setLayout(null);
            }
        }
        notifyDataSetChanged();
    }
}
