package com.example.studentfood.presentation.ui.adapter.community;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.studentfood.R;
import com.example.studentfood.domain.model.CommunityGroup;

import java.util.List;

public class CommunityGroupAdapter extends RecyclerView.Adapter<CommunityGroupAdapter.VH> {

    private final Context context;
    private List<CommunityGroup> list;

    public CommunityGroupAdapter(Context context, List<CommunityGroup> list) {
        this.context = context;
        this.list = list;
    }

    public void setData(List<CommunityGroup> data) {
        this.list = data;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_food_group, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        CommunityGroup g = list.get(position);

        h.txtGroupName.setText(g.getGroupName());
        h.txtMemberCount.setText(g.getFormattedMembers());
        h.txtPostCount.setText(g.getPostCount() + " bài/tuần");

        Glide.with(context)
            .load(g.getCoverImageUrl())
            .placeholder(R.drawable.ic_rice_bowl)
            .centerCrop()
            .into(h.imgCover);

        // Trạng thái nút tham gia
        updateJoinButton(h, g);

        h.btnJoin.setOnClickListener(v -> {
            g.setJoined(!g.isJoined());
            updateJoinButton(h, g);
        });
    }

    private void updateJoinButton(VH h, CommunityGroup g) {
        if (g.isJoined()) {
            h.btnJoin.setText("Đã tham gia");
            h.btnJoin.setTextColor(context.getResources().getColor(android.R.color.white));
            h.btnJoin.setBackgroundResource(R.drawable.bg_btn_shadow);
        } else {
            h.btnJoin.setText("Tham gia");
            h.btnJoin.setTextColor(context.getResources().getColor(R.color.light_blue_600));
            h.btnJoin.setBackgroundResource(R.drawable.bg_tag_category);
        }
    }

    @Override
    public int getItemCount() { return list == null ? 0 : list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView imgCover;
        TextView txtGroupName, txtMemberCount, txtPostCount, btnJoin;

        VH(@NonNull View v) {
            super(v);
            imgCover       = v.findViewById(R.id.imgGroupCover);
            txtGroupName   = v.findViewById(R.id.txtGroupName);
            txtMemberCount = v.findViewById(R.id.txtMemberCount);
            txtPostCount   = v.findViewById(R.id.txtPostCount);
            btnJoin        = v.findViewById(R.id.btnJoin);
        }
    }
}
