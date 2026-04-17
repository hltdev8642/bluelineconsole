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

    private boolean writePreferencesToUri(Uri uri) {
        try {
            Map<String, ?> allPreferences = PreferenceManager.getDefaultSharedPreferences(this).getAll();
            JSONObject jsonObject = new JSONObject();
            for (Map.Entry<String, ?> entry : allPreferences.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof Set) {
                    JSONArray jsonArray = new JSONArray();
                    for (Object setItem : (Set<?>) value) {
                        jsonArray.put(setItem == null ? "" : setItem.toString());
                    }
                    jsonObject.put(entry.getKey(), jsonArray);
                } else {
                    jsonObject.put(entry.getKey(), value);
                }
            }

            try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
                if (outputStream == null) {
                    return false;
                }
                outputStream.write(jsonObject.toString().getBytes(StandardCharsets.UTF_8));
            }
            return true;
        } catch (Exception exception) {
            exception.printStackTrace();
            return false;
        }
    }

    private boolean importPreferencesFromUri(Uri uri) {
        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            if (inputStream == null) {
                return false;
            }

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int readBytes;
            while ((readBytes = inputStream.read(chunk)) != -1) {
                buffer.write(chunk, 0, readBytes);
            }
            if (buffer.size() == 0) {
                return false;
            }

            JSONObject jsonObject = new JSONObject(buffer.toString(StandardCharsets.UTF_8.name()));
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
            Iterator<String> keys = jsonObject.keys();

            while (keys.hasNext()) {
                String key = keys.next();
                Object value = jsonObject.get(key);
                if (value instanceof Boolean) {
                    editor.putBoolean(key, (Boolean) value);
                } else if (value instanceof Integer) {
                    editor.putInt(key, (Integer) value);
                } else if (value instanceof Long) {
                    editor.putLong(key, (Long) value);
                } else if (value instanceof Double) {
                    double numericValue = (Double) value;
                    if (numericValue == Math.rint(numericValue)
                            && numericValue >= Integer.MIN_VALUE
                            && numericValue <= Integer.MAX_VALUE) {
                        editor.putInt(key, (int) numericValue);
                    } else {
                        editor.putFloat(key, (float) numericValue);
                    }
                } else if (value instanceof JSONArray) {
                    JSONArray array = (JSONArray) value;
                    java.util.HashSet<String> stringSet = new java.util.HashSet<>();
                    for (int i = 0; i < array.length(); ++i) {
                        stringSet.add(array.getString(i));
                    }
                    editor.putStringSet(key, stringSet);
                } else {
                    editor.putString(key, value.toString());
                }
            }

            editor.apply();
            return true;
        } catch (Exception exception) {
            exception.printStackTrace();
            return false;
        }
    }

    @Override
    protected void onStop() {
        // This app should be as stateless as possible. When app disappears most activities should finish.
        super.onStop();
        if (! this._comingBack) {
            this.finish();
        }
    }
}
