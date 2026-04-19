package net.nhiroki.bluelineconsole.plugin;

import android.content.Context;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple plugin marketplace stub: can fetch a plugin JSON from URL and save into plugin directory.
 * This is intentionally minimal and synchronous; callers should run it off the UI thread.
 */
public class PluginMarketplace {
    private final File pluginDir;

    public PluginMarketplace(Context context) {
        this.pluginDir = new File(context.getApplicationContext().getFilesDir(), "plugins");
        if (!this.pluginDir.exists()) this.pluginDir.mkdirs();
    }

    public List<String> listAvailablePlugins() {
        // TODO: Implement real marketplace discovery
        return new ArrayList<>();
    }

    public boolean downloadPluginToLocal(String urlString, String destFileName) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection)url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            conn.setRequestMethod("GET");
            conn.connect();

            InputStream in = conn.getInputStream();
            File outFile = new File(pluginDir, destFileName);
            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                byte[] buf = new byte[4096];
                int r;
                while ((r = in.read(buf)) != -1) {
                    fos.write(buf, 0, r);
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
