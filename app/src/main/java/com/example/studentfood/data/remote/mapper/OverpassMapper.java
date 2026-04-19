package com.example.studentfood.data.remote.mapper;

import android.util.Log;
import com.example.studentfood.data.remote.dto.OverpassElement;
import com.example.studentfood.domain.model.Location;
import com.example.studentfood.domain.model.Place;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * OverpassMapper: Chịu trách nhiệm chuyển đổi DTO từ Overpass API sang Domain Model.
 * Giúp giải phóng logic cho Place.java và tuân thủ nguyên tắc Single Responsibility.
 */
public class OverpassMapper {

    private static final String TAG = "OverpassMapper";

    public static Place fromOverpassElement(OverpassElement element) {
        if (element == null) return null;

        try {
            Place place = new Place();
            String osmId = String.valueOf(element.getId());
            place.setId("osm_" + osmId);
            Map<String, String> tags = element.getTags();

            // 1. Resolve Name
            place.setName(resolvePlaceName(element, tags));

            // 2. Map Location
            Location loc = new Location();
            loc.setRefId(place.getId());
            if (element.getLat() != null) {
                loc.setLatitude(element.getLat());
                loc.setLongitude(element.getLon());
            } else if (element.getCenter() != null) {
                loc.setLatitude(element.getCenter().getLat());
                loc.setLongitude(element.getCenter().getLon());
            } else {
                // Critical fallback: if no coordinates, we can't show it on map/list
                return null;
            }
            loc.setAddress(element.getAddress() != null ? element.getAddress() : "Địa chỉ đang cập nhật");
            place.setLocation(loc);

            // 3. Map Type
            place.setType(mapOSMType(tags));

            // 4. Map Extra Data
            if (tags != null) {
                place.setCuisine(tags.get("cuisine"));
                place.setPhone(tags.getOrDefault("phone", tags.get("contact:phone")));
                place.setOpeningHours(tags.get("opening_hours"));
                place.setWebsite(tags.getOrDefault("website", tags.get("contact:website")));
                place.setFacebook(tags.get("contact:facebook"));

                String pLevel = tags.get("price_level");
                if (pLevel != null) {
                    try {
                        place.setPriceLevel(Integer.parseInt(pLevel));
                    } catch (Exception ignored) {}
                }
            }

            place.setSource(Place.DataSource.OSM);
            place.setAmenities(extractAmenities(tags));
            return place;
        } catch (Exception e) {
            Log.e(TAG, "Error mapping element: " + element.getId(), e);
            return null;
        }
    }

    private static String resolvePlaceName(OverpassElement element, Map<String, String> tags) {
        String name = element.getName();
        if (isValid(name)) return name;

        if (tags != null) {
            // Priority list for names
            if (isValid(tags.get("name:vi"))) return tags.get("name:vi");
            if (isValid(tags.get("name:en"))) return tags.get("name:en");
            if (isValid(tags.get("brand"))) return tags.get("brand");
            if (isValid(tags.get("operator"))) return tags.get("operator");
            if (isValid(tags.get("official_name"))) return tags.get("official_name");

            String shop = tags.get("shop");
            if (isValid(shop)) return formatShop(shop);

            String amenity = tags.get("amenity");
            if (isValid(amenity)) return formatAmenity(amenity);
            
            String cuisine = tags.get("cuisine");
            if (isValid(cuisine)) return "Quán " + cuisine;
        }
        return "Địa điểm gần đây";
    }

    private static boolean isValid(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private static String formatAmenity(String amenity) {
        switch (amenity) {
            case "restaurant": return "Nhà hàng";
            case "cafe": return "Quán cà phê";
            case "fast_food": return "Đồ ăn nhanh";
            default: return "Địa điểm";
        }
    }

    private static String formatShop(String shop) {
        switch (shop) {
            case "supermarket": return "Siêu thị";
            case "convenience": return "Cửa hàng tiện lợi";
            case "bakery": return "Tiệm bánh";
            default: return "Cửa hàng";
        }
    }

    private static Place.PlaceType mapOSMType(Map<String, String> tags) {
        if (tags == null) return Place.PlaceType.UNKNOWN;

        String amenity = safeLower(tags.get("amenity"));
        String shop = safeLower(tags.get("shop"));
        String cuisine = safeLower(tags.get("cuisine"));
        String vending = safeLower(tags.get("vending"));

        // 1. VENDING MACHINE
        if ("vending_machine".equals(amenity) || "yes".equals(vending) || "vending_machine".equals(tags.get("vending"))) {
            return Place.PlaceType.VENDING;
        }

        // 2. MARKETS / SHOPPING
        if (!shop.isEmpty()) {
            if ("supermarket".equals(shop)) return Place.PlaceType.SUPERMARKET;
            if ("convenience".equals(shop)) return Place.PlaceType.CONVENIENCE;
            if (shop.matches("mall|department_store|grocery|marketplace|bakery|greengrocer|butcher")) {
                return Place.PlaceType.MARKET;
            }
        }
        if ("marketplace".equals(amenity)) return Place.PlaceType.MARKET;

        // 3. CAFE & DRINKS
        if ("cafe".equals(amenity) || "pub".equals(amenity) || "bar".equals(amenity) || "bistro".equals(amenity) ||
            "ice_cream".equals(amenity) || "tea_shop".equals(amenity) ||
            cuisineMatches(cuisine, "coffee|tea|juice|dessert|bubble_tea|milk_tea|drink")) {
            return Place.PlaceType.CAFE;
        }

        // 4. FAST FOOD
        if ("fast_food".equals(amenity) || "food_court".equals(amenity) || "canteen".equals(amenity) ||
            cuisineMatches(cuisine, "burger|pizza|kebab|fried_chicken|street_food|local_food|noodle|rice|pho|bahn_mi")) {
            return Place.PlaceType.FAST_FOOD;
        }

        // 5. RESTAURANT
        if ("restaurant".equals(amenity) || "diner".equals(amenity) || "food_court".equals(amenity)) {
            return Place.PlaceType.RESTAURANT;
        }

        return Place.PlaceType.UNKNOWN;
    }

    private static String safeLower(String val) { return val == null ? "" : val.toLowerCase(); }

    private static boolean cuisineMatches(String cuisine, String regex) {
        if (cuisine == null) return false;
        String lower = cuisine.toLowerCase();
        for (String part : regex.split("\\|")) {
            if (lower.contains(part)) return true;
        }
        return false;
    }

    private static List<String> extractAmenities(Map<String, String> tags) {
        List<String> list = new ArrayList<>();
        if (tags == null) return list;
        if (isYes(tags.get("wifi"))) list.add("Wifi miễn phí");
        if (isYes(tags.get("air_conditioning"))) list.add("Điều hòa");
        if (isYes(tags.get("outdoor_seating"))) list.add("Chỗ ngồi ngoài trời");
        if (isYes(tags.get("parking"))) list.add("Có chỗ đỗ xe");
        if (isYes(tags.get("payment:momo"))) list.add("Ví MoMo");
        return list;
    }

    private static boolean isYes(String val) { return "yes".equals(val) || "free".equals(val); }
}
