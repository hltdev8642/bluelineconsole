package net.nhiroki.bluelineconsole.commandSearchers.eachSearcher;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.View;

import androidx.annotation.NonNull;

import net.nhiroki.bluelineconsole.applicationMain.MainActivity;
import net.nhiroki.bluelineconsole.commandSearchers.lib.StringMatchStrategy;
import net.nhiroki.bluelineconsole.dataStore.persistent.AliasDatabase;
import net.nhiroki.bluelineconsole.interfaces.CandidateEntry;
import net.nhiroki.bluelineconsole.interfaces.CommandSearcher;
import net.nhiroki.bluelineconsole.interfaces.EventLauncher;
import net.nhiroki.bluelineconsole.interfaces.UsageTrackable;

import java.util.ArrayList;
import java.util.List;

public class AliasCommandSearcher implements CommandSearcher {
    private final AliasDatabase aliasDatabase = new AliasDatabase();

    @Override public void refresh(Context context) {}
    @Override public void close() {}
    @Override public boolean isPrepared() { return true; }
    @Override public void waitUntilPrepared() {}

    @Override
    @NonNull
    public List<CandidateEntry> searchCandidateEntries(String query, Context context) {
        List<CandidateEntry> candidates = new ArrayList<>();
        if (query.isEmpty()) return candidates;

        for (AliasDatabase.Alias alias : aliasDatabase.getAll(context)) {
            boolean keywordMatch = StringMatchStrategy.match(context, query, alias.keyword, false) != -1;
            boolean titleMatch = StringMatchStrategy.match(context, query, alias.title, false) != -1;
            if (keywordMatch || titleMatch) {
                candidates.add(new AliasCandidateEntry(alias));
            }
        }
        return candidates;
    }

    private static class AliasCandidateEntry implements CandidateEntry, UsageTrackable {
        private final AliasDatabase.Alias alias;

        AliasCandidateEntry(AliasDatabase.Alias alias) { this.alias = alias; }

        @Override public String getTitle() { return alias.title + " [" + alias.keyword + "]"; }
        @Override public View getView(MainActivity a) { return null; }
        @Override public boolean hasLongView() { return false; }
        @Override public boolean hasEvent() { return true; }
        @Override public Drawable getIcon(Context c) { return null; }
        @Override public boolean isSubItem() { return false; }
        @Override public boolean viewIsRecyclable() { return true; }
        @Override public String getUsageKey() { return "alias_" + alias.keyword; }

        @Override
        public EventLauncher getEventLauncher(Context context) {
            return activity -> {
                Intent intent;
                if ("url".equals(alias.type)) {
                    intent = new Intent(Intent.ACTION_VIEW, Uri.parse(alias.target));
                } else {
                    intent = activity.getPackageManager().getLaunchIntentForPackage(alias.target);
                    if (intent == null) return;
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(intent);
                activity.finishIfNotHome();
            };
        }
    }
}
