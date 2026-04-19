package com.example.studentfood.data.local.db;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * JsonReader - Utility class for reading and parsing JSON files from assets
 * Tách logic parse JSON ra riêng cho code clean và reuseable
 */
public class JsonReader {
    private static final String TAG = "JsonReader";
    private static final Gson gson = new Gson();

    /**
     * Load JSON string from assets folder
     * @param context Application context
     * @param fileName JSON file name in assets (e.g., "restaurant.json")
     * @return JSON string or null if error
     */
    public static String loadJSONFromAsset(Context context, String fileName) {
        String json;
        try {
            InputStream is = context.getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, StandardCharsets.UTF_8);
            return json;
        } catch (IOException ex) {
            Log.e(TAG, "Error reading from assets: " + fileName, ex);
            return null;
        }
    }

    /**
     * Parse JSON string to List of objects using Gson
     * @param jsonString JSON string
     * @param clazz Target class type
     * @param <T> Generic type
     * @return List of objects or empty list if error
     */
    public static <T> List<T> parseJsonToList(String jsonString, Class<T> clazz) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            Type listType = TypeToken.getParameterized(ArrayList.class, clazz).getType();
            return gson.fromJson(jsonString, listType);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing JSON to list: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Parse JSON string to single object using Gson
     * @param jsonString JSON string
     * @param clazz Target class type
     * @param <T> Generic type
     * @return Object or null if error
     */
    public static <T> T parseJsonToObject(String jsonString, Class<T> clazz) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return null;
        }

        try {
            return gson.fromJson(jsonString, clazz);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing JSON to object: " + e.getMessage());
            return null;
        }
    }

    /**
     * Parse JSON string to JSONArray (for manual parsing)
     * @param jsonString JSON string
     * @return JSONArray or null if error
     */
    public static JSONArray parseJsonToArray(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return null;
        }

        try {
            return new JSONArray(jsonString);
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON to array: " + e.getMessage());
            return null;
        }
    }

    /**
     * Parse JSON string to JSONObject (for manual parsing)
     * @param jsonString JSON string
     * @return JSONObject or null if error
     */
    public static JSONObject parseJsonToObject(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return null;
        }

        try {
            return new JSONObject(jsonString);
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON to object: " + e.getMessage());
            return null;
        }
    }

    /**
     * Read JSON from assets and parse to List using Gson
     * @param context Application context
     * @param fileName JSON file name
     * @param clazz Target class type
     * @param <T> Generic type
     * @return List of objects or empty list if error
     */
    public static <T> List<T> readJsonFromAssets(Context context, String fileName, Class<T> clazz) {
        String jsonString = loadJSONFromAsset(context, fileName);
        return parseJsonToList(jsonString, clazz);
    }

    /**
     * Read JSON from assets and parse to single object using Gson
     * @param context Application context
     * @param fileName JSON file name
     * @param clazz Target class type
     * @param <T> Generic type
     * @return Object or null if error
     */
    public static <T> T readSingleJsonFromAssets(Context context, String fileName, Class<T> clazz) {
        String jsonString = loadJSONFromAsset(context, fileName);
        return parseJsonToObject(jsonString, clazz);
    }

    /**
     * Read JSON from assets and parse to JSONArray (for manual parsing)
     * @param context Application context
     * @param fileName JSON file name
     * @return JSONArray or null if error
     */
    public static JSONArray readJsonArrayFromAssets(Context context, String fileName) {
        String jsonString = loadJSONFromAsset(context, fileName);
        return parseJsonToArray(jsonString);
    }

    /**
     * Read JSON from assets and parse to JSONObject (for manual parsing)
     * @param context Application context
     * @param fileName JSON file name
     * @return JSONObject or null if error
     */
    public static JSONObject readJsonObjectFromAssets(Context context, String fileName) {
        String jsonString = loadJSONFromAsset(context, fileName);
        return parseJsonToObject(jsonString);
    }

    /**
     * Check if JSON file exists in assets
     * @param context Application context
     * @param fileName JSON file name
     * @return true if file exists, false otherwise
     */
    public static boolean isJsonFileExists(Context context, String fileName) {
        try {
            InputStream is = context.getAssets().open(fileName);
            is.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Get all JSON files in assets folder (for debugging)
     * @param context Application context
     * @return Array of JSON file names
     */
    public static String[] getJsonFilesInAssets(Context context) {
        try {
            return context.getAssets().list("");
        } catch (IOException e) {
            Log.e(TAG, "Error listing assets: " + e.getMessage());
            return new String[0];
        }
    }
}
