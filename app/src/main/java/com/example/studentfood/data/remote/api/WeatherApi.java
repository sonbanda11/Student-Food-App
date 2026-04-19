package com.example.studentfood.data.remote.api;

// Import class chứa cấu trúc dữ liệu JSON trả về từ API
import com.example.studentfood.data.remote.repository.WeatherResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Interface định nghĩa các request API thời tiết dùng Retrofit
 */
public interface WeatherApi {

    /**
     * GET request tới endpoint "data/2.5/weather"
     *
     * @param city Tên thành phố, ví dụ "Hanoi"
     * @param units Đơn vị nhiệt độ, ví dụ "metric" (Celsius) hoặc "imperial" (Fahrenheit)
     * @param apiKey API key để xác thực với OpenWeatherMap
     * @return Call<WeatherResponse> - Retrofit sẽ trả về JSON được parse thành WeatherResponse
     */
    @GET("data/2.5/weather")
    Call<WeatherResponse> getWeather(
            @Query("q") String city,     // tham số 'q' trong URL
            @Query("units") String units, // tham số 'units' trong URL
            @Query("appid") String apiKey // tham số 'appid' trong URL
    );

    /**
     * GET weather by coordinates
     */
    @GET("data/2.5/weather")
    Call<WeatherResponse> getWeatherByCoords(
            @Query("lat") double lat,
            @Query("lon") double lon,
            @Query("units") String units,
            @Query("appid") String apiKey
    );
}