package com.stringee.video_conference_sample.stringee_wrapper.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.flexbox.AlignItems;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.JustifyContent;
import com.stringee.video.StringeeVideoTrack;
import com.stringee.video_conference_sample.databinding.FragmentTrackListBinding;
import com.stringee.video_conference_sample.stringee_wrapper.common.listener.OnPageNeedData;
import com.stringee.video_conference_sample.stringee_wrapper.adapter.TrackAdapter;
import com.stringee.video_conference_sample.stringee_wrapper.common.listener.OnTrackViewClick;

import java.util.ArrayList;
import java.util.List;

public class TrackListFragment extends Fragment implements OnTrackViewClick {
    private FragmentTrackListBinding binding;
    private int pageNumber = 0;
    private OnPageNeedData onPageNeedData;
    private OnTrackViewClick onTrackViewClick;

    public TrackListFragment setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
        return this;
    }

    public TrackListFragment setOnPageNeedData(OnPageNeedData onPageNeedData) {
        this.onPageNeedData = onPageNeedData;
        return this;
    }

    public TrackListFragment setOnTrackViewClick(OnTrackViewClick onTrackViewClick) {
        this.onTrackViewClick = onTrackViewClick;
        return this;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (onPageNeedData != null) {
            onPageNeedData.onPage(pageNumber);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        initAdapter(new ArrayList<>());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentTrackListBinding.inflate(inflater, container, false);

        FlexboxLayoutManager layoutManager = new FlexboxLayoutManager(requireContext());
        layoutManager.setFlexDirection(FlexDirection.ROW);
        layoutManager.setJustifyContent(JustifyContent.SPACE_AROUND);
        layoutManager.setFlexWrap(FlexWrap.WRAP);
        layoutManager.setAlignItems(AlignItems.STRETCH);
        binding.rvTrack.setLayoutManager(layoutManager);
        binding.getRoot().setOnClickListener(v -> {
            if (onTrackViewClick != null) {
                onTrackViewClick.onClick();
            }
        });
        return binding.getRoot();
    }

    public void initAdapter(List<StringeeVideoTrack> videoTracks) {
        TrackAdapter adapter = new TrackAdapter(requireContext(), videoTracks, this);
        binding.rvTrack.setAdapter(adapter);
    }

    @Override
    public void onClick() {
        if (onTrackViewClick != null) {
            onTrackViewClick.onClick();
        }
    }
}