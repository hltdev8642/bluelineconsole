package net.nhiroki.bluelineconsole.plugin;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class IntentSpec {
    public String action;
    public String dataUri;
    public String mimeType;
    public List<String> flags = new ArrayList<>();
    public List<Extra> extras = new ArrayList<>();
    public List<String> categories = new ArrayList<>();
    public String componentPackage; // optional package/class like package/class

    public static class Extra {
        public String key;
        public String type; // string, int, boolean
        public String value;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("action", action == null ? JSONObject.NULL : action);
        o.put("dataUri", dataUri == null ? JSONObject.NULL : dataUri);
        o.put("mimeType", mimeType == null ? JSONObject.NULL : mimeType);
        JSONArray fa = new JSONArray();
        for (String f: flags) fa.put(f);
        o.put("flags", fa);
        JSONArray ex = new JSONArray();
        for (Extra e: extras) {
            JSONObject eo = new JSONObject();
            eo.put("key", e.key);
            eo.put("type", e.type);
            eo.put("value", e.value);
            ex.put(eo);
        }
        o.put("extras", ex);
        JSONArray ca = new JSONArray();
        for (String c: categories) ca.put(c);
        o.put("categories", ca);
        o.put("componentPackage", componentPackage == null ? JSONObject.NULL : componentPackage);
        return o;
    }

    public static IntentSpec fromJson(String s) throws JSONException {
        if (s == null) return null;
        JSONObject o = new JSONObject(s);
        IntentSpec spec = new IntentSpec();
        spec.action = o.has("action") && !o.isNull("action") ? o.getString("action") : null;
        spec.dataUri = o.has("dataUri") && !o.isNull("dataUri") ? o.getString("dataUri") : null;
        spec.mimeType = o.has("mimeType") && !o.isNull("mimeType") ? o.getString("mimeType") : null;
        if (o.has("flags")) {
            JSONArray fa = o.getJSONArray("flags");
            for (int i = 0; i < fa.length(); i++) spec.flags.add(fa.getString(i));
        }
        if (o.has("extras")) {
            JSONArray ex = o.getJSONArray("extras");
            for (int i = 0; i < ex.length(); i++) {
                JSONObject eo = ex.getJSONObject(i);
                Extra e = new Extra();
                e.key = eo.getString("key");
                e.type = eo.getString("type");
                e.value = eo.getString("value");
                spec.extras.add(e);
            }
        }
        if (o.has("categories")) {
            JSONArray ca = o.getJSONArray("categories");
            for (int i = 0; i < ca.length(); i++) spec.categories.add(ca.getString(i));
        }
        spec.componentPackage = o.has("componentPackage") && !o.isNull("componentPackage") ? o.getString("componentPackage") : null;
        return spec;
    }

    public Intent toIntent(android.content.Context context, String queryReplacement) {
        Intent intent = new Intent();
        if (action != null) intent.setAction(replaceQuery(action, queryReplacement));
        if (dataUri != null && !dataUri.isEmpty()) {
            String data = replaceQuery(dataUri, queryReplacement);
            try { intent.setData(Uri.parse(data)); } catch (Exception ignored) {}
        }
        if (mimeType != null && !mimeType.isEmpty()) intent.setType(replaceQuery(mimeType, queryReplacement));

        for (String f: flags) {
            try {
                int flagVal = Integer.parseInt(f);
                intent.addFlags(flagVal);
            } catch (NumberFormatException e) {
                // support symbolic names like FLAG_ACTIVITY_NEW_TASK by reflection
                try {
                    java.lang.reflect.Field fld = Intent.class.getField(f);
                    int v = fld.getInt(null);
                    intent.addFlags(v);
                } catch (Exception ignored) {}
            }
        }

        Bundle b = new Bundle();
        for (Extra e: extras) {
            String v = replaceQuery(e.value, queryReplacement);
            switch (e.type) {
                case "int":
                    try { b.putInt(e.key, Integer.parseInt(v)); } catch (NumberFormatException ignored) {}
                    break;
                case "bool":
                case "boolean":
                    b.putBoolean(e.key, Boolean.parseBoolean(v));
                    break;
                default:
                    b.putString(e.key, v);
                    break;
            }
        }
        if (!b.isEmpty()) intent.putExtras(b);

        if (categories != null && !categories.isEmpty()) {
            for (String c: categories) {
                try { intent.addCategory(replaceQuery(c, queryReplacement)); } catch (Exception ignored) {}
            }
        }

        if (componentPackage != null && componentPackage.contains("/")) {
            String[] parts = componentPackage.split("/");
            intent.setClassName(parts[0], parts[1]);
        }

        return intent;
    }

    private String replaceQuery(String template, String queryReplacement) {
        if (template == null) return null;
        if (queryReplacement == null) queryReplacement = "";
        return template.replace("{query}", queryReplacement);
    }
}
