package com.example.studentfood.presentation.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.example.studentfood.R;
import com.example.studentfood.domain.model.Place;
import com.example.studentfood.domain.model.Restaurant;
import androidx.fragment.app.Fragment;

import com.example.studentfood.presentation.ui.activity.PlaceDetailActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Unified PlaceAdapter for all POI types (Restaurant, Cafe, Market, etc.)
 * Follows MVVM and DRY principles.
 */
public class PlaceAdapter extends RecyclerView.Adapter<PlaceAdapter.ViewHolder> {

    public static final int TYPE_HORIZONTAL = 1;
    public static final int TYPE_VERTICAL = 2;

    public interface OnItemClickListener {
        void onItemClick(Place place);
    }

    private final Context context;
    private final List<Place> items = new ArrayList<>();
    private final int orientation;
    private double userLat = 0, userLng = 0;

    private OnItemClickListener onItemClickListener;

    public PlaceAdapter(Context context, int orientation) {
        this.context = context;
        this.orientation = orientation;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public void setUserLocation(double lat, double lng) {
        this.userLat = lat;
        this.userLng = lng;
    }

    public void updateData(List<? extends Place> data) {
        android.util.Log.d("PlaceAdapter", "=== UPDATE DATA CALLED ===");
        android.util.Log.d("PlaceAdapter", "Data null: " + (data == null));
        android.util.Log.d("PlaceAdapter", "Data size: " + (data != null ? data.size() : 0));

        if (data == null) {
            items.clear();
            notifyDataSetChanged();
            android.util.Log.d("PlaceAdapter", "Cleared items and notified");
            return;
        }

        // Use a new list to avoid side effects if 'data' is modified elsewhere
        List<Place> newList = new ArrayList<>(data);

        // Final sanity check for UI stability
        items.clear();
        items.addAll(newList);

        android.util.Log.d("PlaceAdapter", "Items cleared and refilled. New size: " + items.size());

        // Ensure this runs on the main thread if called from background
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            notifyDataSetChanged();
            android.util.Log.d("PlaceAdapter", "Notified on main thread");
        } else {
            new android.os.Handler(android.os.Looper.getMainLooper()).post(this::notifyDataSetChanged);
            android.util.Log.d("PlaceAdapter", "Posted notification to main thread");
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutRes = (orientation == TYPE_HORIZONTAL)
                ? R.layout.item_place_hori
                : R.layout.item_place_vertical;
        View v = LayoutInflater.from(context).inflate(layoutRes, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        android.util.Log.d("PlaceAdapter", "=== ON BIND VIEW HOLDER ===");
        android.util.Log.d("PlaceAdapter", "Position: " + position + ", Items size: " + items.size());

        if (position < 0 || position >= items.size()) {
            android.util.Log.w("PlaceAdapter", "Invalid position: " + position);
            return;
        }

        Place p = items.get(position);
        android.util.Log.d("PlaceAdapter", "Binding place: " + p.getName() + " (" + p.getType() + ")");

        // Basic Info
        holder.txtName.setText(p.getName());
        holder.txtDistance.setText(p.getDistanceDisplay());
        holder.txtRating.setText(String.format(java.util.Locale.getDefault(), "%.1f", p.getRating()));
        holder.txtTotalReviews.setText(String.format(java.util.Locale.getDefault(), "(%d+)", p.getTotalReviews()));

        // Description / Address
        String description = (p.getAddress() != null && !p.getAddress().isEmpty())
                ? p.getAddress()
                : p.getShortSummary();
        holder.txtDescription.setText(description);

        // Price handling - Show price for ALL place types
        String priceText;
        if (p instanceof Restaurant) {
            String restaurantPrice = ((Restaurant) p).getFormattedPriceRange();
            priceText = (restaurantPrice != null && !restaurantPrice.trim().isEmpty())
                ? restaurantPrice
                : "Chưa cập nhật giá";
        } else {
            // For all other place types, use formatted price
            String placePrice = p.getFormattedPrice();
            priceText = (placePrice != null && !placePrice.trim().isEmpty() && !placePrice.equals("$"))
                ? placePrice
                : "Chưa cập nhật giá";
        }
        android.util.Log.d("PlaceAdapter", "Price for " + p.getName() + " (" + p.getType() + "): " + priceText);
        holder.txtPriceRange.setText(priceText);

        // Image loading - User's required logic: Default icon, only use image if uploaded
        java.util.List<String> imageUrls = p.getImageUrls();
        if (imageUrls != null && !imageUrls.isEmpty()) {
            // User uploaded image exists - use it
            String firstImageUrl = imageUrls.get(0);
            android.util.Log.d("PlaceAdapter", "Using USER IMAGE for: " + p.getName() + " -> " + firstImageUrl);
            Glide.with(context)
                    .load(firstImageUrl)
                    .transform(new CenterCrop(), new RoundedCorners(24))
                    .placeholder(getCategoryIcon(p.getType()))
                    .error(getCategoryIcon(p.getType()))
                    .into(holder.imgIcon);
        } else {
            // No user uploaded image - use category icon as default
            int iconRes = getCategoryIcon(p.getType());
            android.util.Log.d("PlaceAdapter", "Using CATEGORY ICON for: " + p.getName() + " -> " + iconRes + " (type: " + p.getType() + ")");
            holder.imgIcon.setImageResource(iconRes);
        }

        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(p);
            } else {
                navigateToDetail(p);
            }
        });

        applyCategoryStyle(p.getType(), holder.iconContainer, holder.imgIcon);
    }

    /**
     * Get icon resource ID for place category
     * @param type Place type enum
     * @return Drawable resource ID
     */
    private int getCategoryIcon(Place.PlaceType type) {
        if (type == null) return R.drawable.ic_placeholder;

        switch (type) {
            case RESTAURANT:
                return R.drawable.ic_restaurant;
            case CAFE:
                return R.drawable.ic_coffee_cup;
            case MARKET:
                return R.drawable.ic_bag;
            case SUPERMARKET:
                return R.drawable.ic_cart;
            case CONVENIENCE:
                return R.drawable.ic_convenience;
            case FAST_FOOD:
                return R.drawable.ic_pizza;
            case VENDING:
                return R.drawable.ic_vending;
            default:
                return R.drawable.ic_placeholder;
        }
    }
    private void applyCategoryStyle(Place.PlaceType type, View container, ImageView icon) {

        if (type == null) return;

        switch (type) {

            case RESTAURANT:
                container.setBackground(
                        ContextCompat.getDrawable(context, R.drawable.bg_gradient_orange)
                );
                icon.setColorFilter(
                        ContextCompat.getColor(context, R.color.cat_restaurant));
                break;

            case MARKET:
                container.setBackground(
                        ContextCompat.getDrawable(context, R.drawable.bg_gradient_green)
                );
                icon.setColorFilter(
                        ContextCompat.getColor(context, R.color.cat_market));
                break;

            case SUPERMARKET:
                container.setBackground(
                        ContextCompat.getDrawable(context, R.drawable.bg_gradient_bluegreen)
                );
                icon.setColorFilter(
                        ContextCompat.getColor(context, R.color.cat_supermarket));
                break;

            case CONVENIENCE:
                container.setBackground(
                        ContextCompat.getDrawable(context, R.drawable.bg_gradient_blue)
                );
                icon.setColorFilter(
                        ContextCompat.getColor(context, R.color.cat_supermarket));
                break;

            case CAFE:
                container.setBackground(
                        ContextCompat.getDrawable(context, R.drawable.bg_gradient_yellow)
                );
                icon.setColorFilter(
                        ContextCompat.getColor(context, R.color.cat_cafe));
                break;

            case FAST_FOOD:
                container.setBackground(
                        ContextCompat.getDrawable(context, R.drawable.bg_gradient_puple)
                );
                icon.setColorFilter(
                        ContextCompat.getColor(context, R.color.cat_fastfood));
                break;

            case VENDING:
                container.setBackground(
                        ContextCompat.getDrawable(context, R.drawable.bg_gradient_red)
                );
                icon.setColorFilter(
                        ContextCompat.getColor(context, R.color.cat_vending));
                break;
            default:
                container.setBackground(
                        ContextCompat.getDrawable(context, R.drawable.bg_gradient_tag)
                );
                icon.setColorFilter(
                        ContextCompat.getColor(context, R.color.cat_restaurant));
                break;
        }
    }

    public void navigateToDetail(Place p) {
        Intent intent = new Intent(context, PlaceDetailActivity.class);

        // ✅ CHỈ dùng 1 flow duy nhất
        intent.putExtra(PlaceDetailActivity.DATA_TYPE, PlaceDetailActivity.TYPE_OSM_PLACE);

        intent.putExtra(PlaceDetailActivity.EXTRA_PLACE_ID, p.getId());
        intent.putExtra(PlaceDetailActivity.EXTRA_PLACE_NAME, p.getName());

        if (p.getType() != null) {
            intent.putExtra(PlaceDetailActivity.EXTRA_PLACE_TYPE, p.getType().ordinal());
        }

        intent.putExtra(PlaceDetailActivity.EXTRA_PLACE_LAT, p.getLatitude());
        intent.putExtra(PlaceDetailActivity.EXTRA_PLACE_LNG, p.getLongitude());
        intent.putExtra(PlaceDetailActivity.EXTRA_USER_LAT, userLat);
        intent.putExtra(PlaceDetailActivity.EXTRA_USER_LNG, userLng);
        intent.putExtra(
                PlaceDetailActivity.EXTRA_PLACE_TYPE,
                p.getType() != null ? p.getType().ordinal() : Place.PlaceType.RESTAURANT.ordinal()
        );

        // Pass additional place data
        if (p.getLocation() != null && p.getLocation().getAddress() != null) {
            intent.putExtra(PlaceDetailActivity.EXTRA_PLACE_ADDRESS, p.getLocation().getAddress());
        }
        if (p.getPhone() != null) {
            intent.putExtra(PlaceDetailActivity.EXTRA_PLACE_PHONE, p.getPhone());
        }
        if (p.getOpeningHours() != null) {
            intent.putExtra(PlaceDetailActivity.EXTRA_PLACE_HOURS, p.getOpeningHours());
        }
        if (p.getWebsite() != null) {
            intent.putExtra(PlaceDetailActivity.EXTRA_PLACE_WEBSITE, p.getWebsite());
        }
        if (p.getBrand() != null) {
            intent.putExtra(PlaceDetailActivity.EXTRA_PLACE_BRAND, p.getBrand());
        }
        
        // Pass images if available
        if (p.getImageUrls() != null && !p.getImageUrls().isEmpty()) {
            intent.putExtra(PlaceDetailActivity.EXTRA_PLACE_IMAGES, p.getImageUrls().toArray(new String[0]));
        }

        context.startActivity(intent);
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgIcon;
        FrameLayout iconContainer;
        TextView txtName, txtDescription, txtDistance, txtRating, txtTotalReviews, txtPriceRange;

        ViewHolder(@NonNull View v) {
            super(v);

            imgIcon = v.findViewById(R.id.imgRestaurant);
            txtName = v.findViewById(R.id.txtRestaurantName);
            txtDescription = v.findViewById(R.id.txtDescription);
            txtDistance = v.findViewById(R.id.txtDistance);
            txtRating = v.findViewById(R.id.txtRating);
            txtTotalReviews = v.findViewById(R.id.txtTotalReviews);
            txtPriceRange = v.findViewById(R.id.txtPriceRange);

            iconContainer = v.findViewById(R.id.iconContainer);

            imgIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        }
    }
}
