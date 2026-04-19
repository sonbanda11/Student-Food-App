package com.example.studentfood.presentation.ui.adapter.home;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studentfood.R;

import java.util.List;

public class HomeLocationAdapter extends RecyclerView.Adapter<HomeLocationAdapter.ViewHolder> {

    private List<String> locations;
    private OnLocationClickListener listener;

    public interface OnLocationClickListener {
        void onLocationClick(String location);
    }

    public HomeLocationAdapter(List<String> locations, OnLocationClickListener listener) {
        this.locations = locations;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_location, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        String location = locations.get(position);

        if (location == null) return;

        // 🔥 tách title + address
        String title;
        String address;

        if (location.contains(",")) {
            String[] parts = location.split(",", 2);
            title = parts[0].trim();
            address = parts[1].trim();
        } else {
            title = location;
            address = "";
        }

        // ✅ set dữ liệu
        holder.txtTitle.setText(title);
        holder.txtAddress.setText(address);

        // 🔥 highlight item đầu tiên
        if (position == 0) {
            holder.txtTitle.setTypeface(null, Typeface.BOLD);
        } else {
            holder.txtTitle.setTypeface(null, Typeface.NORMAL);
        }

        // 👉 click
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onLocationClick(location);
            }
        });
    }

    @Override
    public int getItemCount() {
        return locations != null ? locations.size() : 0;
    }

    // 🔥 update data (sau này dùng rất nhiều)
    public void updateData(List<String> newList) {
        this.locations = newList;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView txtTitle;
        TextView txtAddress;

        public ViewHolder(View itemView) {
            super(itemView);

            txtTitle = itemView.findViewById(R.id.txtTitle);
            txtAddress = itemView.findViewById(R.id.txtAddress);
        }
    }
}