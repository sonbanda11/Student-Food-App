package com.example.studentfood.presentation.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.studentfood.R;
import com.example.studentfood.data.local.manager.UserManager;
import com.example.studentfood.domain.model.Post;
import com.example.studentfood.domain.model.User;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

public class CommentBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_POST_ID   = "post_id";
    private static final String ARG_POST_USER = "post_user";
    private static final String ARG_COMMENT_COUNT = "comment_count";

    public static CommentBottomSheet newInstance(Post post) {
        CommentBottomSheet sheet = new CommentBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_POST_ID, post.getPostId());
        args.putString(ARG_POST_USER, post.getUserName());
        args.putInt(ARG_COMMENT_COUNT, post.getCommentCount());
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_comments, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String postUser = getArguments() != null ? getArguments().getString(ARG_POST_USER, "") : "";
        int commentCount = getArguments() != null ? getArguments().getInt(ARG_COMMENT_COUNT, 0) : 0;

        RecyclerView rvComments = view.findViewById(R.id.rvComments);
        EditText edtComment     = view.findViewById(R.id.edtComment);
        ImageView btnSend       = view.findViewById(R.id.btnSendComment);
        ImageView imgAvatar     = view.findViewById(R.id.imgCommentAvatar);

        // Load user avatar
        User user = UserManager.getUser(requireContext());
        if (user != null && user.getAvatar() != null) {
            Glide.with(this).load(user.getAvatarUrl())
                .placeholder(R.drawable.ic_person).circleCrop().into(imgAvatar);
        }

        // Tạo dữ liệu comment mẫu
        List<CommentItem> comments = generateSampleComments(postUser, commentCount);

        CommentAdapter adapter = new CommentAdapter(comments);
        rvComments.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvComments.setAdapter(adapter);

        // Gửi comment
        btnSend.setOnClickListener(v -> {
            String text = edtComment.getText().toString().trim();
            if (text.isEmpty()) return;

            String name = user != null ? user.getFullName() : "Bạn";
            String avatar = user != null ? user.getAvatarUrl() : "";
            comments.add(0, new CommentItem(name, avatar, text, "Vừa xong"));
            adapter.notifyItemInserted(0);
            rvComments.scrollToPosition(0);
            edtComment.setText("");
            Toast.makeText(requireContext(), "Đã bình luận!", Toast.LENGTH_SHORT).show();
        });
    }

    private List<CommentItem> generateSampleComments(String postUser, int count) {
        List<CommentItem> list = new ArrayList<>();
        String avatar = "https://cdn-media.sforum.vn/storage/app/media/ctvseo_maihue/hinh-nen-do-an-cute/hinh-nen-do-an-cute-1.jpg";
        String[] names = {"Nguyễn Viết Sơn", "Hằng Nga", "Tuấn Anh", "Linh Chi", "Minh Quang"};
        String[] texts = {
            "Trông ngon quá, cho mình địa chỉ với!",
            "Mình cũng hay ăn ở đây, đúng là ngon thật 😍",
            "Giá cả thế nào bạn ơi?",
            "Hôm nào rủ mình đi cùng nhé!",
            "Ảnh chụp đẹp quá, filter gì vậy?"
        };
        String[] times = {"5 phút trước", "12 phút trước", "1 giờ trước", "2 giờ trước", "3 giờ trước"};

        int show = Math.min(count, names.length);
        for (int i = 0; i < show; i++) {
            list.add(new CommentItem(names[i], avatar, texts[i % texts.length], times[i]));
        }
        return list;
    }

    // ===================== DATA CLASS =====================
    static class CommentItem {
        String userName, userAvatar, content, time;
        CommentItem(String n, String a, String c, String t) {
            userName = n; userAvatar = a; content = c; time = t;
        }
    }

    // ===================== ADAPTER =====================
    static class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.VH> {
        private final List<CommentItem> list;
        CommentAdapter(List<CommentItem> list) { this.list = list; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            CommentItem item = list.get(position);
            h.txtUserName.setText(item.userName);
            h.txtContent.setText(item.content);
            h.txtTime.setText(item.time);
            Glide.with(h.itemView.getContext())
                .load(item.userAvatar)
                .placeholder(R.drawable.ic_person)
                .circleCrop()
                .into(h.imgAvatar);
        }

        @Override public int getItemCount() { return list.size(); }

        static class VH extends RecyclerView.ViewHolder {
            ImageView imgAvatar;
            TextView txtUserName, txtContent, txtTime;
            VH(@NonNull View v) {
                super(v);
                imgAvatar   = v.findViewById(R.id.imgAvatar);
                txtUserName = v.findViewById(R.id.txtUserName);
                txtContent  = v.findViewById(R.id.txtContent);
                txtTime     = v.findViewById(R.id.txtTime);
            }
        }
    }
}
