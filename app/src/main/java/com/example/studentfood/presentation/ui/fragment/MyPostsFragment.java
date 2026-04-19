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
import com.example.studentfood.data.local.manager.UserManager;
import com.example.studentfood.domain.model.Post;
import com.example.studentfood.presentation.ui.adapter.community.PostAdapter;
import com.example.studentfood.presentation.viewmodel.CommunityViewModel;

import java.util.ArrayList;
import java.util.List;

public class MyPostsFragment extends Fragment {

    public MyPostsFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_post_feed, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        CommunityViewModel vm = new ViewModelProvider(requireActivity())
            .get(CommunityViewModel.class);

        RecyclerView rv = view.findViewById(R.id.rvPosts);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        PostAdapter adapter = new PostAdapter(requireContext(), new ArrayList<>());
        rv.setAdapter(adapter);

        String myUserId = UserManager.getUser(requireContext()) != null
            ? UserManager.getUser(requireContext()).getUserId() : "";

        vm.getPostsLiveData().observe(getViewLifecycleOwner(), posts -> {
            if (posts == null) return;
            List<Post> mine = new ArrayList<>();
            for (Post p : posts) {
                if (myUserId.equals(p.getUserId())) mine.add(p);
            }
            adapter.setData(mine);
        });
    }
}
