package com.example.studentfood.data.remote.api;

import android.net.Uri;

/**
 * Không gọi HTTP: tạo URL / {@link Uri} cho bản đồ tĩnh OSM, Google Maps (deep link / web).
 * Dùng từ ViewModel / Activity thông qua luồng MVVM.
 */
public final class MapLinksApi {

    private static final String OSM_STATIC = "https://staticmap.openstreetmap.de/staticmap.php";

    private MapLinksApi() {}

    public static String osmStaticMapBannerUrl(double lat, double lng) {
        return OSM_STATIC
            + "?center=" + lat + "," + lng
            + "&zoom=16&size=600x300"
            + "&markers=" + lat + "," + lng + ",red-pushpin";
    }

    /** Deep link mở Google Maps chỉ đường tới điểm đích. */
    public static Uri googleNavigationUri(double destLat, double destLng) {
        return Uri.parse("google.navigation:q=" + destLat + "," + destLng);
    }

    /** Web fallback khi không cài Google Maps (chỉ đích). */
    public static String googleMapsWebDestinationUrl(double destLat, double destLng) {
        return "https://www.google.com/maps/dir/?api=1&destination=" + destLat + "," + destLng;
    }

    /** Web: chỉ đường từ vị trí user tới đích (khi có tọa độ user). */
    public static String googleMapsWebDirUrl(double fromLat, double fromLng,
                                               double toLat, double toLng) {
        return "https://www.google.com/maps/dir/" + fromLat + "," + fromLng + "/" + toLat + "," + toLng;
    }

    public static String googleMapsGeoQueryUrl(double lat, double lng) {
        return "https://www.google.com/maps?q=" + lat + "," + lng;
    }
}
