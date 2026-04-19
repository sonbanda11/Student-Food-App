package com.example.studentfood.presentation.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.studentfood.R;
import com.example.studentfood.domain.model.FavoriteRestaurant;
import com.example.studentfood.utils.IconMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * FavoriteRestaurantAdapter - Adapter cho danh sách quán yêu thích
 */
public class FavoriteRestaurantAdapter extends RecyclerView.Adapter<FavoriteRestaurantAdapter.ViewHolder> {
    
    private List<FavoriteRestaurant> favorites;
    private OnFavoriteClickListener listener;
    private Context context;
    
    public interface OnFavoriteClickListener {
        void onFavoriteClick(FavoriteRestaurant favorite);
        void onFavoriteRemove(FavoriteRestaurant favorite);
        void onFavoriteShare(FavoriteRestaurant favorite);
    }
    
    public FavoriteRestaurantAdapter(List<FavoriteRestaurant> favorites) {
        this.favorites = favorites != null ? favorites : new ArrayList<>();
    }
    
    public void setOnFavoriteClickListener(OnFavoriteClickListener listener) {
        this.listener = listener;
    }
    
    public void setFavorites(List<FavoriteRestaurant> favorites) {
        this.favorites = favorites != null ? favorites : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_favorite_restaurant, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FavoriteRestaurant favorite = favorites.get(position);
        
        holder.bind(favorite);
    }
    
    @Override
    public int getItemCount() {
        return favorites.size();
    }
    
    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgRestaurant, imgFavorite, imgShare, imgRemove;
        TextView txtName, txtAddress, txtCategory, txtRating;
        RatingBar ratingBar;
        
        ViewHolder(View itemView) {
            super(itemView);
            
            imgRestaurant = itemView.findViewById(R.id.imgRestaurant);
            imgFavorite = itemView.findViewById(R.id.imgFavorite);
            imgShare = itemView.findViewById(R.id.imgShare);
            imgRemove = itemView.findViewById(R.id.imgRemove);
            txtName = itemView.findViewById(R.id.txtName);
            txtAddress = itemView.findViewById(R.id.txtAddress);
            txtCategory = itemView.findViewById(R.id.txtCategory);
            txtRating = itemView.findViewById(R.id.txtRating);
            ratingBar = itemView.findViewById(R.id.ratingBar);
        }
        
        void bind(FavoriteRestaurant favorite) {
            // Restaurant info
            txtName.setText(favorite.getRestaurantName());
            txtAddress.setText(favorite.getRestaurantAddress());
            txtCategory.setText(favorite.getRestaurantCategory());
            
            // Rating
            float rating = favorite.getRestaurantRating();
            txtRating.setText(String.format("%.1f", rating));
            ratingBar.setRating(rating);
            
            // Restaurant image - use icon if no custom image
            String imageUrl = favorite.getRestaurantImage();
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                // Use default restaurant icon
                imageUrl = "android.resource://" + context.getPackageName() + "/" + R.drawable.ic_rice_bowl;
            }
            
            Glide.with(context)
                .load(imageUrl)
                .placeholder(R.drawable.ic_rice_bowl)
                .error(R.drawable.ic_rice_bowl)
                .centerCrop()
                .into(imgRestaurant);
            
            // Favorite icon (always filled in favorites list)
            imgFavorite.setImageResource(R.drawable.ic_favorite_done);
            
            // Click listeners
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onFavoriteClick(favorite);
                }
            });
            
            imgFavorite.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onFavoriteClick(favorite);
                }
            });
            
            imgShare.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onFavoriteShare(favorite);
                }
            });
            
            imgRemove.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onFavoriteRemove(favorite);
                }
            });
        }
    }
}
