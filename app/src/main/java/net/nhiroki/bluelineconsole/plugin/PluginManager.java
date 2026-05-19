package net.nhiroki.bluelineconsole.plugin;

import android.content.Context;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PluginManager {
    private final File pluginDir;
    private final List<PluginDefinition> plugins = new ArrayList<>();

    public PluginManager(Context context) {
        Context applicationContext = context.getApplicationContext();
        this.pluginDir = new File(applicationContext.getFilesDir(), "plugins");
        if (!this.pluginDir.exists()) {
            this.pluginDir.mkdirs();
        }
        refresh();
    }

    public synchronized void refresh() {
        plugins.clear();
        File[] files = pluginDir.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (!file.isFile()) {
                continue;
            }
            try {
                PluginDefinition definition = PluginDefinition.fromFile(file);
                if (definition != null) {
                    plugins.add(definition);
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }

    public synchronized List<PluginDefinition> getPlugins() {
        return new ArrayList<>(plugins);
    }

    public synchronized boolean installPluginFile(File src) {
        try {
            File dst = new File(pluginDir, src.getName());
            java.io.FileInputStream in = new java.io.FileInputStream(src);
            java.io.FileOutputStream out = new java.io.FileOutputStream(dst);
            byte[] buf = new byte[4096];
            int r;
            while ((r = in.read(buf)) != -1) {
                out.write(buf, 0, r);
            }
            in.close();
            out.close();
            refresh();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public synchronized boolean deletePlugin(String sourceFileName) {
        try {
            File f = new File(pluginDir, sourceFileName);
            boolean r = f.exists() && f.delete();
            refresh();
            return r;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public synchronized File getPluginFile(PluginDefinition def) {
        if (def == null || def.sourceFileName == null) return null;
        return new File(pluginDir, def.sourceFileName);
    }

    public void setPluginEnabled(android.content.Context context, String sourceFileName, boolean enabled) {
        if (sourceFileName == null) return;
        android.content.SharedPreferences sp = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean("plugin_enabled_" + sourceFileName, enabled).apply();
        refresh();
    }

    public boolean isPluginEnabled(android.content.Context context, String sourceFileName) {
        if (sourceFileName == null) return false;
        android.content.SharedPreferences sp = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean("plugin_enabled_" + sourceFileName, false);
    }
}
