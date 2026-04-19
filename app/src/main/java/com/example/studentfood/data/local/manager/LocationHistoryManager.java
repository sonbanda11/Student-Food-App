package com.example.studentfood.data.local.manager;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LocationHistoryManager {

    private static final String PREF_NAME = "location_history";
    private static final String KEY_HISTORY = "history";
    private static final int MAX_SIZE = 10;

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static void saveLocation(Context context, String address) {

        if (address == null) return;

        address = address.trim();

        // ❗ chặn rác
        if (address.isEmpty() || address.equals("Đang lấy địa chỉ...")) return;

        List<String> list = getHistory(context);

        // ❗ tránh trùng (ignore case)
        String finalAddress = address;
        list.removeIf(item -> item.equalsIgnoreCase(finalAddress));

        // thêm lên đầu
        list.add(0, address);

        // ❗ giới hạn size
        if (list.size() > MAX_SIZE) {
            list = list.subList(0, MAX_SIZE);
        }

        saveList(context, list);
    }

    public static List<String> getHistory(Context context) {

        String data = getPrefs(context).getString(KEY_HISTORY, "");

        if (data == null || data.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<String> list = new ArrayList<>(Arrays.asList(data.split("\\|")));

        // ❗ clean data (tránh lỗi rác)
        list.removeIf(item -> item == null || item.trim().isEmpty());

        return list;
    }

    private static void saveList(Context context, List<String> list) {

        String data = String.join("|", list);

        getPrefs(context)
                .edit()
                .putString(KEY_HISTORY, data)
                .apply();
    }

    // 🔥 BONUS: clear history
    public static void clearHistory(Context context) {
        getPrefs(context)
                .edit()
                .remove(KEY_HISTORY)
                .apply();
    }
}