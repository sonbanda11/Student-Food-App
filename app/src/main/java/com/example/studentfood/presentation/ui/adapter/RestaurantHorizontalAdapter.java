package com.example.studentfood.presentation.ui.adapter;

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
import com.example.studentfood.domain.model.Restaurant;
import com.example.studentfood.domain.model.Image;
import com.example.studentfood.utils.IconMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for Restaurant items in horizontal layout
 */
public class RestaurantHorizontalAdapter extends RecyclerView.Adapter<RestaurantHorizontalAdapter.ViewHolder> {

    public interface OnRestaurantClickListener {
        void onRestaurantClick(Restaurant restaurant);
    }

    private final Context context;
    private final List<Restaurant> items = new ArrayList<>();
    private OnRestaurantClickListener listener;

    public RestaurantHorizontalAdapter(Context context) {
        this.context = context;
    }

    public RestaurantHorizontalAdapter(Context context, OnRestaurantClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setOnRestaurantClickListener(OnRestaurantClickListener listener) {
        this.listener = listener;
    }

    public void updateRestaurants(List<Restaurant> restaurants) {
        items.clear();
        if (restaurants != null) {
            items.addAll(restaurants);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_place_hori, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Restaurant restaurant = items.get(position);
        
        // Set restaurant name
        holder.txtRestaurantName.setText(restaurant.getRestaurantName());
        
        // Set rating
        holder.txtRating.setText(String.format("%.1f", restaurant.getRating()));
        
        // Set total reviews if available
        if (restaurant.getTotalReviews() > 0) {
            holder.txtTotalReviews.setText("(+" + restaurant.getTotalReviews() + ")");
        } else {
            holder.txtTotalReviews.setText("(+0)");
        }
        
        // Set distance if available
        if (restaurant.getLocation() != null && restaurant.getLocation().getDistance() > 0) {
            holder.txtDistance.setText(String.format("%.1f km", restaurant.getLocation().getDistance()));
        } else {
            holder.txtDistance.setText("N/A");
        }
        
        // Set price range if available
        if (restaurant.getMinPrice() > 0 && restaurant.getMaxPrice() > 0) {
            holder.txtPriceRange.setText(String.format("%,dđ - %,dđ", 
                (int)restaurant.getMinPrice(), (int)restaurant.getMaxPrice()));
        } else {
            holder.txtPriceRange.setText("Chưa cập nhật giá");
        }
        
        // Load restaurant image
        loadRestaurantImage(holder.imgRestaurant, restaurant);
        
        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRestaurantClick(restaurant);
            }
        });
    }

    private void loadRestaurantImage(ImageView imageView, Restaurant restaurant) {
        // User's required logic: Default icon, only use image if uploaded
        java.util.List<String> imageUrls = restaurant.getImageUrls();
        if (imageUrls != null && !imageUrls.isEmpty()) {
            // User uploaded image exists - use it
            String firstImageUrl = imageUrls.get(0);
            Glide.with(context)
                    .load(firstImageUrl)
                    .placeholder(R.drawable.ic_rice_bowl)
                    .error(R.drawable.ic_rice_bowl)
                    .centerCrop()
                    .into(imageView);
        } else {
            // No user uploaded image - use restaurant icon as default
            imageView.setImageResource(R.drawable.ic_rice_bowl);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * Get restaurant at position
     */
    public Restaurant getRestaurant(int position) {
        if (position >= 0 && position < items.size()) {
            return items.get(position);
        }
        return null;
    }

    /**
     * Get all restaurants
     */
    public List<Restaurant> getAllRestaurants() {
        return new ArrayList<>(items);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgRestaurant;
        TextView txtRestaurantName;
        TextView txtRating;
        TextView txtTotalReviews;
        TextView txtDistance;
        TextView txtPriceRange;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            
            imgRestaurant = itemView.findViewById(R.id.imgRestaurant);
            txtRestaurantName = itemView.findViewById(R.id.txtRestaurantName);
            txtRating = itemView.findViewById(R.id.txtRating);
            txtTotalReviews = itemView.findViewById(R.id.txtTotalReviews);
            txtDistance = itemView.findViewById(R.id.txtDistance);
            txtPriceRange = itemView.findViewById(R.id.txtPriceRange);
        }
    }
}
