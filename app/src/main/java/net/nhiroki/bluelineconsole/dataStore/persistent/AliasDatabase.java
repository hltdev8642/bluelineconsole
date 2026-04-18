package net.nhiroki.bluelineconsole.dataStore.persistent;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AliasDatabase {
    private static final String PREFS_NAME = "blc_aliases";
    private static final String KEY_ALIASES = "aliases_json";

    public static class Alias {
        public String keyword;
        public String title;
        public String target;
        public String type; // "url" or "app"
        public String icon; // optional: if null or empty and type=="app", use parent app icon; if starts with "uri:", use that image URI

        public Alias() {}
        public Alias(String keyword, String title, String target, String type) {
            this.keyword = keyword;
            this.title = title;
            this.target = target;
            this.type = type;
            this.icon = null;
        }
        public Alias(String keyword, String title, String target, String type, String icon) {
            this.keyword = keyword;
            this.title = title;
            this.target = target;
            this.type = type;
            this.icon = icon;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject o = new JSONObject();
            o.put("keyword", keyword);
            o.put("title", title);
            o.put("target", target);
            o.put("type", type);
            if (icon != null) o.put("icon", icon);
            return o;
        }

        public static Alias fromJson(JSONObject o) throws JSONException {
            Alias a = new Alias();
            a.keyword = o.getString("keyword");
            a.title = o.getString("title");
            a.target = o.getString("target");
            a.type = o.getString("type");
            a.icon = o.has("icon") ? o.getString("icon") : null;
            return a;
        }
    }

    public List<Alias> getAll(Context context) {
        String json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                             .getString(KEY_ALIASES, "[]");
        List<Alias> result = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                result.add(Alias.fromJson(arr.getJSONObject(i)));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }

    public void saveAll(Context context, List<Alias> aliases) {
        JSONArray arr = new JSONArray();
        for (Alias a : aliases) {
            try { arr.put(a.toJson()); } catch (JSONException ignored) {}
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
               .edit().putString(KEY_ALIASES, arr.toString()).apply();
    }

    public void add(Context context, Alias alias) {
        List<Alias> all = getAll(context);
        List<Alias> filtered = new ArrayList<>();
        for (Alias a : all) {
            if (!a.keyword.equals(alias.keyword)) {
                filtered.add(a);
            }
        }
        filtered.add(alias);
        saveAll(context, filtered);
    }

    public void delete(Context context, String keyword) {
        List<Alias> all = getAll(context);
        List<Alias> filtered = new ArrayList<>();
        for (Alias a : all) {
            if (!a.keyword.equals(keyword)) {
                filtered.add(a);
            }
        }
        saveAll(context, filtered);
    }
}
