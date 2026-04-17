package net.nhiroki.bluelineconsole.commandSearchers.eachSearcher;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

import net.nhiroki.bluelineconsole.applicationMain.MainActivity;
import net.nhiroki.bluelineconsole.interfaces.CandidateEntry;
import net.nhiroki.bluelineconsole.interfaces.CommandSearcher;
import net.nhiroki.bluelineconsole.interfaces.EventLauncher;
import net.nhiroki.bluelineconsole.plugin.PluginDefinition;
import net.nhiroki.bluelineconsole.plugin.PluginManager;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PluginCommandSearcher implements CommandSearcher {
    private final PluginManager pluginManager;
    private volatile boolean prepared = false;

    public PluginCommandSearcher(Context context) {
        this.pluginManager = new PluginManager(context);
        this.prepared = true;
    }

    @Override
    public void refresh(Context context) {
        this.pluginManager.refresh();
        this.prepared = true;
    }

    @Override
    public void close() {
    }

    @Override
    public boolean isPrepared() {
        return prepared;
    }

    @Override
    public void waitUntilPrepared() {
    }

    @Override
    public List<CandidateEntry> searchCandidateEntries(String query, Context context) {
        List<CandidateEntry> results = new ArrayList<>();
        String safeQuery = query == null ? "" : query;

        for (PluginDefinition definition : pluginManager.getPlugins()) {
            if (definition.sourceFileName != null
                    && !PreferenceManager.getDefaultSharedPreferences(context).getBoolean("plugin_enabled_" + definition.sourceFileName, false)) {
                continue;
            }

            for (PluginDefinition.PatternDef pattern : definition.patterns) {
                String parameter = null;
                boolean matched = false;

                switch (pattern.type) {
                    case "prefix":
                        if (safeQuery.startsWith(pattern.value)) {
                            matched = true;
                            parameter = safeQuery.substring(pattern.value.length()).trim();
                        }
                        break;
                    case "contains":
                        if (safeQuery.contains(pattern.value)) {
                            matched = true;
                            parameter = safeQuery;
                        }
                        break;
                    case "regex":
                        try {
                            Matcher matcher = Pattern.compile(pattern.value).matcher(safeQuery);
                            if (matcher.find()) {
                                matched = true;
                                parameter = matcher.groupCount() >= 1 ? matcher.group(1) : safeQuery;
                            }
                        } catch (Exception exception) {
                            exception.printStackTrace();
                        }
                        break;
                    case "always":
                        matched = true;
                        parameter = safeQuery;
                        break;
                    default:
                        break;
                }

                if (matched) {
                    String title = definition.displayName;
                    if (parameter != null && !parameter.isEmpty()) {
                        title += ": " + parameter;
                    }
                    results.add(new PluginCandidateEntry(title, createLauncher(definition, parameter)));
                    break;
                }
            }
        }

        return results;
    }

    private EventLauncher createLauncher(PluginDefinition definition, String parameter) {
        if (definition.action == null) {
            return null;
        }

        switch (definition.action.type) {
            case "url":
                return activity -> {
                    try {
                        String url = definition.action.template.replace("{query}", Uri.encode(parameter == null ? "" : parameter));
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        activity.startActivity(intent);
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                };
            case "copy":
                return activity -> {
                    ClipboardManager clipboardManager = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clipboardManager != null) {
                        String text = definition.action.template.replace("{query}", parameter == null ? "" : parameter);
                        clipboardManager.setPrimaryClip(ClipData.newPlainText(definition.displayName, text));
                    }
                };
            case "text":
            default:
                return activity -> {
                    String text = definition.action.template.replace("{query}", parameter == null ? "" : parameter);
                    Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
                };
        }
    }

    private static class PluginCandidateEntry implements CandidateEntry {
        private final String title;
        private final EventLauncher launcher;

        private PluginCandidateEntry(String title, EventLauncher launcher) {
            this.title = title;
            this.launcher = launcher;
        }

        @Override
        public String getTitle() {
            return title;
        }

        @Override
        public View getView(MainActivity mainActivity) {
            return null;
        }

        @Override
        public boolean hasLongView() {
            return false;
        }

        @Override
        public EventLauncher getEventLauncher(Context context) {
            return launcher;
        }

        @Override
        public android.graphics.drawable.Drawable getIcon(Context context) {
            return null;
        }

        @Override
        public boolean hasEvent() {
            return launcher != null;
        }

        @Override
        public boolean isSubItem() {
            return false;
        }

        @Override
        public boolean viewIsRecyclable() {
            return true;
        }
    }
}
