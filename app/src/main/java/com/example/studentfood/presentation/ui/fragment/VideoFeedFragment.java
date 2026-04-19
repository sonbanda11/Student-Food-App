package com.example.studentfood.presentation.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.studentfood.R;
import com.example.studentfood.domain.model.Post;
import com.example.studentfood.presentation.ui.adapter.community.VideoFeedAdapter;
import com.example.studentfood.presentation.viewmodel.CommunityViewModel;

public class VideoFeedFragment extends Fragment {

    private CommunityViewModel viewModel;
    private VideoFeedAdapter adapter;
    private ViewPager2 vpVideos;

    public VideoFeedFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_video_feed, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireParentFragment())
            .get(CommunityViewModel.class);

        vpVideos = view.findViewById(R.id.vpVideos);
        vpVideos.setOffscreenPageLimit(1);

        // Tắt nested scroll để vuốt mượt
        RecyclerView rv = (RecyclerView) vpVideos.getChildAt(0);
        if (rv != null) rv.setOverScrollMode(View.OVER_SCROLL_NEVER);

        adapter = new VideoFeedAdapter(requireContext(), new java.util.ArrayList<>());
        vpVideos.setAdapter(adapter);

        adapter.setOnVideoActionListener(new VideoFeedAdapter.OnVideoActionListener() {
            @Override
            public void onLikeClick(Post post, int position) {
                // Already handled in adapter
            }
            @Override
            public void onCommentClick(Post post) {
                Toast.makeText(requireContext(), "Bình luận video", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onShareClick(Post post) {
                Toast.makeText(requireContext(), "Đã chia sẻ!", Toast.LENGTH_SHORT).show();
            }
        });

        // Pause video khi chuyển trang
        vpVideos.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                // Stop previous video — handled by onViewRecycled in adapter
            }
        });

        viewModel.getVideosLiveData().observe(getViewLifecycleOwner(), videos -> {
            if (videos != null) adapter.setData(videos);
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        // Pause video khi rời fragment
        if (vpVideos != null) {
            RecyclerView rv = (RecyclerView) vpVideos.getChildAt(0);
            if (rv != null) {
                VideoFeedAdapter.VideoViewHolder vh =
                    (VideoFeedAdapter.VideoViewHolder) rv.findViewHolderForAdapterPosition(vpVideos.getCurrentItem());
                if (vh != null && vh.videoView.isPlaying()) vh.videoView.pause();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (vpVideos != null) {
            RecyclerView rv = (RecyclerView) vpVideos.getChildAt(0);
            if (rv != null) {
                VideoFeedAdapter.VideoViewHolder vh =
                    (VideoFeedAdapter.VideoViewHolder) rv.findViewHolderForAdapterPosition(vpVideos.getCurrentItem());
                if (vh != null) vh.videoView.start();
            }
        }
    }
}
