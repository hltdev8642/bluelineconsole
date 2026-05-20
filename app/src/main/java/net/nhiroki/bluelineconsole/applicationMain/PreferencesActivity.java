package net.nhiroki.bluelineconsole.applicationMain;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

import net.nhiroki.bluelineconsole.R;
import net.nhiroki.bluelineconsole.commandSearchers.eachSearcher.ContactSearchCommandSearcher;
import net.nhiroki.bluelineconsole.wrapperForAndroid.ContactsReader;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class PreferencesActivity extends BaseWindowActivity {
    private static final int READ_CONTACT_PERMISSION_GRANT_REQUEST_ID = 1;
    private static final int POST_NOTIFICATIONS_PERMISSION_GRANT_REQUEST_ID = 2;
    public static final int PREF_EXPORT_REQUEST_CODE = 2001;
    public static final int PREF_IMPORT_REQUEST_CODE = 2002;

    private boolean _comingBack = false;
    private PreferencesFragmentWithOnChangeListener preferenceFragment = null;
    public PreferencesActivity() {
        super(R.layout.preferences_activity_body, false);
    }

    @Override
    public void onCreate(Bundle savedInstanceStates) {
        super.onCreate(savedInstanceStates);

        this.setHeaderFooterTexts(getString(R.string.preferences_title_for_header_and_footer), null);
        this.setWindowBoundarySize(ROOT_WINDOW_FULL_WIDTH_IN_MOBILE, 1);

        this.setWindowLocationGravity(Gravity.CENTER_VERTICAL);

        this.preferenceFragment = new PreferencesFragmentWithOnChangeListener();
        this.getSupportFragmentManager().beginTransaction().replace(R.id.main_preference_fragment, preferenceFragment).commit();

        setResult(RESULT_OK, new Intent(this, MainActivity.class));

        this.changeBaseWindowElementSizeForAnimation(false);
        this.enableBaseWindowAnimation();
    }

    @Override
    public void onRequestPermissionsResult (int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        for (int i = 0; i < permissions.length; ++i) {
            if (permissions[i].equals(Manifest.permission.READ_CONTACTS)) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    SharedPreferences.Editor prefEdit = PreferenceManager.getDefaultSharedPreferences(this).edit();
                    prefEdit.putBoolean(ContactSearchCommandSearcher.PREF_CONTACT_SEARCH_ENABLED_KEY, false);
                    prefEdit.apply();

                    ((SwitchPreference)this.preferenceFragment.findPreference(ContactSearchCommandSearcher.PREF_CONTACT_SEARCH_ENABLED_KEY)).setChecked(false);
                }
            }
            if (permissions[i].equals(Manifest.permission.POST_NOTIFICATIONS)) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    AppNotification.update(PreferencesActivity.this);

                } else {
                    SharedPreferences.Editor prefEdit = PreferenceManager.getDefaultSharedPreferences(this).edit();
                    prefEdit.putBoolean(AppNotification.PREF_KEY_ALWAYS_SHOW_NOTIFICATION, false);
                    prefEdit.apply();

                    ((SwitchPreference)this.preferenceFragment.findPreference(AppNotification.PREF_KEY_ALWAYS_SHOW_NOTIFICATION)).setChecked(false);
                }
            }
        }
    }

    protected void setComingBackFlag() {
        this._comingBack = true;
        MainActivity.setIsComingBack(true);
    }

    public static class PreferencesFragmentWithOnChangeListener extends PreferencesFragment {
        SharedPreferences.OnSharedPreferenceChangeListener preferenceChangedListener;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            preferenceChangedListener = (sharedPreferences, key) -> {
                if (key.equals(AppNotification.PREF_KEY_ALWAYS_SHOW_NOTIFICATION)) {
                    if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(PreferencesFragmentWithOnChangeListener.this.getContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        PreferencesFragmentWithOnChangeListener.this.requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},
                                POST_NOTIFICATIONS_PERMISSION_GRANT_REQUEST_ID);
                    } else {
                        AppNotification.update(PreferencesFragmentWithOnChangeListener.this.getActivity());
                    }
                }
                if (key.equals(ContactSearchCommandSearcher.PREF_CONTACT_SEARCH_ENABLED_KEY) &&
                        sharedPreferences.getBoolean(ContactSearchCommandSearcher.PREF_CONTACT_SEARCH_ENABLED_KEY, false)) {
                    if (! ContactsReader.appHasReadContactsPermission(PreferencesFragmentWithOnChangeListener.this.getContext())) {
                        PreferencesFragmentWithOnChangeListener.this.requestPermissions(new String[]{Manifest.permission.READ_CONTACTS},
                                READ_CONTACT_PERMISSION_GRANT_REQUEST_ID);
                    }
                }
            };
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(preferenceChangedListener);
        }

        @Override
        public void onPause() {
            getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(preferenceChangedListener);
            super.onPause();
        }
    }

    @Override
    public void onUserLeaveHint() {
        setResult(RESULT_OK, new Intent(this, MainActivity.class));
        super.onUserLeaveHint();
    }

    @Override
    public void onResume() {
        super.onResume();
        this._comingBack = false;
        MainActivity.setIsComingBack(false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PREF_EXPORT_REQUEST_CODE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            boolean success = writePreferencesToUri(data.getData());
            Toast.makeText(this, success ? R.string.preferences_export_success : R.string.preferences_export_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        if (requestCode == PREF_IMPORT_REQUEST_CODE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            boolean success = importPreferencesFromUri(data.getData());
            Toast.makeText(this, success ? R.string.preferences_import_success : R.string.preferences_import_failed, Toast.LENGTH_SHORT).show();
            if (success) {
                recreate();
            }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        this.changeBaseWindowElementSizeForAnimation(true);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Do not auto-finish to avoid closing the preferences stack unexpectedly when launching dialogs or sub-screens.
    }

    private static JSONObject sharedPrefsToJson(android.content.SharedPreferences sp) throws Exception {
        JSONObject jo = new JSONObject();
        Map<String, ?> all = sp.getAll();
        if (all == null) return jo;
        for (Map.Entry<String, ?> entry : all.entrySet()) {
            Object v = entry.getValue();
            if (v instanceof Set) {
                JSONArray arr = new JSONArray();
                for (Object item : (Set<?>) v) arr.put(item == null ? "" : item.toString());
                jo.put(entry.getKey(), arr);
            } else {
                jo.put(entry.getKey(), v);
            }
        }
        return jo;
    }

    private boolean writePreferencesToUri(Uri uri) {
        try {
            JSONObject root = new JSONObject();

            // Default shared preferences (package_name + "_preferences")
            String defaultPrefsName = getPackageName() + "_preferences";
            android.content.SharedPreferences defaultPrefs =
                    androidx.preference.PreferenceManager.getDefaultSharedPreferences(this);
            root.put(defaultPrefsName, sharedPrefsToJson(defaultPrefs));

            // All other named shared_prefs files
            java.io.File spDir = new java.io.File(getApplicationInfo().dataDir, "shared_prefs");
            if (spDir.exists() && spDir.isDirectory()) {
                java.io.File[] files = spDir.listFiles();
                if (files != null) {
                    for (java.io.File f : files) {
                        String fname = f.getName();
                        if (!fname.endsWith(".xml")) continue;
                        String name = fname.substring(0, fname.length() - 4);
                        if (root.has(name)) continue;
                        try {
                            android.content.SharedPreferences sp = getSharedPreferences(name, MODE_PRIVATE);
                            root.put(name, sharedPrefsToJson(sp));
                        } catch (Exception ignored) {}
                    }
                }
            }

            // Explicit alias backup (ensures aliases survive even if blc_aliases prefs file is missed)
            net.nhiroki.bluelineconsole.dataStore.persistent.AliasDatabase adb =
                    new net.nhiroki.bluelineconsole.dataStore.persistent.AliasDatabase();
            java.util.List<net.nhiroki.bluelineconsole.dataStore.persistent.AliasDatabase.Alias> aliases =
                    adb.getAll(this);
            JSONArray aliasArr = new JSONArray();
            for (net.nhiroki.bluelineconsole.dataStore.persistent.AliasDatabase.Alias a : aliases) {
                try { aliasArr.put(a.toJson()); } catch (Exception ignored) {}
            }
            root.put("aliases_backup", aliasArr);

            byte[] bytes = root.toString(2).getBytes(StandardCharsets.UTF_8);
            try (OutputStream out = getContentResolver().openOutputStream(uri, "wt")) {
                if (out == null) return false;
                out.write(bytes);
                out.flush();
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void applyJsonToPrefs(JSONObject jo, SharedPreferences.Editor editor) throws Exception {
        Iterator<String> keys = jo.keys();
        while (keys.hasNext()) {
            String k = keys.next();
            Object v = jo.get(k);
            if (v instanceof Boolean) editor.putBoolean(k, (Boolean) v);
            else if (v instanceof Integer) editor.putInt(k, (Integer) v);
            else if (v instanceof Long) editor.putLong(k, (Long) v);
            else if (v instanceof Double) {
                double d = (Double) v;
                if (d == Math.rint(d) && d >= Integer.MIN_VALUE && d <= Integer.MAX_VALUE)
                    editor.putInt(k, (int) d);
                else
                    editor.putFloat(k, (float) d);
            } else if (v instanceof JSONArray) {
                JSONArray arr = (JSONArray) v;
                java.util.HashSet<String> set = new java.util.HashSet<>();
                for (int i = 0; i < arr.length(); i++) set.add(arr.getString(i));
                editor.putStringSet(k, set);
            } else {
                editor.putString(k, v.toString());
            }
        }
        editor.apply();
    }

    private boolean importPreferencesFromUri(Uri uri) {
        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            if (inputStream == null) return false;

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int n;
            while ((n = inputStream.read(chunk)) != -1) buffer.write(chunk, 0, n);
            if (buffer.size() == 0) return false;

            JSONObject root = new JSONObject(buffer.toString(StandardCharsets.UTF_8.name()));
            String defaultPrefsName = getPackageName() + "_preferences";

            Iterator<String> keys = root.keys();
            while (keys.hasNext()) {
                String name = keys.next();
                Object obj = root.get(name);

                if ("aliases_backup".equals(name) && obj instanceof JSONArray) {
                    JSONArray ja = (JSONArray) obj;
                    java.util.List<net.nhiroki.bluelineconsole.dataStore.persistent.AliasDatabase.Alias> aliases =
                            new java.util.ArrayList<>();
                    for (int i = 0; i < ja.length(); i++)
                        aliases.add(net.nhiroki.bluelineconsole.dataStore.persistent.AliasDatabase.Alias.fromJson(ja.getJSONObject(i)));
                    new net.nhiroki.bluelineconsole.dataStore.persistent.AliasDatabase().saveAll(this, aliases);
                    continue;
                }

                // Old format compat: "default_preferences" key → write to actual default prefs file
                if ("default_preferences".equals(name) && obj instanceof JSONObject) {
                    SharedPreferences.Editor ed = getSharedPreferences(defaultPrefsName, MODE_PRIVATE).edit();
                    applyJsonToPrefs((JSONObject) obj, ed);
                    continue;
                }

                if (obj instanceof JSONObject) {
                    SharedPreferences.Editor ed = getSharedPreferences(name, MODE_PRIVATE).edit();
                    applyJsonToPrefs((JSONObject) obj, ed);
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}

