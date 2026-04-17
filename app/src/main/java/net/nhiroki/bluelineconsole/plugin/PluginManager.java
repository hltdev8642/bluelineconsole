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
}
