package com.example.studentfood.presentation.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.studentfood.R;
import com.example.studentfood.domain.suggestion.WeatherFoodSuggestionItem;
import com.example.studentfood.presentation.ui.activity.PlaceDetailActivity;

import java.util.ArrayList;
import java.util.List;

public class WeatherFoodSuggestionAdapter extends RecyclerView.Adapter<WeatherFoodSuggestionAdapter.VH> {

    private final Context context;
    private final List<WeatherFoodSuggestionItem> items = new ArrayList<>();

    public WeatherFoodSuggestionAdapter(Context context) {
        this.context = context;
    }

    public void setData(List<WeatherFoodSuggestionItem> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_weather_menu_item_suggestion, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        WeatherFoodSuggestionItem row = items.get(position);
        h.txtName.setText(row.getMenuItem().getName());
        h.txtRestaurant.setText(row.getRestaurantName());
        h.txtPrice.setText(row.getMenuItem().getFormattedPrice());
        h.txtDistance.setText(row.getDistanceLabel());
        h.txtRating.setText(String.format(java.util.Locale.getDefault(),
                "%.1f ⭐", row.getMenuItem().getRating()));

        String url = row.getMenuItem().getImageUrl();
        if (url != null && !url.isEmpty()) {
            Glide.with(context).load(url).placeholder(R.drawable.sample_food).centerCrop().into(h.img);
        } else {
            h.img.setImageResource(R.drawable.sample_food);
        }

        h.itemView.setOnClickListener(v -> {
            Intent i = new Intent(context, PlaceDetailActivity.class);
            i.putExtra(PlaceDetailActivity.DATA_TYPE, PlaceDetailActivity.TYPE_RESTAURANT);
            i.putExtra(PlaceDetailActivity.EXTRA_RESTAURANT_ID, row.getRestaurantId());
            i.putExtra(PlaceDetailActivity.EXTRA_PLACE_NAME, row.getRestaurantName());
            i.putExtra(PlaceDetailActivity.EXTRA_PLACE_LAT, 0.0); // Will be updated by location service
            i.putExtra(PlaceDetailActivity.EXTRA_PLACE_LNG, 0.0); // Will be updated by location service
            context.startActivity(i);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView img;
        TextView txtName, txtRestaurant, txtPrice, txtDistance, txtRating;

        VH(@NonNull View v) {
            super(v);
            img = v.findViewById(R.id.imgFood);
            txtName = v.findViewById(R.id.txtFoodName);
            txtRestaurant = v.findViewById(R.id.txtRestaurant);
            txtPrice = v.findViewById(R.id.txtPrice);
            txtDistance = v.findViewById(R.id.txtDistance);
            txtRating = v.findViewById(R.id.txtRatingSmall);
        }
    }
}
