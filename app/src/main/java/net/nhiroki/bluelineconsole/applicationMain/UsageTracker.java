package net.nhiroki.bluelineconsole.applicationMain;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class UsageTracker {
    private static final String PREFS_NAME = "blc_usage_tracker";

    public static void recordLaunch(Context context, String key) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
             .putInt("count_" + key, prefs.getInt("count_" + key, 0) + 1)
             .putLong("time_" + key, System.currentTimeMillis())
             .apply();
    }

    public static int getUsageCount(Context context, String key) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                      .getInt("count_" + key, 0);
    }

    public static List<String> getRecentKeys(Context context, int limit) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Map<String, ?> all = prefs.getAll();
        List<Map.Entry<String, Long>> timeEntries = new ArrayList<>();
        for (Map.Entry<String, ?> entry : all.entrySet()) {
            if (entry.getKey().startsWith("time_") && entry.getValue() instanceof Long) {
                timeEntries.add(new AbstractMap.SimpleEntry<>(
                    entry.getKey().substring(5), (Long) entry.getValue()));
            }
        }
        Collections.sort(timeEntries, (a, b) -> Long.compare(b.getValue(), a.getValue()));
        List<String> result = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, timeEntries.size()); i++) {
            result.add(timeEntries.get(i).getKey());
        }
        return result;
    }
}
