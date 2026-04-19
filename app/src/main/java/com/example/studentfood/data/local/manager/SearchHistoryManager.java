package com.example.studentfood.data.local.manager;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class  SearchHistoryManager {

    private static final String PREF_NAME = "search_history.json";
    private static final String KEY_HISTORY = "history_list";

    public static void saveHistory(Context context, String keyword) {

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Set<String> historySet = prefs.getStringSet(KEY_HISTORY, new HashSet<>());

        historySet.add(keyword);

        prefs.edit().putStringSet(KEY_HISTORY, historySet).apply();
    }

    public static List<String> getHistory(Context context) {

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Set<String> historySet = prefs.getStringSet(KEY_HISTORY, new HashSet<>());

        return new ArrayList<>(historySet);
    }

    public static void clearHistory(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_HISTORY).apply();
    }
}