package com.example.studentfood.presentation.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studentfood.R;
import com.example.studentfood.domain.model.CommunityGroup;
import com.example.studentfood.presentation.ui.adapter.community.CommunityGroupAdapter;
import com.example.studentfood.presentation.ui.component.RecyclerScrollComponent;

import java.util.ArrayList;
import java.util.List;

public class GroupFeedFragment extends Fragment {

    private CommunityGroupAdapter adapter;
    private RecyclerScrollComponent scrollComponent;

    public GroupFeedFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_group_feed, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView rvGroups = view.findViewById(R.id.rvGroups);
        rvGroups.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new CommunityGroupAdapter(requireContext(), generateSampleGroups());
        rvGroups.setAdapter(adapter);

        // Scroll component — gọi lên CommunityFragment
        scrollComponent = new RecyclerScrollComponent(rvGroups,
            new RecyclerScrollComponent.ScrollCallback() {
                @Override public void onScrollDown() { notifyParent(true); }
                @Override public void onScrollUp()   { notifyParent(false); }
                @Override public void onAtTop()      { notifyParent(false); }
            });
        scrollComponent.attach();
    }

    private void notifyParent(boolean hide) {
        Fragment parent = getParentFragment();
        if (parent instanceof CommunityFragment) {
            if (hide) ((CommunityFragment) parent).hideHeaderAndNav();
            else      ((CommunityFragment) parent).showHeaderAndNav();
        }
    }

    private List<CommunityGroup> generateSampleGroups() {
        String img = "https://cdn-media.sforum.vn/storage/app/media/ctvseo_maihue/hinh-nen-do-an-cute/hinh-nen-do-an-cute-1.jpg";
        List<CommunityGroup> list = new ArrayList<>();
        list.add(new CommunityGroup("GRP_01", "Hội mê cơm tấm Hà Nội", "Chia sẻ địa điểm cơm tấm ngon, giá sinh viên", img, 2400, 38, "Cơm"));
        list.add(new CommunityGroup("GRP_02", "Trà sữa Bách Khoa", "Review trà sữa khu vực Bách Khoa - Hai Bà Trưng", img, 1850, 52, "Trà sữa"));
        list.add(new CommunityGroup("GRP_03", "Phở & Bún ngon Hà Nội", "Tổng hợp quán phở bún chuẩn vị Hà Nội", img, 3200, 45, "Phở & Bún"));
        list.add(new CommunityGroup("GRP_04", "Cà phê sách sinh viên", "Góc chill học bài, cà phê giá rẻ", img, 980, 20, "Cà phê"));
        list.add(new CommunityGroup("GRP_05", "Bánh mì Hà Nội", "Bánh mì que, bánh mì pate, bánh mì thập cẩm", img, 1560, 33, "Bánh mì"));
        list.add(new CommunityGroup("GRP_06", "Ăn vặt cổng trường", "Nem chua rán, bánh tráng trộn, xiên que", img, 4100, 78, "Ăn vặt"));
        list.add(new CommunityGroup("GRP_07", "Lẩu sinh viên", "Lẩu ly, lẩu thái, lẩu ếch giá rẻ", img, 720, 15, "Lẩu"));
        list.add(new CommunityGroup("GRP_08", "Đồ ngọt & Chè", "Chè bưởi, sữa chua, tào phớ, kem", img, 1340, 29, "Đồ ngọt"));
        list.add(new CommunityGroup("GRP_09", "Bún đậu mắm tôm", "Hội ghiền bún đậu khu vực Hà Nội", img, 2100, 41, "Phở & Bún"));
        list.add(new CommunityGroup("GRP_10", "Review quán ăn đêm", "Ăn đêm khuya, hủ tiếu, ốc, bánh mì", img, 890, 22, "Ăn vặt"));
        return list;
    }
}
