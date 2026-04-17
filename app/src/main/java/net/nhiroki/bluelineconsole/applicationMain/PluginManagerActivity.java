package net.nhiroki.bluelineconsole.applicationMain;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

import net.nhiroki.bluelineconsole.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class PluginManagerActivity extends BaseWindowActivity {
    private static final int PLUGIN_IMPORT_REQUEST_CODE = 3001;

    private ListView pluginListView;
    private final List<String> pluginFiles = new ArrayList<>();

    public PluginManagerActivity() {
        super(R.layout.plugin_manager_activity_body, false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHeaderFooterTexts(getString(R.string.plugin_manager_title), null);
        setWindowBoundarySize(ROOT_WINDOW_FULL_WIDTH_IN_MOBILE, 1);
        setWindowLocationGravity(Gravity.CENTER_VERTICAL);

        pluginListView = findViewById(R.id.pluginListView);
        Button importButton = findViewById(R.id.importPluginButton);

        importButton.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            try {
                startActivityForResult(intent, PLUGIN_IMPORT_REQUEST_CODE);
            } catch (Exception exception) {
                exception.printStackTrace();
                Toast.makeText(this, R.string.plugin_manager_file_picker_unavailable, Toast.LENGTH_SHORT).show();
            }
        });

        pluginListView.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= pluginFiles.size()) {
                return;
            }

            String fileName = pluginFiles.get(position);
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            String preferenceKey = "plugin_enabled_" + fileName;
            boolean enabled = preferences.getBoolean(preferenceKey, false);
            preferences.edit().putBoolean(preferenceKey, !enabled).apply();
            Toast.makeText(
                    this,
                    getString(enabled ? R.string.plugin_manager_disabled : R.string.plugin_manager_enabled, fileName),
                    Toast.LENGTH_SHORT
            ).show();
            refreshList();
        });

        pluginListView.setOnItemLongClickListener((parent, view, position, id) -> {
            if (position >= pluginFiles.size()) {
                return false;
            }

            String fileName = pluginFiles.get(position);
            new AlertDialog.Builder(this)
                    .setTitle(R.string.button_delete)
                    .setMessage(getString(R.string.preferences_item_plugin_delete_confirm, fileName))
                    .setPositiveButton(R.string.button_delete, (dialog, which) -> {
                        File pluginFile = new File(getPluginDirectory(), fileName);
                        if (pluginFile.exists() && pluginFile.delete()) {
                            PreferenceManager.getDefaultSharedPreferences(this).edit().remove("plugin_enabled_" + fileName).apply();
                            Toast.makeText(this, getString(R.string.preferences_item_plugin_deleted, fileName), Toast.LENGTH_SHORT).show();
                            refreshList();
                        }
                    })
                    .setNegativeButton(R.string.button_close, null)
                    .show();
            return true;
        });

        refreshList();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != PLUGIN_IMPORT_REQUEST_CODE || resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }

        Uri uri = data.getData();
        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            if (inputStream == null) {
                throw new IllegalStateException("Cannot open input stream for plugin import");
            }

            String fileName = resolveDisplayName(uri);
            if (fileName == null) {
                fileName = "imported_plugin_" + System.currentTimeMillis() + ".json";
            }

            File outputFile = new File(getPluginDirectory(), fileName);
            try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
                byte[] buffer = new byte[4096];
                int readBytes;
                while ((readBytes = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, readBytes);
                }
            }

            Toast.makeText(this, getString(R.string.plugin_manager_import_success, outputFile.getName()), Toast.LENGTH_SHORT).show();
            refreshList();
        } catch (Exception exception) {
            exception.printStackTrace();
            Toast.makeText(this, R.string.plugin_manager_import_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void refreshList() {
        File[] files = getPluginDirectory().listFiles();
        pluginFiles.clear();

        List<String> displayValues = new ArrayList<>();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (files != null) {
            for (File file : files) {
                if (!file.isFile()) {
                    continue;
                }
                String name = file.getName();
                pluginFiles.add(name);
                boolean enabled = preferences.getBoolean("plugin_enabled_" + name, false);
                displayValues.add(enabled ? name + " (" + getString(R.string.plugin_manager_toggle_enabled) + ")" : name);
            }
        }

        if (pluginFiles.isEmpty()) {
            displayValues.add(getString(R.string.plugin_manager_no_plugins));
        }

        pluginListView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, displayValues));
    }

    private File getPluginDirectory() {
        File pluginDirectory = new File(getFilesDir(), "plugins");
        if (!pluginDirectory.exists()) {
            pluginDirectory.mkdirs();
        }
        return pluginDirectory;
    }

    private String resolveDisplayName(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor == null) {
            return null;
        }

        try {
            int displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (displayNameIndex != -1 && cursor.moveToFirst()) {
                return cursor.getString(displayNameIndex);
            }
            return null;
        } finally {
            cursor.close();
        }
    }
}
