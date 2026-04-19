package com.example.studentfood.presentation.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studentfood.R;
import com.example.studentfood.domain.model.Post;
import com.example.studentfood.presentation.ui.adapter.community.PostAdapter;
import com.example.studentfood.presentation.ui.component.RecyclerScrollComponent;
import com.example.studentfood.presentation.ui.fragment.CommentBottomSheet_Enhanced;
import com.example.studentfood.presentation.viewmodel.CommunityViewModel;

public class PostFeedFragment extends Fragment {

    private CommunityViewModel viewModel;
    private PostAdapter adapter;
    private RecyclerView rvPosts;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerScrollComponent scrollComponent;

    public PostFeedFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_post_feed, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireParentFragment())
            .get(CommunityViewModel.class);

        rvPosts = view.findViewById(R.id.rvPosts);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);

        rvPosts.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvPosts.setHasFixedSize(false);

        swipeRefreshLayout.setOnRefreshListener(() -> {
            viewModel.loadPosts();
        });

        adapter = new PostAdapter(requireContext(), new java.util.ArrayList<>());
        // Create post là item đầu tiên trong RecyclerView
        adapter.setOnCreatePostClickListener(() -> {
            CreatePostBottomSheet sheet = CreatePostBottomSheet.newInstance();
            sheet.setOnPostCreatedListener(post -> viewModel.addPost(post));
            sheet.show(getChildFragmentManager(), "create_post");
        });
        rvPosts.setAdapter(adapter);

        // Scroll component — gọi lên CommunityFragment
        scrollComponent = new RecyclerScrollComponent(rvPosts,
            new RecyclerScrollComponent.ScrollCallback() {
                @Override public void onScrollDown() { notifyParent(true); }
                @Override public void onScrollUp()   { notifyParent(false); }
                @Override public void onAtTop()      { notifyParent(false); }
            });
        scrollComponent.attach();

        adapter.setOnPostActionListener(new PostAdapter.OnPostActionListener() {
            @Override
            public void onLikeClick(Post post, int position) {
                viewModel.toggleLike(post);
            }
            @Override
            public void onCommentClick(Post post) {
                CommentBottomSheet_Enhanced.newInstance(post)
                    .show(getChildFragmentManager(), "comments");
            }
            @Override
            public void onShareClick(Post post) {
                viewModel.toggleShare(post);
                android.content.Intent shareIntent = new android.content.Intent(
                    android.content.Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
                    "Bài viết từ " + post.getUserName());
                shareIntent.putExtra(android.content.Intent.EXTRA_TEXT,
                    post.getUserName() + " chia sẻ:\n" + post.getContent()
                    + "\n\n— Từ app Student Food");
                startActivity(android.content.Intent.createChooser(shareIntent, "Chia sẻ qua..."));
            }
            @Override
            public void onPostClick(Post post) {}
        });

        viewModel.getPostsLiveData().observe(getViewLifecycleOwner(), posts -> {
            if (posts != null) adapter.setData(posts);
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private android.content.BroadcastReceiver dataImportReceiver = new android.content.BroadcastReceiver() {
        @Override
        public void onReceive(android.content.Context context, android.content.Intent intent) {
            if ("com.example.studentfood.DATA_IMPORTED".equals(intent.getAction())) {
                if (viewModel != null) {
                    viewModel.loadPosts();
                }
            }
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(requireContext())
                .registerReceiver(dataImportReceiver, new android.content.IntentFilter("com.example.studentfood.DATA_IMPORTED"));
    }

    @Override
    public void onStop() {
        super.onStop();
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(requireContext())
                .unregisterReceiver(dataImportReceiver);
    }

    private void notifyParent(boolean hide) {
        Fragment parent = getParentFragment();
        if (parent instanceof CommunityFragment) {
            if (hide) ((CommunityFragment) parent).hideHeaderAndNav();
            else      ((CommunityFragment) parent).showHeaderAndNav();
        }
    }

    public void scrollToTopAndRefresh() {
        if (rvPosts != null) rvPosts.scrollToPosition(0);
        if (scrollComponent != null) scrollComponent.reset();
        viewModel.loadPosts();
        // Hiện lại header khi về đầu
        notifyParent(false);
    }
}
