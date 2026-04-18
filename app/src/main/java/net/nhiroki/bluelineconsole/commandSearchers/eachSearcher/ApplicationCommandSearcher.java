package net.nhiroki.bluelineconsole.commandSearchers.eachSearcher;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import net.nhiroki.bluelineconsole.R;
import net.nhiroki.bluelineconsole.applicationMain.MainActivity;
import net.nhiroki.bluelineconsole.applicationMain.UsageTracker;
import net.nhiroki.bluelineconsole.commandSearchers.lib.StringMatchStrategy;
import net.nhiroki.bluelineconsole.commands.applications.ApplicationDatabase;
import net.nhiroki.bluelineconsole.dataStore.cache.ApplicationInformation;
import net.nhiroki.bluelineconsole.interfaces.CandidateEntry;
import net.nhiroki.bluelineconsole.interfaces.CommandSearcher;
import net.nhiroki.bluelineconsole.interfaces.ContextAction;
import net.nhiroki.bluelineconsole.interfaces.ContextActionProvider;
import net.nhiroki.bluelineconsole.interfaces.EventLauncher;
import net.nhiroki.bluelineconsole.interfaces.UsageTrackable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ApplicationCommandSearcher implements CommandSearcher {
    private ApplicationDatabase applicationDatabase;

    public ApplicationCommandSearcher() {
    }

    @Override
    public void refresh(Context context) {
        this.applicationDatabase = new ApplicationDatabase(context);
    }

    @Override
    public void close() {
        this.applicationDatabase.close();
    }

    @Override
    public boolean isPrepared() {
        return this.applicationDatabase.isPrepared();
    }

    @Override
    public void waitUntilPrepared() {
        this.applicationDatabase.waitUntilPrepared();
    }

    @Override
    @NonNull
    public List<CandidateEntry> searchCandidateEntries(String query, Context context) {
        List<CandidateEntry> candidates = new ArrayList<>();

        final boolean matchAllApplications = query.equalsIgnoreCase("all_apps");

        List<Pair<Integer, CandidateEntry>> appCandidates = new ArrayList<>();
        for (ApplicationInformation applicationInformation : applicationDatabase.getApplicationInformationList()) {
            final String appLabel = applicationInformation.getLabel();
            final ApplicationInfo androidApplicationInfo = applicationDatabase.getAndroidApplicationInfo(applicationInformation.getPackageName());
            final String pkgName = applicationInformation.getPackageName();

            if (matchAllApplications) {
                appCandidates.add(new Pair<>(0, new AppOpenCandidateEntry(context, applicationInformation, androidApplicationInfo, appLabel)));
                continue;
            }

            int appLabelMatchResult = StringMatchStrategy.match(context, query, appLabel, false);
            if (appLabelMatchResult != -1) {
                int usageCount = UsageTracker.getUsageCount(context, pkgName);
                int adjustedScore = appLabelMatchResult * 100 - Math.min(usageCount * 3, 50);
                appCandidates.add(new Pair<>(adjustedScore, new AppOpenCandidateEntry(context, applicationInformation, androidApplicationInfo, appLabel)));
                continue;
            }

            int packageNameMatchResult = StringMatchStrategy.match(context, query, pkgName, false);
            if (packageNameMatchResult != -1) {
                int usageCount = UsageTracker.getUsageCount(context, pkgName);
                int adjustedScore = (100000 + packageNameMatchResult) * 100 - Math.min(usageCount * 3, 50);
                appCandidates.add(new Pair<>(adjustedScore, new AppOpenCandidateEntry(context, applicationInformation, androidApplicationInfo, appLabel)));
                //noinspection UnnecessaryContinue
                continue;
            }
        }

        Collections.sort(appCandidates, (o1, o2) -> o1.first.compareTo(o2.first));

        for (Pair<Integer, CandidateEntry> entry : appCandidates) {
            candidates.add(entry.second);
            String pkgName = ((AppOpenCandidateEntry) entry.second).applicationInformation.getPackageName();
            candidates.addAll(getShortcutsForApp(context, pkgName));
        }

        return candidates;
    }

    public List<CandidateEntry> getRecentEntries(Context context, int limit) {
        if (applicationDatabase == null || !applicationDatabase.isPrepared()) {
            return Collections.emptyList();
        }
        List<String> recentKeys = UsageTracker.getRecentKeys(context, limit);
        List<CandidateEntry> result = new ArrayList<>();
        for (String pkgName : recentKeys) {
            for (ApplicationInformation appInfo : applicationDatabase.getApplicationInformationList()) {
                if (appInfo.getPackageName().equals(pkgName)) {
                    ApplicationInfo androidInfo = applicationDatabase.getAndroidApplicationInfo(pkgName);
                    result.add(new AppOpenCandidateEntry(context, appInfo, androidInfo, appInfo.getLabel()));
                    break;
                }
            }
        }
        return result;
    }

    @SuppressWarnings("MissingPermission")
    private List<CandidateEntry> getShortcutsForApp(Context context, String packageName) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return Collections.emptyList();
        try {
            android.content.pm.LauncherApps launcherApps =
                (android.content.pm.LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
            if (launcherApps == null || !launcherApps.hasShortcutHostPermission()) return Collections.emptyList();

            android.content.pm.LauncherApps.ShortcutQuery query = new android.content.pm.LauncherApps.ShortcutQuery();
            query.setQueryFlags(android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST |
                                android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC);
            query.setPackage(packageName);

            List<android.content.pm.ShortcutInfo> shortcuts =
                launcherApps.getShortcuts(query, android.os.Process.myUserHandle());
            if (shortcuts == null) return Collections.emptyList();

            List<CandidateEntry> entries = new ArrayList<>();
            int count = 0;
            for (android.content.pm.ShortcutInfo si : shortcuts) {
                if (count++ >= 3) break;
                entries.add(new AppShortcutCandidateEntry(si));
            }
            return entries;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private static class AppOpenCandidateEntry implements CandidateEntry, UsageTrackable, ContextActionProvider {
        private final ApplicationInformation applicationInformation;
        private final ApplicationInfo androidApplicationInfo;
        private final String title;
        private final boolean displayPackageName;

        // Getting app title in Android is slow, so app title also should be given via constructor from cache.
        AppOpenCandidateEntry(Context context, ApplicationInformation applicationInformation, ApplicationInfo androidApplicationInfo, String appTitle) {
            this.applicationInformation = applicationInformation;
            this.androidApplicationInfo = androidApplicationInfo;
            this.title = appTitle;
            this.displayPackageName = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("pref_apps_show_package_name", false);
        }

        @Override
        @NonNull
        public String getTitle() {
            return title;
        }

        @Override
        public View getView(MainActivity mainActivity) {
            if(!displayPackageName) {
                return null;
            }

            String packageName = AppOpenCandidateEntry.this.applicationInformation.getPackageName();
            TextView packageNameView = new TextView(mainActivity);
            packageNameView.setText(packageName);
            packageNameView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            return packageNameView;
        }

        @Override
        public boolean hasEvent() {
            return true;
        }

        @Override
        public EventLauncher getEventLauncher(final Context context) {
            return activity -> {
                String packageName = AppOpenCandidateEntry.this.applicationInformation.getPackageName();
                Intent intent = activity.getPackageManager().getLaunchIntentForPackage(AppOpenCandidateEntry.this.applicationInformation.getPackageName());
                if (packageName.equals(context.getPackageName())) {
                    // special case that happens to some curious behavior in home app
                    activity.finishIfNotHome();
                    activity.startActivity(new Intent(activity, MainActivity.class));
                    return;
                }
                if (intent == null) {
                    Toast.makeText(activity, String.format(activity.getString(R.string.error_failure_not_found_opening_application_with_class), packageName), Toast.LENGTH_LONG).show();
                    return;
                }
                activity.startActivity(intent);
                activity.finishIfNotHome();
            };
        }

        @Override
        public boolean hasLongView() {
            return false;
        }

        @Override
        public Drawable getIcon(Context context) {
            return context.getPackageManager().getApplicationIcon(androidApplicationInfo);
        }

        @Override
        public boolean isSubItem() {
            return false;
        }

        @Override
        public boolean viewIsRecyclable() {
            return true;
        }

        @Override
        public String getUsageKey() {
            return this.applicationInformation.getPackageName();
        }

        @Override
        public List<ContextAction> getContextActions(Context context) {
            List<ContextAction> actions = new ArrayList<>();
            String packageName = this.applicationInformation.getPackageName();

            actions.add(new ContextAction(context.getString(R.string.result_action_app_info), activity -> {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + packageName));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(intent);
            }));

            actions.add(new ContextAction(context.getString(R.string.result_action_uninstall), activity -> {
                Intent intent = new Intent(Intent.ACTION_DELETE);
                intent.setData(Uri.parse("package:" + packageName));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(intent);
            }));

            return actions;
        }
    }

    @android.annotation.TargetApi(Build.VERSION_CODES.N_MR1)
    private static class AppShortcutCandidateEntry implements CandidateEntry {
        private final android.content.pm.ShortcutInfo shortcutInfo;

        AppShortcutCandidateEntry(android.content.pm.ShortcutInfo info) {
            this.shortcutInfo = info;
        }

        @Override
        public String getTitle() {
            CharSequence label = shortcutInfo.getShortLabel();
            return label != null ? label.toString() : shortcutInfo.getId();
        }

        @Override public View getView(MainActivity a) { return null; }
        @Override public boolean hasLongView() { return false; }
        @Override public boolean hasEvent() { return true; }
        @Override public boolean isSubItem() { return true; }
        @Override public boolean viewIsRecyclable() { return true; }

        @Override
        public Drawable getIcon(Context context) {
            try {
                android.content.pm.LauncherApps launcherApps =
                    (android.content.pm.LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
                if (launcherApps != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    return launcherApps.getShortcutBadgedIconDrawable(shortcutInfo, 0);
                }
            } catch (Exception ignored) {}
            return null;
        }

        @Override
        public EventLauncher getEventLauncher(Context context) {
            return activity -> {
                try {
                    android.content.pm.LauncherApps launcherApps =
                        (android.content.pm.LauncherApps) activity.getSystemService(Context.LAUNCHER_APPS_SERVICE);
                    if (launcherApps != null) {
                        launcherApps.startShortcut(shortcutInfo, null, null);
                        activity.finishIfNotHome();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            };
        }
    }
}
