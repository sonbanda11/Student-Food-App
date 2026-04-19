package com.example.studentfood.presentation.ui.adapter.community;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.studentfood.R;
import com.example.studentfood.domain.model.Post;
import com.example.studentfood.utils.TimeUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for Community Posts with Multi-Image Layout
 */
public class PostAdapter extends RecyclerView.Adapter<PostAdapter.ViewHolder> {

    private static final int TYPE_CREATE_POST = 0;
    private static final int TYPE_POST = 1;

    private final Context context;
    private List<Post> list;
    private OnCreatePostClickListener onCreatePostClickListener;
    private OnPostActionListener listener;

    public interface OnCreatePostClickListener {
        void onClick();
    }

    public interface OnPostActionListener {
        void onLikeClick(Post post, int position);
        void onCommentClick(Post post);
        void onShareClick(Post post);
        void onPostClick(Post post);
    }

    public PostAdapter(Context context, List<Post> list) {
        this.context = context;
        this.list = list != null ? list : new ArrayList<>();
    }

    public void setOnCreatePostClickListener(OnCreatePostClickListener onCreatePostClickListener) {
        this.onCreatePostClickListener = onCreatePostClickListener;
    }

    public void setOnPostActionListener(OnPostActionListener listener) {
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? TYPE_CREATE_POST : TYPE_POST;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_CREATE_POST) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_create_post, parent, false);
            return new CreatePostViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false);
            return new PostViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE_CREATE_POST) {
            ((CreatePostViewHolder) holder).bind();
        } else {
            Post post = list.get(position - 1);
            ((PostViewHolder) holder).bind(post);
        }
    }

    @Override
    public int getItemCount() {
        return list.size() + 1; // +1 for CreatePost header
    }

    public void setData(List<Post> newList) {
        this.list = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    // Base ViewHolder to keep structure simple
    public abstract static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    // Header ViewHolder
    public class CreatePostViewHolder extends ViewHolder {
        private final LinearLayout layoutCreatePost;

        public CreatePostViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutCreatePost = itemView.findViewById(R.id.layoutCreatePost);
        }

        public void bind() {
            layoutCreatePost.setOnClickListener(v -> {
                if (onCreatePostClickListener != null) onCreatePostClickListener.onClick();
            });
        }
    }

    // Main Post ViewHolder
    public class PostViewHolder extends ViewHolder {
        ImageView imgAvatar, imgLike, imgLocation, imgComment;
        TextView txtUser, txtTime, txtLocation, txtContent, txtSeeMore;
        TextView txtLikeCount, txtCommentCountStats, txtShareCountStats;
        TextView txtLike, txtCommentCount, txtShareCount;
        LinearLayout btnLike, btnComment, btnShare, layoutImages;
        RecyclerView recyclerImages;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            imgLike = itemView.findViewById(R.id.imgLike);
            imgLocation = itemView.findViewById(R.id.imgLocation);
            imgComment = itemView.findViewById(R.id.imgComment);
            txtUser = itemView.findViewById(R.id.txtUser);
            txtTime = itemView.findViewById(R.id.txtTime);
            txtLocation = itemView.findViewById(R.id.txtLocation);
            txtContent = itemView.findViewById(R.id.txtContent);
            txtSeeMore = itemView.findViewById(R.id.txtSeeMore);
            txtLikeCount = itemView.findViewById(R.id.txtLikeCount);
            txtCommentCountStats = itemView.findViewById(R.id.txtCommentCountStats);
            txtShareCountStats = itemView.findViewById(R.id.txtShareCountStats);
            txtLike = itemView.findViewById(R.id.txtLike);
            txtCommentCount = itemView.findViewById(R.id.txtCommentCount);
            txtShareCount = itemView.findViewById(R.id.txtShareCount);
            btnLike = itemView.findViewById(R.id.btnLike);
            btnComment = itemView.findViewById(R.id.btnComment);
            btnShare = itemView.findViewById(R.id.btnShare);
            recyclerImages = itemView.findViewById(R.id.recyclerImages);
            layoutImages = itemView.findViewById(R.id.layoutImages);
        }

        void bind(Post post) {
            txtTime.setText(TimeUtils.getTimeAgo(post.getTime()));
            txtContent.setText(post.getContent());
            txtUser.setText(post.getUserName() != null ? post.getUserName() : "User");

            // Location
            if (post.getLocation() != null && !post.getLocation().isEmpty()) {
                txtLocation.setText(post.getLocation());
                txtLocation.setVisibility(View.VISIBLE);
                imgLocation.setVisibility(View.VISIBLE);
            } else {
                txtLocation.setVisibility(View.GONE);
                imgLocation.setVisibility(View.GONE);
            }

            // See more
            txtContent.post(() -> {
                if (txtContent.getLineCount() > 4) {
                    txtSeeMore.setVisibility(View.VISIBLE);
                } else {
                    txtSeeMore.setVisibility(View.GONE);
                }
            });
            txtSeeMore.setOnClickListener(v -> {
                if (txtContent.getMaxLines() == 4) {
                    txtContent.setMaxLines(Integer.MAX_VALUE);
                    txtSeeMore.setText("Thu gọn");
                } else {
                    txtContent.setMaxLines(4);
                    txtSeeMore.setText("Xem thêm");
                }
            });

            // Stats
            txtLikeCount.setText(formatCount(post.getLikeCount()) + " lượt thích");
            
            if (txtCommentCountStats != null) {
                txtCommentCountStats.setText(formatCount(post.getCommentCount()) + " bình luận");
                txtCommentCountStats.setOnClickListener(v -> {
                    if (listener != null) listener.onCommentClick(post);
                });
            }
            
            if (txtShareCountStats != null) {
                txtShareCountStats.setText(formatCount(post.getShareCount()) + " chia sẻ");
                txtShareCountStats.setOnClickListener(v -> {
                    if (listener != null) listener.onShareClick(post);
                });
            }

            // Like button state
            updateLikeUI(this, post);

            // Action buttons text
            txtCommentCount.setText("Bình luận");
            txtShareCount.setText("Chia sẻ");

            // Avatar
            Glide.with(context)
                .load(post.getUserAvatar())
                .placeholder(R.drawable.ic_person)
                .circleCrop()
                .into(imgAvatar);

            // Images Logic
            List<String> urls = post.getImageUrls();
            if (post.hasImages() && !urls.isEmpty()) {
                recyclerImages.setVisibility(View.GONE);
                layoutImages.setVisibility(View.VISIBLE);
                layoutImages.removeAllViews();

                int count = urls.size();
                if (count == 1) {
                    ImageView img = makeImageView(layoutImages);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(context, 240));
                    img.setLayoutParams(lp);
                    Glide.with(context).load(urls.get(0)).centerCrop().into(img);
                    layoutImages.addView(img);
                } else if (count == 2) {
                    layoutImages.setOrientation(LinearLayout.HORIZONTAL);
                    for (int i = 0; i < 2; i++) {
                        ImageView img = makeImageView(layoutImages);
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dpToPx(context, 200), 1f);
                        if (i == 0) lp.setMarginEnd(2);
                        img.setLayoutParams(lp);
                        Glide.with(context).load(urls.get(i)).centerCrop().into(img);
                        layoutImages.addView(img);
                    }
                } else {
                    layoutImages.setOrientation(LinearLayout.HORIZONTAL);
                    // Left Image
                    ImageView imgLeft = makeImageView(layoutImages);
                    LinearLayout.LayoutParams lpLeft = new LinearLayout.LayoutParams(0, dpToPx(context, 200), 1f);
                    lpLeft.setMarginEnd(2);
                    imgLeft.setLayoutParams(lpLeft);
                    Glide.with(context).load(urls.get(0)).centerCrop().into(imgLeft);
                    layoutImages.addView(imgLeft);

                    // Right Column
                    LinearLayout colRight = new LinearLayout(context);
                    colRight.setOrientation(LinearLayout.VERTICAL);
                    LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(0, dpToPx(context, 200), 1f);
                    colRight.setLayoutParams(colLp);

                    for (int i = 1; i <= 2 && i < urls.size(); i++) {
                        ImageView img = makeImageView(colRight);
                        LinearLayout.LayoutParams imgLp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
                        if (i == 1 && urls.size() > 2) imgLp.setMargins(0, 0, 0, 2);
                        img.setLayoutParams(imgLp);
                        
                        if (i == 2 && urls.size() > 3) {
                            FrameLayout frame = new FrameLayout(context);
                            frame.setLayoutParams(imgLp);
                            frame.addView(img);
                            
                            TextView overlay = new TextView(context);
                            overlay.setLayoutParams(new FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
                            overlay.setGravity(Gravity.CENTER);
                            overlay.setText("+" + (urls.size() - 3));
                            overlay.setTextColor(0xFFFFFFFF);
                            overlay.setTextSize(18);
                            overlay.setTypeface(null, Typeface.BOLD);
                            overlay.setBackgroundColor(0x88000000);
                            frame.addView(overlay);
                            colRight.addView(frame);
                        } else {
                            colRight.addView(img);
                        }
                        Glide.with(context).load(urls.get(i)).centerCrop().into(img);
                    }
                    layoutImages.addView(colRight);
                }
            } else {
                layoutImages.setVisibility(View.GONE);
                recyclerImages.setVisibility(View.GONE);
            }

            // Click Listeners
            btnLike.setOnClickListener(v -> {
                // Không tự toggle ở đây, để Repository xử lý và báo về qua LiveData
                if (listener != null) listener.onLikeClick(post, getAdapterPosition() - 1);
            });

            btnComment.setOnClickListener(v -> {
                if (listener != null) listener.onCommentClick(post);
            });

            btnShare.setOnClickListener(v -> {
                // Không tự toggle ở đây
                if (listener != null) listener.onShareClick(post);
            });

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onPostClick(post);
            });
        }
    }

    private void updateLikeUI(PostViewHolder h, Post post) {
        if (post.isLiked()) {
            h.imgLike.setImageResource(R.drawable.ic_like_filled);
            h.txtLike.setTextColor(context.getResources().getColor(R.color.light_blue_600));
        } else {
            h.imgLike.setImageResource(R.drawable.ic_like);
            h.txtLike.setTextColor(context.getResources().getColor(R.color.gray_600));
        }
    }

    private ImageView makeImageView(ViewGroup parent) {
        ImageView img = new ImageView(parent.getContext());
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        return img;
    }

    private int dpToPx(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private String formatCount(int count) {
        if (count < 1000) return String.valueOf(count);
        if (count < 1000000) return String.format("%.1fK", count / 1000.0);
        return String.format("%.1fM", count / 1000000.0);
    }
}
