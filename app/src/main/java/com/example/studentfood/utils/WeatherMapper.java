package com.example.studentfood.utils;

import android.content.Context;
import android.os.Build;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.studentfood.R;
import com.example.studentfood.data.remote.repository.WeatherResponse;

/**
 * WeatherMapper - Class chung cho logic weather mapping
 * Dùng cho Activity và Component, tránh duplicate logic
 */
public class WeatherMapper {

    // Lớp dữ liệu thời tiết
    public static class WeatherData {
        public String cityName;
        public String weatherDescription;
        public int weatherIcon;
        public int backgroundResource;
        public String foodSuggestion;
        public String foodLabel;
        public int textColor;
        public int subTextColor;
        public int dividerColor;
        public int iconTintColor;
        public AnimationType animationType;
        public boolean shouldAnimate;

        public enum AnimationType {
            SUN,
            CLOUD,
            RAIN,
            SNOW,
            FOG,
            STORM,
            NONE
        }
    }

    /**
     * Map weather response sang WeatherData
     */
    public static WeatherData mapWeatherResponse(WeatherResponse data, Context context, boolean isNight) {
        // fallback sớm
        if (data == null || data.main == null) {
            return getDefaultWeatherData(context, isNight);
        }

        WeatherData result = new WeatherData();

        // ===== 1. Temperature =====
        float temp = data.main.temp;

        // ===== 2. City =====
        String cityName = "Unknown";
        if (data.name != null && !data.name.trim().isEmpty()) {
            cityName = data.name;
        }
        result.cityName = cityName;

        // ===== 3. Weather info =====
        String weatherMain = "Unknown";
        String weatherDesc = "Unknown";

        if (data.weather != null && data.weather.length > 0 && data.weather[0] != null) {
            if (data.weather[0].main != null) {
                weatherMain = data.weather[0].main;
            }
            if (data.weather[0].description != null) {
                weatherDesc = data.weather[0].description;
            }
        }

        result.weatherDescription = weatherDesc;

        // ===== 4. NEW: Xác định thời gian chính xác dựa trên sunrise/sunset =====
        String timePeriod = getTimePeriod(data);
        boolean isNightAccurate = "NIGHT".equals(timePeriod);
        
        // Log để debug logic mới
        android.util.Log.d("WeatherMapper", "Time period detected: " + timePeriod + ", isNight: " + isNightAccurate);
        
        // ===== 5. Mapping UI =====
        mapWeatherConditions(result, weatherMain, weatherDesc, temp, isNightAccurate);

        // ===== 6. Colors =====
        setWeatherColors(result, isNightAccurate, weatherMain, temp);

        // ===== 7. Food suggestion =====
        //setFoodSuggestion(result, temp, weatherMain, isNightAccurate);

        return result;
    }
    /**
     * Xác định thời gian trong ngày dựa trên sunrise/sunset
     * Hoạt động chính xác cho mọi quốc gia, không phụ thuộc vào mùa
     */
    private static String getTimePeriod(WeatherResponse weatherResponse) {
        // Fallback: nếu không có sunrise/sunset, dùng logic đơn giản
        if (weatherResponse == null || weatherResponse.sys == null) {
            android.util.Log.d("WeatherMapper", "Using fallback - weatherResponse or sys is null");
            return getTimePeriodFallback();
        }
        
        long currentTime = weatherResponse.dt * 1000L; // Convert to milliseconds
        long sunriseTime = weatherResponse.sys.sunrise * 1000L;
        long sunsetTime = weatherResponse.sys.sunset * 1000L;
        
        android.util.Log.d("WeatherMapper", String.format("Time data - current: %d, sunrise: %d, sunset: %d", 
            currentTime, sunriseTime, sunsetTime));
        
        // Nếu dữ liệu không hợp lệ, dùng fallback
        if (sunriseTime <= 0 || sunsetTime <= 0 || currentTime <= 0) {
            android.util.Log.d("WeatherMapper", "Using fallback - invalid time data");
            return getTimePeriodFallback();
        }
        
        // Tính toán các mốc thời gian
        long morningEnd = sunriseTime + (3 * 60 * 60 * 1000L); // 3 giờ sau sunrise
        long eveningStart = sunsetTime - (2 * 60 * 60 * 1000L); // 2 giờ trước sunset
        long nightStart = sunsetTime + (2 * 60 * 60 * 1000L); // 2 giờ sau sunset
        
        android.util.Log.d("WeatherMapper", String.format("Time periods - morningEnd: %d, eveningStart: %d, nightStart: %d", 
            morningEnd, eveningStart, nightStart));
        
        // Xác định thời gian trong ngày
        String period;
        if (currentTime >= sunriseTime && currentTime < morningEnd) {
            period = "MORNING";
        } else if (currentTime >= morningEnd && currentTime < eveningStart) {
            period = "DAY";
        } else if (currentTime >= eveningStart && currentTime < sunsetTime) {
            period = "EVENING"; // Buổi tối sớm (trước và ngay sau sunset)
        } else if (currentTime >= sunsetTime && currentTime < nightStart) {
            period = "EVENING"; // Vẫn là buổi tối sớm (2 giờ sau sunset)
        } else {
            period = "NIGHT"; // Ban đêm muộn (sau 2 giờ sau sunset)
        }
        
        android.util.Log.d("WeatherMapper", "Detected time period: " + period);
        return period;
    }
    
    /**
     * Fallback logic khi không có sunrise/sunset data
     * Dùng giờ cục bộ với logic đơn giản
     */
    private static String getTimePeriodFallback() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int hourOfDay = calendar.get(java.util.Calendar.HOUR_OF_DAY);
        
        if (hourOfDay >= 5 && hourOfDay < 8) {
            return "MORNING";
        } else if (hourOfDay >= 8 && hourOfDay < 17) {
            return "DAY";
        } else if (hourOfDay >= 17 && hourOfDay < 22) {
            return "EVENING"; // 17:00-21:59 là buổi tối
        } else {
            return "NIGHT"; // 22:00-4:59 là ban đêm
        }
    }
    
    /**
     * Kiểm tra có phải là ban đêm không (dùng cho UI theme)
     * Chỉ return true cho NIGHT, không phải EVENING
     */
    private static boolean isNightTime(WeatherResponse weatherResponse) {
        String timePeriod = getTimePeriod(weatherResponse);
        return "NIGHT".equals(timePeriod);
    }
    
    /**
     * Kiểm tra có phải là buổi tối không (EVENING)
     * Dùng cho các UI cần theme buổi tối nhưng chưa phải đêm
     */
    public static boolean isCurrentlyEvening(WeatherResponse weatherResponse) {
        String timePeriod = getTimePeriod(weatherResponse);
        return "EVENING".equals(timePeriod);
    }
    
    /**
     * Public method để lấy time period hiện tại
     * Dùng cho các component khác cần custom UI theo thời gian
     */
    public static String getCurrentTimePeriod(WeatherResponse weatherResponse) {
        return getTimePeriod(weatherResponse);
    }
    
    /**
     * Public method để kiểm tra ban đêm
     * Dùng cho các component khác cần theme night/day
     */
    public static boolean isCurrentlyNight(WeatherResponse weatherResponse) {
        return isNightTime(weatherResponse);
    }
    
    /**
     * Test method để verify logic với sample data
     * Chỉ dùng cho debugging, xóa khi production
     */
    public static void testTimePeriodLogic() {
        android.util.Log.d("WeatherMapper", "=== Testing Time Period Logic ===");
        
        // Test với fallback logic
        String fallbackPeriod = getTimePeriod(null);
        android.util.Log.d("WeatherMapper", "Fallback period: " + fallbackPeriod);
        
        // Test MORNING (7:00)
        WeatherResponse morningData = createTestData(7, 0, 6, 0, 18, 0);
        android.util.Log.d("WeatherMapper", "MORNING test (7:00): " + getTimePeriod(morningData));
        
        // Test DAY (12:00)
        WeatherResponse dayData = createTestData(12, 0, 6, 0, 18, 0);
        android.util.Log.d("WeatherMapper", "DAY test (12:00): " + getTimePeriod(dayData));
        
        // Test EVENING (17:00 - trước sunset)
        WeatherResponse eveningData = createTestData(17, 0, 6, 0, 18, 0);
        android.util.Log.d("WeatherMapper", "EVENING test (17:00): " + getTimePeriod(eveningData));
        
        // Test EVENING (19:00 - sau sunset)
        WeatherResponse eveningAfterData = createTestData(19, 0, 6, 0, 18, 0);
        android.util.Log.d("WeatherMapper", "EVENING test (19:00): " + getTimePeriod(eveningAfterData));
        
        // Test NIGHT (23:00 - sau 2 giờ sunset)
        WeatherResponse nightData = createTestData(23, 0, 6, 0, 18, 0);
        android.util.Log.d("WeatherMapper", "NIGHT test (23:00): " + getTimePeriod(nightData));
        
        android.util.Log.d("WeatherMapper", "=== End Test ===");
    }
    
    /**
     * Helper method để tạo test data
     */
    private static WeatherResponse createTestData(int currentHour, int currentMin, 
                                               int sunriseHour, int sunriseMin,
                                               int sunsetHour, int sunsetMin) {
        WeatherResponse data = new WeatherResponse();
        data.sys = new WeatherResponse.Sys();
        
        java.util.Calendar cal = java.util.Calendar.getInstance();
        
        // Set current time
        cal.set(java.util.Calendar.HOUR_OF_DAY, currentHour);
        cal.set(java.util.Calendar.MINUTE, currentMin);
        cal.set(java.util.Calendar.SECOND, 0);
        data.dt = cal.getTimeInMillis() / 1000L;
        
        // Set sunrise
        cal.set(java.util.Calendar.HOUR_OF_DAY, sunriseHour);
        cal.set(java.util.Calendar.MINUTE, sunriseMin);
        data.sys.sunrise = cal.getTimeInMillis() / 1000L;
        
        // Set sunset
        cal.set(java.util.Calendar.HOUR_OF_DAY, sunsetHour);
        cal.set(java.util.Calendar.MINUTE, sunsetMin);
        data.sys.sunset = cal.getTimeInMillis() / 1000L;
        
        return data;
    }
    /**
     * Ánh xạ điều kiện thời tiết thành dữ liệu UI
     */
    private static void mapWeatherConditions(WeatherData result, String weatherMain, String weatherDesc, float temp, boolean isNight) {
        switch (weatherMain) {
            case "Clear":
            case "Clouds":

                if (!isNight) {
                    // ❄️ LẠNH
                    if (temp <= 20) {
                        result.weatherIcon = R.drawable.ic_cloud;
                        result.backgroundResource = R.drawable.bg_weather_cloud;
                        result.animationType = WeatherData.AnimationType.CLOUD;
                        result.shouldAnimate = true;

                        if (temp <= 10) {
                            result.weatherDescription = "Buốt giá";
                        } else if (temp <= 15) {
                            result.weatherDescription = "Trời lạnh";
                        } else {
                            result.weatherDescription = "Trời se lạnh";
                        }
                    }

                    // 🌤 MÁT / DỄ CHỊU (MỚI THÊM)
                    else if (temp <= 32) {
                        result.weatherIcon = (weatherMain.equals("Clouds"))
                                ? R.drawable.ic_cloud
                                : R.drawable.ic_sun;

                        result.backgroundResource = R.drawable.bg_weather_cloud;
                        result.animationType = WeatherData.AnimationType.CLOUD;
                        result.shouldAnimate = true;
                        result.weatherDescription = "Thời tiết dễ chịu";
                    }

                    // 🔥 NẮNG
                    else {
                        result.weatherIcon = R.drawable.ic_sun;
                        result.backgroundResource = R.drawable.bg_weather_sunny;
                        result.animationType = WeatherData.AnimationType.SUN;
                        result.shouldAnimate = true;

                        if (temp >= 36) {
                            result.weatherDescription = "Nắng gắt";
                        } else {
                            result.weatherDescription = "Trời nắng";
                        }
                    }
                }

                else {
                    // 🌙 NIGHT (FIX LẠI LOGIC)
                    result.weatherIcon = R.drawable.ic_moon;
                    result.backgroundResource = R.drawable.bg_weather_night;
                    result.animationType = WeatherData.AnimationType.NONE;
                    result.shouldAnimate = false;

                    if (temp <= 10) {
                        result.weatherDescription = "Đêm rét buốt";
                    } else if (temp <= 18) {
                        result.weatherDescription = "Đêm lạnh";
                    } else if (temp <= 28) {
                        result.weatherDescription = "Đêm se lạnh";
                    } else {
                        result.weatherDescription = "Đêm oi bức";
                    }
                }

                break;


            case "Rain":

                result.weatherIcon = R.drawable.ic_rain;
                result.animationType = WeatherData.AnimationType.RAIN;
                result.shouldAnimate = true;

                if (isNight) {
                    // 🌙 NIGHT + RAIN
                    result.backgroundResource = R.drawable.bg_weather_night;

                    if (temp <= 20) {
                        result.weatherDescription = "Đêm mưa lạnh";
                    } else {
                        result.weatherDescription = "Đêm mưa";
                    }

                } else {
                    // ☀️ DAY + RAIN
                    result.backgroundResource = R.drawable.bg_weather_rain;

                    if (temp >= 30) {
                        result.weatherDescription = "Mưa nhưng vẫn oi bức";
                    } else if (temp <= 20) {
                        result.weatherDescription = "Trời mưa lạnh";
                    } else {
                        result.weatherDescription = "Trời đang mưa";
                    }
                }

                break;


            case "Drizzle":

                result.weatherIcon = R.drawable.ic_rain;
                result.animationType = WeatherData.AnimationType.RAIN;
                result.shouldAnimate = true;

                if (isNight) {
                    // 🌙 NIGHT + DRIZZLE
                    result.backgroundResource = R.drawable.bg_weather_night;

                    if (temp <= 20) {
                        result.weatherDescription = "Đêm mưa phùn lạnh";
                    } else {
                        result.weatherDescription = "Đêm mưa phùn";
                    }

                } else {
                    // ☀️ DAY + DRIZZLE
                    result.backgroundResource = R.drawable.bg_weather_rain;

                    if (temp <= 20) {
                        result.weatherDescription = "Mưa phùn se lạnh";
                    } else {
                        result.weatherDescription = "Mưa phùn nhẹ";
                    }
                }

                break;


            case "Thunderstorm":

                result.weatherIcon = R.drawable.ic_storm;
                result.animationType = WeatherData.AnimationType.STORM;
                result.shouldAnimate = true;

                if (isNight) {
                    // 🌙 NIGHT + STORM
                    result.backgroundResource = R.drawable.bg_weather_night;

                    if (temp <= 20) {
                        result.weatherDescription = "Đêm dông lạnh";
                    } else {
                        result.weatherDescription = "Đêm dông sét";
                    }

                } else {
                    // ☀️ DAY + STORM
                    result.backgroundResource = R.drawable.bg_weather_storm;

                    if (temp >= 30) {
                        result.weatherDescription = "Dông nóng oi bức";
                    } else if (temp <= 20) {
                        result.weatherDescription = "Dông lạnh";
                    } else {
                        result.weatherDescription = "Dông sét";
                    }
                }

                break;


            case "Mist":
            case "Smoke":
            case "Haze":
            case "Dust":
            case "Fog":

                result.weatherIcon = R.drawable.ic_fog;
                result.animationType = WeatherData.AnimationType.FOG;
                result.shouldAnimate = true;

                if (isNight) {
                    // 🌙 NIGHT + FOG
                    result.backgroundResource = R.drawable.bg_weather_night;

                    if (temp <= 20) {
                        result.weatherDescription = "Đêm sương mù lạnh";
                    } else {
                        result.weatherDescription = "Đêm sương mù";
                    }

                } else {
                    // ☀️ DAY + FOG
                    result.backgroundResource = R.drawable.bg_weather_fog;

                    switch (weatherMain) {

                        case "Mist":
                            result.weatherDescription = "Sương nhẹ";
                            break;

                        case "Haze":
                            result.weatherDescription = "Không khí mờ";
                            break;

                        case "Fog":
                            result.weatherDescription = "Sương mù dày";
                            break;

                        case "Smoke":
                            result.weatherDescription = "Khói mù không khí";
                            break;

                        case "Dust":
                            result.weatherDescription = "Bụi mù ngoài trời";
                            break;

                        default:
                            result.weatherDescription = "Sương mù";
                            break;
                    }
                }
                break;
            case "Snow":

                result.weatherIcon = R.drawable.ic_snow;
                result.animationType = WeatherData.AnimationType.SNOW;
                result.shouldAnimate = true;

                if (isNight) {
                    // 🌙 NIGHT + SNOW
                    result.backgroundResource = R.drawable.bg_weather_night;

                    if (temp <= 0) {
                        result.weatherDescription = "Đêm tuyết rơi buốt giá";
                    } else if (temp <= 5) {
                        result.weatherDescription = "Đêm tuyết lạnh";
                    } else {
                        result.weatherDescription = "Đêm lạnh có tuyết";
                    }

                } else {
                    // ☀️ DAY + SNOW
                    result.backgroundResource = R.drawable.bg_weather_snow;

                    if (temp <= 0) {
                        result.weatherDescription = "Tuyết rơi buốt giá";
                    } else if (temp <= 5) {
                        result.weatherDescription = "Tuyết rơi lạnh giá";
                    } else if (temp <= 10) {
                        result.weatherDescription = "Tuyết rơi nhẹ";
                    } else {
                        result.weatherDescription = "Hiếm gặp tuyết rơi";
                    }
                }

                break;


            default:

                // 🌙 NIGHT fallback
                if (isNight) {

                    result.weatherIcon = R.drawable.ic_moon;
                    result.backgroundResource = R.drawable.bg_weather_night;
                    result.animationType = WeatherData.AnimationType.NONE;
                    result.shouldAnimate = false;

                    if (temp <= 10) {
                        result.weatherDescription = "Đêm rét buốt";
                    } else if (temp <= 18) {
                        result.weatherDescription = "Đêm lạnh";
                    } else if (temp <= 28) {
                        result.weatherDescription = "Đêm se lạnh";
                    } else {
                        result.weatherDescription = "Đêm oi bức";
                    }

                } else {

                    // ☀️ DAY fallback
                    result.weatherIcon = R.drawable.ic_cloud;
                    result.backgroundResource = R.drawable.bg_weather_cloud;
                    result.animationType = WeatherData.AnimationType.CLOUD;
                    result.shouldAnimate = true;

                    if (temp <= 20) {
                        result.weatherDescription = "Thời tiết hơi lạnh";
                    } else if (temp <= 32) {
                        result.weatherDescription = "Thời tiết dễ chịu";
                    } else {
                        result.weatherDescription = "Thời tiết oi bức";
                    }
                }

                break;
        }
    }

    /**
     * Cài đặt màu sắc thời tiết dựa trên thời gian
     */
    private static void setWeatherColors(WeatherData result, boolean isNight, String weatherMain, float temp) {

        boolean isHot = temp >= 33;
        boolean isCold = temp <= 20;

        // 🌙 NIGHT MODE
        if (isNight) {

            result.textColor = 0xFFE0E0E0; // xám trắng cho chữ chính
            result.subTextColor = 0xFFB0B0B0; // xám nhạt cho chữ phụ
            result.dividerColor = 0x40E0E0E0; // xám trắng cho divider
            result.iconTintColor = 0xFFE0E0E0; // xám trắng cho icon (fastfood, arrow)
            return;
        }

        // ☀️ DAY MODE (SMART THEME)

        switch (weatherMain) {

            case "Rain":
            case "Drizzle":
            case "Thunderstorm":

                // 🌧 Cool tone (blue cold UI)
                result.textColor = 0xFFFFFFFF;
                result.subTextColor = 0xFFE3F2FD;
                result.dividerColor = 0x33FFFFFF;
                result.iconTintColor = 0xFFE3F2FD; // xanh lạnh
                break;

            case "Snow":

                // ❄ Ice theme
                result.textColor = 0xFF0D47A1;
                result.subTextColor = 0xFF1976D2;
                result.dividerColor = 0x22000000;
                result.iconTintColor = 0xFF90CAF9; // xanh băng
                break;

            case "Fog":
            case "Mist":
            case "Haze":

                // 🌫 Soft gray UI
                result.textColor = 0xFF263238;
                result.subTextColor = 0xFF546E7A;
                result.dividerColor = 0x33000000;
                result.iconTintColor = 0xFF90A4AE; // xám sương
                break;

            case "Clear":

                if (isHot) {
                    // 🔥 hot sun theme
                    result.textColor = 0xFF1A1A1A;
                    result.subTextColor = 0xFF424242;
                    result.dividerColor = 0x33000000;
                    result.iconTintColor = 0xFFFFB74D; // vàng nắng
                } else {
                    // ☀️ normal clean sky
                    result.textColor = 0xFF0D47A1;
                    result.subTextColor = 0xFF1976D2;
                    result.dividerColor = 0x33000000;

                    result.iconTintColor = 0xFF42A5F5; // xanh trời
                }
                break;

            case "Clouds":
            default:

                // ☁ neutral sky
                result.textColor = 0xFF212121;
                result.subTextColor = 0xFF616161;
                result.dividerColor = 0x33000000;
                result.iconTintColor = 0xFF90A4AE; // mây xám xanh
                break;
        }
    }

    /**
     * Cài đặt gợi ý món ăn dựa trên thời tiết
     */
//    private static void setFoodSuggestion(WeatherData result, float temp, String weatherMain, boolean isNight) {
//
//        result.foodLabel = "Hôm nay ăn gì?";
//
//        String w = weatherMain != null ? weatherMain : "Unknown";
//
//        // 🌙 NIGHT MODE PRIORITY
//
//        if (isNight) {
//
//            if (w.equalsIgnoreCase("Rain") || w.equalsIgnoreCase("Drizzle")) {
//
//                result.foodSuggestion =
//                        temp <= 20
//                                ? "Đêm mưa lạnh, làm tô phở nóng hay lẩu cho ấm bụng nhé! 🍲"
//                                : "Đêm mưa nhẹ, ăn mì nóng hoặc cháo cho dễ ngủ 🌙";
//
//            } else if (w.equalsIgnoreCase("Thunderstorm")) {
//
//                result.foodSuggestion = "Đêm dông sét, ở nhà ăn mì nóng cho an toàn ⚡";
//
//            } else if (temp <= 20) {
//
//                result.foodSuggestion =
//                        temp <= 10
//                                ? "Đêm rét buốt, ăn lẩu nóng hoặc đồ cay cho ấm người 🔥"
//                                : "Đêm lạnh, ăn đồ nướng hoặc soup nóng 🍢";
//
//            } else if (temp <= 28) {
//
//                result.foodSuggestion = "Đêm mát, ăn nhẹ như mì hoặc đồ ăn vặt là hợp 🌙";
//
//            } else {
//
//                result.foodSuggestion = "Đêm hơi nóng, ăn nhẹ và uống nước mát thôi 😴";
//            }
//
//            return;
//        }
//
//        // ☀️ DAY MODE
//        if (w.equalsIgnoreCase("Rain") || w.equalsIgnoreCase("Drizzle")) {
//
//            result.foodSuggestion =
//                    temp <= 20
//                            ? "Trời mưa lạnh, làm bát phở nóng hay lẩu nấm nhé! 🍲"
//                            : "Trời mưa nhẹ, ăn đồ nóng cho chill 🌧";
//
//        } else if (temp >= 33) {
//
//            result.foodSuggestion =
//                    "Trời nóng gắt, giải nhiệt bằng trà chanh hoặc bún chả nào! 🍹";
//
//        } else if (temp <= 20) {
//
//            result.foodSuggestion =
//                    "Trời se lạnh, thịt nướng hay đồ cay nóng là nhất! 🍢";
//
//        } else if (w.equalsIgnoreCase("Clear") || w.equalsIgnoreCase("Clouds")) {
//
//            result.foodSuggestion =
//                    "Thời tiết đẹp, đi ăn cơm tấm hoặc cà phê với bạn bè ☕";
//
//        } else {
//
//            result.foodSuggestion =
//                    "Tìm món ngon phù hợp với tâm trạng của bạn ngay! 😋";
//        }
//    }

    /**
     * Lấy dữ liệu thời tiết mặc định khi API lỗi
     */
    private static WeatherData getDefaultWeatherData(Context context, boolean isNight) {

        WeatherData result = new WeatherData();

        result.cityName = "Hà Nội";
        result.weatherDescription = "Không có dữ liệu";

        result.weatherIcon = R.drawable.ic_cloud;

        result.backgroundResource = isNight
                ? R.drawable.bg_weather_night
                : R.drawable.bg_weather_cloud;

        result.foodLabel = "Gợi ý món ăn";
        result.foodSuggestion = "Bấm xem chi tiết";

        result.animationType = WeatherData.AnimationType.NONE;
        result.shouldAnimate = false;

        // ✅ SAFE FALLBACK VALUES
        setWeatherColors(result, isNight, "Unknown", 25f);

        return result;
    }

    /**
     * Áp dụng animation thời tiết vào ImageView
     */
    public static ObjectAnimator applyAnimation(ImageView imageView, WeatherData.AnimationType type, ObjectAnimator currentAnimator) {

        if (imageView == null) return null;

        if (currentAnimator != null) {
            currentAnimator.cancel();
        }

        imageView.setRotation(0f);
        imageView.setTranslationX(0f);
        imageView.setTranslationY(0f);
        imageView.setAlpha(1f);

        switch (type) {

            // ☀️ SUN
            case SUN:

                imageView.setPivotX(imageView.getWidth() / 2f);
                imageView.setPivotY(imageView.getHeight() / 2f);

                currentAnimator = ObjectAnimator.ofFloat(imageView, "rotation", 0f, 360f);
                currentAnimator.setDuration(6000);
                currentAnimator.setRepeatCount(ValueAnimator.INFINITE);
                currentAnimator.setInterpolator(new android.view.animation.LinearInterpolator());

                break;

            // ☁️ CLOUD
            case CLOUD:

                currentAnimator = ObjectAnimator.ofFloat(imageView, "translationX", -20f, 20f);
                currentAnimator.setDuration(6000); // chậm hơn để giống mây
                currentAnimator.setRepeatMode(ValueAnimator.REVERSE);
                currentAnimator.setRepeatCount(ValueAnimator.INFINITE);
                currentAnimator.setInterpolator(new android.view.animation.LinearInterpolator());

                imageView.setAlpha(0.85f);

                break;

            // 🌧️ RAIN
            case RAIN:

                currentAnimator = ObjectAnimator.ofFloat(imageView, "translationY", -10f, 10f);
                currentAnimator.setDuration(1400); // nhanh hơn (trước 2000)
                currentAnimator.setRepeatMode(ValueAnimator.REVERSE);
                currentAnimator.setRepeatCount(ValueAnimator.INFINITE);
                currentAnimator.setInterpolator(new android.view.animation.LinearInterpolator());

                break;

            // ❄️ SNOW (chậm + nhẹ)
            case SNOW:
                currentAnimator = ObjectAnimator.ofFloat(imageView, "translationY", -40f, 40f);
                currentAnimator.setDuration(2500);
                currentAnimator.setRepeatMode(ValueAnimator.RESTART);
                currentAnimator.setRepeatCount(ValueAnimator.INFINITE);
                break;

            // 🌫️ FOG (trôi mờ)
            case FOG:
                currentAnimator = ObjectAnimator.ofFloat(imageView, "translationX", -15f, 15f);
                currentAnimator.setDuration(6000);
                currentAnimator.setRepeatMode(ValueAnimator.REVERSE);
                currentAnimator.setRepeatCount(ValueAnimator.INFINITE);

                imageView.setAlpha(0.5f);
                break;

            // ⛈️ STORM (flash + shake)
            case STORM:

                currentAnimator = ObjectAnimator.ofFloat(imageView, "translationX", -10f, 10f);
                currentAnimator.setDuration(300);
                currentAnimator.setRepeatCount(ValueAnimator.INFINITE); // ⬅️ chạy mãi
                currentAnimator.setRepeatMode(ValueAnimator.REVERSE);

                imageView.setAlpha(0.7f);

                break;

            default:
                return null;
        }

        if (currentAnimator != null) {
            currentAnimator.start();
        }

        return currentAnimator;
    }

    /**
     * Dừng animation hiện tại
     */
    public static void stopAnimation(ObjectAnimator currentAnimator) {
        if (currentAnimator != null) {
            currentAnimator.cancel();
        }
    }
}
