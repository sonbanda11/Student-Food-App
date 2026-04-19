package com.example.studentfood.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Lấy đường đi bằng OSRM (OpenStreetMap) — miễn phí, không cần API key.
 */
public class DirectionsHelper {

    private static final String TAG = "DirectionsHelper";

    // Danh sách server OSRM fallback
    private static final String[] OSRM_SERVERS = {
        "https://router.project-osrm.org",
        "https://routing.openstreetmap.de"
    };

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface DirectionsCallback {
        void onSuccess(List<LatLng> points);
        void onError(String message);
    }

    public void getRoute(LatLng origin, LatLng destination, DirectionsCallback callback) {
        executor.execute(() -> {
            List<LatLng> points = new ArrayList<>();
            String lastError = "Unknown error";

            for (String server : OSRM_SERVERS) {
                try {
                    // OSRM: longitude trước, latitude sau
                    String urlStr = server + "/route/v1/walking/"
                        + origin.longitude + "," + origin.latitude + ";"
                        + destination.longitude + "," + destination.latitude
                        + "?overview=full&geometries=geojson";

                    Log.d(TAG, "Trying: " + urlStr);

                    URL url = new URL(urlStr);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);
                    conn.setRequestProperty("User-Agent", "StudentFoodApp/1.0");

                    int code = conn.getResponseCode();
                    Log.d(TAG, "HTTP " + code + " from " + server);

                    if (code != 200) {
                        lastError = "HTTP " + code;
                        conn.disconnect();
                        continue;
                    }

                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();
                    conn.disconnect();

                    String response = sb.toString();
                    Log.d(TAG, "Response snippet: " + response.substring(0, Math.min(200, response.length())));

                    points = parseOsrmResponse(response);
                    Log.d(TAG, "Decoded " + points.size() + " points");

                    if (!points.isEmpty()) break; // Thành công, thoát vòng lặp

                    lastError = "Empty route from " + server;

                } catch (Exception e) {
                    lastError = e.getMessage();
                    Log.e(TAG, "Error with " + server + ": " + e.getMessage());
                }
            }

            final List<LatLng> finalPoints = points;
            final String finalError = lastError;
            mainHandler.post(() -> {
                if (!finalPoints.isEmpty()) {
                    callback.onSuccess(finalPoints);
                } else {
                    callback.onError(finalError);
                }
            });
        });
    }

    private List<LatLng> parseOsrmResponse(String json) {
        List<LatLng> points = new ArrayList<>();
        try {
            JSONObject root = new JSONObject(json);
            String status = root.optString("code", "");
            if (!"Ok".equals(status)) {
                Log.e(TAG, "OSRM status: " + status + " | message: " + root.optString("message"));
                return points;
            }

            JSONArray routes = root.getJSONArray("routes");
            if (routes.length() == 0) return points;

            JSONArray coords = routes.getJSONObject(0)
                .getJSONObject("geometry")
                .getJSONArray("coordinates");

            for (int i = 0; i < coords.length(); i++) {
                JSONArray c = coords.getJSONArray(i);
                // OSRM trả về [longitude, latitude]
                points.add(new LatLng(c.getDouble(1), c.getDouble(0)));
            }
        } catch (Exception e) {
            Log.e(TAG, "Parse error: " + e.getMessage(), e);
        }
        return points;
    }

    public void shutdown() {
        if (!executor.isShutdown()) executor.shutdown();
    }
}
