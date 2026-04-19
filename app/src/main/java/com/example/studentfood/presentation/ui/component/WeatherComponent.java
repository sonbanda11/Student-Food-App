package com.example.studentfood.presentation.ui.component;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.lifecycle.LifecycleOwner;

import com.example.studentfood.R;
import com.example.studentfood.data.remote.repository.WeatherResponse;
import com.example.studentfood.presentation.viewmodel.HomeViewModel;
import com.example.studentfood.utils.WeatherMapper;

public class WeatherComponent {

    private TextView txtTemp, txtCity, txtWeatherDesc, txtFoodSuggest, txtFoodLabel;
    private ImageView imgWeather, imgFood;
    private CardView cardWeather;
    private View lineDivider;

    private Context context;
    private HomeViewModel viewModel;
    private LifecycleOwner lifecycleOwner;
    private ObjectAnimator currentAnimator;
    
    public WeatherComponent(Context context,
                            View rootView,
                            LifecycleOwner lifecycleOwner,
                            HomeViewModel viewModel) {

        this.context = context;
        this.viewModel = viewModel;
        this.lifecycleOwner = lifecycleOwner;

        // Tim weather card trong include layout
        cardWeather = rootView.findViewById(R.id.cardWeather);
        
        if (cardWeather != null) {
            // Tim các view bên trong cardWeather
            txtTemp = cardWeather.findViewById(R.id.txtTemp);
            txtCity = cardWeather.findViewById(R.id.txtCity);
            imgWeather = cardWeather.findViewById(R.id.imgWeather);
            txtWeatherDesc = cardWeather.findViewById(R.id.txtWeatherDesc);
            
            // Tim layout food suggest
            LinearLayout layoutFoodSuggest = cardWeather.findViewById(R.id.layoutFoodSuggest);
            if (layoutFoodSuggest != null) {
                txtFoodSuggest = layoutFoodSuggest.findViewById(R.id.txtFoodSuggest);
                txtFoodLabel = layoutFoodSuggest.findViewById(R.id.txtFoodLabel);
                imgFood = layoutFoodSuggest.findViewById(R.id.imgFood);
            }
            
            // Tim thanh chia
            lineDivider = cardWeather.findViewById(R.id.lineDivider);
        }

        observeWeather();
        
        // Test logic mới (chỉ để debug, xóa khi production)
        WeatherMapper.testTimePeriodLogic();
    }

    private void observeWeather() {
        Log.d("WeatherComponent", "Starting to observe weather data");
        viewModel.getWeatherLiveData().observe(lifecycleOwner, data -> {
            Log.d("WeatherComponent", "Weather data received: " + (data != null ? data.name + " " + data.main.temp + "°C" : "null"));
            if (data != null) {
                updateWeatherUI(data);
            } else {
                Log.d("WeatherComponent", "Weather data is null, setting default weather");
                setDefaultWeather();
            }
        });
    }

    public void updateWeatherUI(WeatherResponse data) {
        Log.d("WeatherComponent", "updateWeatherUI called with data: " + (data != null ? "non-null" : "null"));
        if (data == null) {
            Log.d("WeatherComponent", "Data is null, setting default weather");
            setDefaultWeather();
            return;
        }

        // Su dung WeatherMapper de map data (sẽ tự động tính isNight dựa trên sunrise/sunset)
        WeatherMapper.WeatherData weatherData = WeatherMapper.mapWeatherResponse(data, context, false); // isNight parameter sẽ bị ignore

        // Cap nhat UI voi weather data
        Log.d("WeatherComponent", "Updating UI - Temp: " + Math.round(data.main.temp) + "°C, City: " + weatherData.cityName);
        if (txtTemp != null) txtTemp.setText(Math.round(data.main.temp) + "°C");
        if (txtCity != null) txtCity.setText(weatherData.cityName);
        if (txtWeatherDesc != null) txtWeatherDesc.setText(weatherData.weatherDescription);
        if (imgWeather != null) imgWeather.setImageResource(weatherData.weatherIcon);
        if (cardWeather != null) cardWeather.setBackgroundResource(weatherData.backgroundResource);

        // Cap nhat mau sac
        if (txtTemp != null) txtTemp.setTextColor(weatherData.textColor);
        if (txtCity != null) txtCity.setTextColor(weatherData.textColor);
        if (txtWeatherDesc != null) txtWeatherDesc.setTextColor(weatherData.subTextColor);
        
        if (txtFoodSuggest != null) txtFoodSuggest.setTextColor(weatherData.subTextColor);
        if (txtFoodLabel != null) txtFoodLabel.setTextColor(weatherData.textColor);
        if (lineDivider != null) lineDivider.setBackgroundColor(weatherData.dividerColor);

        // Cap nhat goi y mon an
//        if (txtFoodSuggest != null) {
//            // txtFoodSuggest.setText(weatherData.foodSuggestion);
//        }
//        if (txtFoodLabel != null) {
//            txtFoodLabel.setText(weatherData.foodLabel);
//        }

        // Ap dung animation
        if (weatherData.shouldAnimate) {
            WeatherMapper.applyAnimation(imgWeather, weatherData.animationType, currentAnimator);
        } else {
            WeatherMapper.stopAnimation(currentAnimator);
        }
    }

    
    private void setDefaultWeather() {
        Log.d("WeatherComponent", "setDefaultWeather called - waiting for data");
        // No hardcoded data here, let it be empty or a loading state until observeWeather receives data
        if (txtTemp != null) txtTemp.setText("--°C");
        if (txtCity != null) txtCity.setText("Đang tải...");
        if (txtWeatherDesc != null) txtWeatherDesc.setText("...");
    }

    public void navigateToWeatherDetail() {
        Intent intent = new Intent(context, com.example.studentfood.presentation.ui.activity.WeatherFoodSuggestionActivity.class);
        
        // Get current location from HomeViewModel if available
        if (viewModel != null) {
            // Try to get location from weather data
            com.example.studentfood.data.remote.repository.WeatherResponse weatherData = viewModel.getWeatherLiveData().getValue();
            if (weatherData != null && weatherData.coord != null) {
                intent.putExtra(com.example.studentfood.presentation.ui.activity.WeatherFoodSuggestionActivity.EXTRA_USER_LAT, weatherData.coord.lat);
                intent.putExtra(com.example.studentfood.presentation.ui.activity.WeatherFoodSuggestionActivity.EXTRA_USER_LNG, weatherData.coord.lon);
                intent.putExtra("city_name", weatherData.name);
                Log.d("WeatherComponent", "Passing location to WeatherFoodSuggestion: " + weatherData.coord.lat + ", " + weatherData.coord.lon);
            } else {
                Log.w("WeatherComponent", "No weather data available for location");
            }
        }
        
        context.startActivity(intent);
    }
}
