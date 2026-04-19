package com.example.studentfood.presentation.ui.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.studentfood.R;
import com.example.studentfood.data.local.manager.UserManager;
import com.example.studentfood.data.local.repository.CommentRepository;
import com.example.studentfood.domain.model.Comment;
import com.example.studentfood.domain.model.Post;
import com.example.studentfood.domain.model.User;
import com.example.studentfood.presentation.viewmodel.CommentViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * CommentBottomSheet_Enhanced - BottomSheet hiên thî bình luân nâng cao
 * Hô trî threaded comments (replies) và indentation
 */
public class CommentBottomSheet_Enhanced extends BottomSheetDialogFragment {

    private static final String ARG_POST_ID = "post_id";
    private static final String ARG_POST_USER = "post_user";
    private static final String ARG_COMMENT_COUNT = "comment_count";

    private RecyclerView rvComments;
    private EditText edtComment;
    private ImageView btnSend;
    private ImageView imgAvatar;

    private CommentViewModel commentViewModel;
    private CommentRepository commentRepository;
    private List<Comment> comments = new ArrayList<>();
    private CommentAdapter adapter;
    private String currentPostId;
    private Comment replyingToComment = null;

    public static CommentBottomSheet_Enhanced newInstance(Post post) {
        CommentBottomSheet_Enhanced sheet = new CommentBottomSheet_Enhanced();
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

        bindViews(view);
        setupViewModel();
        setupUI();
        loadComments();
        setupSendComment();
    }

    private void bindViews(View view) {
        rvComments = view.findViewById(R.id.rvComments);
        edtComment = view.findViewById(R.id.edtComment);
        btnSend = view.findViewById(R.id.btnSendComment);
        imgAvatar = view.findViewById(R.id.imgCommentAvatar);
    }

    private void setupViewModel() {
        commentViewModel = new ViewModelProvider(requireActivity()).get(CommentViewModel.class);
        commentRepository = CommentRepository.getInstance();
        
        // Lây post ID t arguments
        currentPostId = getArguments() != null ? getArguments().getString(ARG_POST_ID, "") : "";
    }

    private void setupUI() {
        // Tài avatar ngûôi dùng
        User user = UserManager.getUser(requireContext());
        if (user != null && imgAvatar != null) {
            Glide.with(this)
                .load(user.getAvatarUrl())
                .placeholder(R.drawable.ic_person)
                .circleCrop()
                .into(imgAvatar);
        }

        // Cài dé RecyclerView
        adapter = new CommentAdapter(comments);
        adapter.setCallback(new CommentAdapter.CommentCallback() {
            @Override
            public void onReplyClick(Comment comment) {
                replyingToComment = comment;
                edtComment.setHint("Phân hôi " + comment.getUserName());
                edtComment.requestFocus();
                // Show keyboard
                edtComment.post(() -> {
                    android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) 
                        requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(edtComment, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                });
            }
        });
        rvComments.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvComments.setAdapter(adapter);
    }

    private void loadComments() {
        Log.d("CommentBottomSheet", "Loading comments for post: " + currentPostId);
        
        // Tài bình luân sû dng CommentRepository
        commentViewModel.loadCommentsByTarget(currentPostId, Comment.TargetType.POST);
        
        // Quan sát LiveData bình luân
        commentViewModel.getCommentsLiveData().observe(getViewLifecycleOwner(), commentsList -> {
            Log.d("CommentBottomSheet", "Received comments: " + (commentsList != null ? commentsList.size() : 0));
            if (commentsList != null) {
                comments.clear();
                // Sáp xêp comments theo thread structure
                List<Comment> threadedComments = organizeThreadedComments(commentsList);
                comments.addAll(threadedComments);
                Log.d("CommentBottomSheet", "Threaded comments size: " + threadedComments.size());
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                    Log.d("CommentBottomSheet", "Adapter notified with " + adapter.getItemCount() + " items");
                }
            }
        });
    }

    private void setupSendComment() {
        btnSend.setOnClickListener(v -> {
            String text = edtComment.getText().toString().trim();
            if (text.isEmpty()) return;

            User user = UserManager.getUser(requireContext());
            String name = user != null ? user.getFullName() : "Ban";
            String avatar = user != null ? user.getAvatarUrl() : "";
            
            // Tao bình luân mui
            Comment newComment = new Comment();
            newComment.setCommentId("CMT_" + System.currentTimeMillis());
            newComment.setTargetId(currentPostId);
            newComment.setTargetType(Comment.TargetType.POST);
            newComment.setUserId(user != null ? user.getUserId() : "");
            newComment.setUserName(name);
            newComment.setUserAvatar(avatar);
            newComment.setContent(text);
            newComment.setTimestamp(System.currentTimeMillis());
            
            // Xû lî reply
            if (replyingToComment != null) {
                newComment.setParentCommentId(replyingToComment.getCommentId());
                newComment.setReplyToUserId(replyingToComment.getUserId());
            }
            
            // Thêm bình luân sû dng ViewModel
            commentViewModel.addComment(newComment);
            
            // Reset reply state
            replyingToComment = null;
            edtComment.setText("");
            edtComment.setHint("Viêt bình luân...");
            
            Toast.makeText(requireContext(), replyingToComment != null ? "Dã phân hôi!" : "Dã bình luân!", Toast.LENGTH_SHORT).show();
        });
    }
    
    /**
     * Tô chîc comments theo cô trúc thread (parent comments trûc, replies sau)
     */
    private List<Comment> organizeThreadedComments(List<Comment> flatComments) {
        List<Comment> result = new ArrayList<>();
        List<Comment> topLevelComments = new ArrayList<>();
        
        // Phân loai top-level và replies
        for (Comment comment : flatComments) {
            if (comment.getParentCommentId() == null || comment.getParentCommentId().isEmpty()) {
                topLevelComments.add(comment);
            }
        }
        
        // Sáp xêp top-level comments theo timestamp
        topLevelComments.sort((c1, c2) -> Long.compare(c2.getTimestamp(), c1.getTimestamp()));
        
        // Thêm top-level comments và replies cùa chúng
        for (Comment parent : topLevelComments) {
            result.add(parent);
            
            // Thêm replies cùa comment này
            for (Comment comment : flatComments) {
                if (parent.getCommentId().equals(comment.getParentCommentId())) {
                    result.add(comment);
                }
            }
        }
        
        return result;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    // ===================== ADAPTER ======================================
    static class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.VH> {
        private final List<Comment> list;
        
        CommentAdapter(List<Comment> list) { 
            this.list = list; 
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment_threaded, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Comment comment = list.get(position);
            
            // Set indentation for replies
            int indentLevel = getIndentLevel(comment);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) h.indentationSpace.getLayoutParams();
            params.width = indentLevel * 48; // 48dp per level
            h.indentationSpace.setLayoutParams(params);
            
            // Set user name and content
            h.txtUserName.setText(comment.getUserName() != null ? comment.getUserName() : "Nôi dung");
            
            // Handle reply info
            if (comment.getReplyToUserName() != null && !comment.getReplyToUserName().isEmpty()) {
                h.txtReplyInfo.setText("Trâ lôi " + comment.getReplyToUserName());
                h.txtReplyInfo.setVisibility(View.VISIBLE);
                
                // Create bold reply text
                android.text.SpannableString replyText = new android.text.SpannableString(comment.getReplyToUserName() + " " + comment.getContent());
                replyText.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, comment.getReplyToUserName().length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                h.txtContent.setText(replyText);
            } else {
                h.txtReplyInfo.setVisibility(View.GONE);
                h.txtContent.setText(comment.getContent());
            }
            
            h.txtTime.setText(formatTime(comment.getTimestamp()));
            
            // Cài dé vân bán và trâng thái nút thích
            String likeText = comment.isLiked() ? "Dã thích" : "Thích";
            if (comment.getLikeCount() > 0) {
                likeText += " (" + comment.getLikeCount() + ")";
            }
            h.btnLike.setText(likeText);
            h.btnLike.setTextColor(comment.isLiked() ? 
                h.btnLike.getContext().getResources().getColor(android.R.color.holo_blue_dark) : 
                h.btnLike.getContext().getResources().getColor(android.R.color.darker_gray));
            
            // Tài avatar nêu có
            if (comment.getUserAvatar() != null && !comment.getUserAvatar().isEmpty()) {
                Glide.with(h.imgAvatar.getContext())
                    .load(comment.getUserAvatar())
                    .placeholder(R.drawable.ic_person)
                    .circleCrop()
                    .into(h.imgAvatar);
            }
            
            // Xû lî click nút thích
            h.btnLike.setOnClickListener(v -> {
                // Bình thuwòng sôi dùng callback d thông báo cho fragment
                // Tâm thôi chî câp nhât UI
                comment.setLiked(!comment.isLiked());
                comment.setLikeCount(comment.isLiked() ? 
                    comment.getLikeCount() + 1 : 
                    Math.max(0, comment.getLikeCount() - 1));
                notifyItemChanged(position);
            });
            
            // Xû lî click nût trâ lôi
            h.btnReply.setOnClickListener(v -> {
                // Set reply mode
                if (callback != null) {
                    callback.onReplyClick(comment);
                }
            });
        }

        @Override
        public int getItemCount() { 
            return list.size(); 
        }

        private static String formatTime(long timestamp) {
            long now = System.currentTimeMillis();
            long diff = now - timestamp;
            
            if (diff < 60000) return "Vùa xong";
            if (diff < 3600000) return (diff / 60000) + " phút trûc";
            if (diff < 86400000) return (diff / 3600000) + " gi trûc";
            return (diff / 86400000) + " ngày trûc";
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView txtUserName, txtContent, txtTime, btnLike, btnReply, txtReplyInfo;
            ImageView imgAvatar, imgComment;
            View indentationSpace;
            
            VH(View v) {
                super(v);
                txtUserName = v.findViewById(R.id.txtUserName);
                txtContent = v.findViewById(R.id.txtContent);
                txtTime = v.findViewById(R.id.txtTime);
                btnLike = v.findViewById(R.id.btnLike);
                btnReply = v.findViewById(R.id.btnReply);
                txtReplyInfo = v.findViewById(R.id.txtReplyInfo);
                imgAvatar = v.findViewById(R.id.imgUserAvatar);
                imgComment = v.findViewById(R.id.imgComment);
                indentationSpace = v.findViewById(R.id.indentationSpace);
            }
        }
        
        // Helper method to get indentation level
        private int getIndentLevel(Comment comment) {
            if (comment.getParentCommentId() == null || comment.getParentCommentId().isEmpty()) {
                return 0; // Top-level comment
            }
            return 1; // Reply (can be extended for deeper nesting)
        }
        
        // Callback interface
        public interface CommentCallback {
            void onReplyClick(Comment comment);
        }
        
        private CommentCallback callback;
        
        public void setCallback(CommentCallback callback) {
            this.callback = callback;
        }
    }
}
