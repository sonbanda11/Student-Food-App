package com.example.studentfood.presentation.ui.adapter.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.studentfood.R;
import com.example.studentfood.domain.model.SearchHistory;
import java.util.List;

public class SearchHistoryAdapter extends RecyclerView.Adapter<SearchHistoryAdapter.ViewHolder> {

    private List<SearchHistory> list;
    private OnHistoryClickListener listener;

    public interface OnHistoryClickListener {
        void onItemClick(String query);
        void onDeleteClick(int id);
    }

    public SearchHistoryAdapter(List<SearchHistory> list, OnHistoryClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SearchHistory item = list.get(position);
        holder.txtQuery.setText(item.getQueryText());

        // Click vào cả cái tag để thực hiện tìm kiếm lại
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item.getQueryText());
        });

        // Nếu Sơn muốn có nút xóa riêng trong tag (như dấu X nhỏ)
        if (holder.btnDelete != null) {
            holder.btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    // Ép kiểu ID cẩn thận vì searchId thường là String trong Model của Sơn
                    try {
                        int id = Integer.parseInt(item.getSearchId());
                        listener.onDeleteClick(id);
                    } catch (NumberFormatException e) {
                        // Xử lý nếu ID không phải số
                    }
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    public void updateData(List<SearchHistory> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtQuery;
        ImageView btnDelete; // Nút X để xóa từng mục

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // SỬA Ở ĐÂY: Phải khớp với ID trong file item_search_history.xml
            txtQuery = itemView.findViewById(R.id.tvHistoryText);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}