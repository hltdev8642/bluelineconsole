package net.nhiroki.bluelineconsole.plugin;

import android.content.Context;

/**
 * Deprecated marketplace stub removed to reduce footprint.
 * Kept as a minimal stub for compatibility; no network or file operations.
 */
@Deprecated
public final class PluginMarketplace {
    public PluginMarketplace(Context context) {
        // no-op
    }

    public java.util.List<String> listAvailablePlugins() { return java.util.Collections.emptyList(); }

    public boolean downloadPluginToLocal(String urlString, String destFileName) { return false; }
}
