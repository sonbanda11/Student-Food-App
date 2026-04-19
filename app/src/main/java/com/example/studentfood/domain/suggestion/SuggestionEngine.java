package com.example.studentfood.domain.suggestion;

import com.example.studentfood.domain.model.MenuItem;
import com.example.studentfood.domain.model.Restaurant;
import com.example.studentfood.utils.WeatherMapper;
import com.example.studentfood.data.remote.repository.WeatherResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * So khop thoi tiet (nhi t d, m m, mã tình trang) voi danh mc món trong DB.
 * Dùng WeatherMapper cho logic time period chính xác
 */
public final class SuggestionEngine {

    public enum FoodProfile {
        /** 5h-11h: an nhanh gon */
        MORNING,
        /** Lanh / mua / m cao: do nóng, nc dùng, nung */
        COLD_OR_WET,
        /** Nóng: giai nhi t, do uong, nh */
        HOT,
        /** M c nh */
        NEUTRAL
    }

    private SuggestionEngine() {}

    /**
     * NEW: Classify using WeatherMapper time period logic
     * Dùng sunrise/sunset cho chính xác toàn c u
     */
    public static FoodProfile classify(float tempC, float humidityPercent, String weatherMain, WeatherResponse weatherResponse) {
        // 1. u tiên bu i sáng - dùng WeatherMapper logic chính xác
        String timePeriod = WeatherMapper.getCurrentTimePeriod(weatherResponse);
        if ("MORNING".equals(timePeriod)) {
            return FoodProfile.MORNING;
        }

        String w = weatherMain != null ? weatherMain.trim() : "";
        // Ch tính là "u t" nu có mua th c s, bo Mist/Fog ra vì nó ch là sng/m
        boolean strictlyWet = w.equalsIgnoreCase("Rain")
            || w.equalsIgnoreCase("Drizzle")
            || w.equalsIgnoreCase("Thunderstorm")
            || w.equalsIgnoreCase("Snow");

        // 2. Logic Lanh hoac Mua: 
        // - Có mua/tuyt th c s
        // - Hoac nhi t d xu du 17 d (m c th c s cn n d nóng)
        // - Hoac se lanh (du 20 d) kèm m m c cao (>92%) - kiu tr i nm/m  wet khó ch
        if (strictlyWet || tempC < 17f || (tempC < 20f && humidityPercent > 92)) {
            return FoodProfile.COLD_OR_WET;
        }

        // 3. Trói nóng (Nhi t d cn nhng thng > 29-30 d)
        if (tempC >= 29f) {
            return FoodProfile.HOT;
        }

        // 4. M c nh: Trói mt mt, d ch (ba gàm ca ban m Hanoi mùa này 20-25 d)
        return FoodProfile.NEUTRAL;
    }
    
    /**
     * Legacy method for backward compatibility - uses simple hour logic
     * @deprecated Use classify(WeatherResponse) instead
     */
    @Deprecated
    public static FoodProfile classify(float tempC, float humidityPercent, String weatherMain, int hourOfDay) {
        // 1. u tiên bu i sáng (5h - 10h)
        if (hourOfDay >= 5 && hourOfDay < 10) {
            return FoodProfile.MORNING;
        }

        String w = weatherMain != null ? weatherMain.trim() : "";
        // Ch tính là "u t" nu có mua th c s, bo Mist/Fog ra vì nó ch là sng/m
        boolean strictlyWet = w.equalsIgnoreCase("Rain")
            || w.equalsIgnoreCase("Drizzle")
            || w.equalsIgnoreCase("Thunderstorm")
            || w.equalsIgnoreCase("Snow");

        // 2. Logic Lanh hoac Mua: 
        // - Có mua/tuyt th c s
        // - Hoac nhi t d xu du 17 d (m c th c s cn n d nóng)
        // - Hoac se lanh (du 20 d) kèm m m c cao (>92%) - kiu tr i nm/m  wet khó ch
        if (strictlyWet || tempC < 17f || (tempC < 20f && humidityPercent > 92)) {
            return FoodProfile.COLD_OR_WET;
        }

        // 3. Trói nóng (Nhi t d cn nhng thng > 29-30 d)
        if (tempC >= 29f) {
            return FoodProfile.HOT;
        }

        // 4. M c nh: Trói mt mt, d ch (ba gàm ca ban m Hanoi mùa này 20-25 d)
        return FoodProfile.NEUTRAL;
    }

    public static Set<String> categoryIdsFor(FoodProfile profile) {
        Set<String> ids = new HashSet<>();
        switch (profile) {
            case MORNING:
                ids.addAll(Arrays.asList("CAT_05", "CAT_07", "CAT_13", "CAT_01"));
                break;
            case COLD_OR_WET:
                ids.addAll(Arrays.asList("CAT_09", "CAT_02", "CAT_01", "CAT_15", "CAT_04"));
                break;
            case HOT:
                ids.addAll(Arrays.asList("CAT_03", "CAT_11", "CAT_16", "CAT_12", "CAT_02"));
                break;
            case NEUTRAL:
            default:
                ids.addAll(Arrays.asList("CAT_02", "CAT_01", "CAT_07", "CAT_03", "CAT_05"));
                break;
        }
        return ids;
    }

    public static String moodMessage(FoodProfile profile, String cityLabel, Random random) {
        String city = cityLabel != null ? cityLabel : "b n";
        List<String> pool = new ArrayList<>();
        switch (profile) {
            case MORNING:
                pool.add("Sáng ròi " + city + " i - xôi bánh mì nhanh gon cho k p ti t!");
                pool.add("Trói s trong veo, an sáng no b ng ròi cày ti p nhé!");
                pool.add("M t bánh mì nóng giòn là du nng luong ti p trua!");
                break;
            case COLD_OR_WET:
                pool.add("Trói mua lâm thâm - bát mì cay hay l u nóng là m cái b ng!");
                pool.add("Se se lanh ròi: ph bún nóng h i hay do nung th m lng thu ch a?");
                pool.add("M cao d n - do nóng, cay nh giúp d ch h n d y!");
                break;
            case HOT:
                pool.add("Trói oi b c - chè, tra s a, do mát cho nóng nóng trong ng i!");
                pool.add("Náng g rt th này: do uong mát, trai cây ho c bún nh là h p vibe!");
                pool.add("Nóng qu ròi - kem, s a chua hay tra o gi i nhi t li n!");
                break;
            case NEUTRAL:
            default:
                pool.add("An gi cng du - d app ch n giúp vài món h p gu sinh vi n nhé!");
                pool.add("Trói d ch - th i món m i cho b a nay thôi!");
                break;
        }
        return pool.get(random.nextInt(pool.size()));
    }

    public static String profileLabel(FoodProfile p) {
        switch (p) {
            case MORNING: return "Bu i sáng";
            case COLD_OR_WET: return "Trói lanh / mua";
            case HOT: return "Trói nóng";
            default: return "G i i chung";
        }
    }

    /**
     * L c món theo profile, xáo tr n, gi i h n s ph n tu.
     */
    public static List<WeatherFoodSuggestionItem> pickSuggestions(
        List<MenuItem> allItems,
        Map<String, Restaurant> restaurantById,
        FoodProfile profile,
        Random random,
        int limit
    ) {
        Set<String> wanted = categoryIdsFor(profile);
        List<MenuItem> pool = new ArrayList<>();
        for (MenuItem item : allItems) {
            if (item == null || !item.isAvailable()) continue;
            if (wanted.contains(item.getMenuCategoryId())) {
                pool.add(item);
            }
        }
        if (pool.isEmpty()) {
            wanted = categoryIdsFor(FoodProfile.NEUTRAL);
            for (MenuItem item : allItems) {
                if (item != null && item.isAvailable() && wanted.contains(item.getMenuCategoryId())) {
                    pool.add(item);
                }
            }
        }
        if (pool.isEmpty()) {
            for (MenuItem item : allItems) {
                if (item != null && item.isAvailable()) pool.add(item);
            }
        }
        Collections.shuffle(pool, random);
        int n = Math.min(limit, pool.size());
        List<WeatherFoodSuggestionItem> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            MenuItem item = pool.get(i);
            Restaurant r = restaurantById.get(item.getPlaceId());
            String resName = r != null ? r.getRestaurantName() : "Quán";
            String dist = "--";
            if (r != null && r.getLocation() != null) {
                dist = r.getLocation().getDistanceDisplay();
            }
            out.add(new WeatherFoodSuggestionItem(item, resName, dist));
        }
        return out;
    }

    public static String formatHumidity(float humidity) {
        return String.format(Locale.getDefault(), "%d%% m", Math.round(humidity));
    }
}
