package net.nhiroki.bluelineconsole.applicationMain;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import net.nhiroki.bluelineconsole.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AliasAppPickerActivity extends BaseWindowActivity {

    public static final String EXTRA_COMPONENT = "component";

    private static class AppEntry {
        final String label;
        final String packageName;
        AppEntry(String label, String packageName) {
            this.label = label;
            this.packageName = packageName;
        }
    }

    private static class ActivityEntry {
        final String label;
        final String packageName;
        final String className;
        ActivityEntry(String label, String packageName, String className) {
            this.label = label;
            this.packageName = packageName;
            this.className = className;
        }
    }

    private final List<AppEntry> appList = new ArrayList<>();
    private ListView listView;
    private Button backButton;

    public AliasAppPickerActivity() {
        super(R.layout.preferences_alias_app_picker_body, false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHeaderFooterTexts(getString(R.string.alias_picker_title), null);
        setWindowBoundarySize(ROOT_WINDOW_FULL_WIDTH_IN_MOBILE, 2);

        listView = findViewById(R.id.appPickerListView);
        backButton = findViewById(R.id.appPickerBackButton);
        backButton.setOnClickListener(v -> showAppList());

        loadApps();
        showAppList();
    }

    private void loadApps() {
        try {
            PackageManager pm = getPackageManager();
            Intent query = new Intent(Intent.ACTION_MAIN);
            query.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> resolved = pm.queryIntentActivities(query, 0);
            appList.clear();
            if (resolved != null) {
                for (ResolveInfo ri : resolved) {
                    CharSequence lbl = ri.loadLabel(pm);
                    String label = lbl != null ? lbl.toString() : ri.activityInfo.packageName;
                    String pkg = ri.activityInfo != null ? ri.activityInfo.packageName : "";
                    appList.add(new AppEntry(label, pkg));
                }
            }
            Collections.sort(appList, (a, b) -> a.label.compareToIgnoreCase(b.label));
        } catch (Exception e) {
            e.printStackTrace();
            // Show a simple fallback message
            appList.clear();
            appList.add(new AppEntry(getString(R.string.alias_picker_error_no_apps), ""));
        }
    }

    private void showAppList() {
        backButton.setVisibility(View.GONE);
        ArrayAdapter<AppEntry> adapter = new ArrayAdapter<AppEntry>(this,
                android.R.layout.simple_list_item_2, appList) {
            @Override
            public View getView(int pos, View convertView, ViewGroup parent) {
                View v = super.getView(pos, convertView, parent);
                AppEntry e = getItem(pos);
                if (e != null) {
                    TextView t1 = v.findViewById(android.R.id.text1);
                    TextView t2 = v.findViewById(android.R.id.text2);
                    if (t1 != null) t1.setText(e.label);
                    if (t2 != null) t2.setText(e.packageName);
                }
                return v;
            }
        };
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, pos, id) -> {
            if (pos < 0 || pos >= appList.size()) return;
            AppEntry chosen = appList.get(pos);
            showActivities(chosen);
        });
    }

    private void showActivities(AppEntry app) {
        backButton.setVisibility(View.VISIBLE);
        final List<ActivityEntry> activities = new ArrayList<>();
        try {
            PackageManager pm = getPackageManager();
            PackageInfo pi = pm.getPackageInfo(app.packageName, PackageManager.GET_ACTIVITIES);
            if (pi.activities != null) {
                for (ActivityInfo ai : pi.activities) {
                    if (ai.exported) {
                        CharSequence lbl = ai.loadLabel(pm);
                        String label = lbl != null ? lbl.toString() : (ai.name != null ? ai.name : ai.packageName);
                        String pkg = ai.packageName != null ? ai.packageName : app.packageName;
                        activities.add(new ActivityEntry(label, pkg, ai.name));
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Fall back to the default launch activity if no exported activities found
        if (activities.isEmpty()) {
            try {
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(app.packageName);
                if (launchIntent != null && launchIntent.getComponent() != null) {
                    activities.add(new ActivityEntry(
                            app.label, app.packageName, launchIntent.getComponent().getClassName()));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (activities.isEmpty()) {
            // Nothing to show
            ArrayList<ActivityEntry> empty = new ArrayList<>();
            empty.add(new ActivityEntry(getString(R.string.alias_picker_no_activities), app.packageName, ""));
            ArrayAdapter<ActivityEntry> adapter = new ArrayAdapter<ActivityEntry>(this,
                    android.R.layout.simple_list_item_2, empty) {
                @Override
                public View getView(int pos, View convertView, ViewGroup parent) {
                    View v = super.getView(pos, convertView, parent);
                    ActivityEntry e = getItem(pos);
                    if (e != null) {
                        TextView t1 = v.findViewById(android.R.id.text1);
                        TextView t2 = v.findViewById(android.R.id.text2);
                        if (t1 != null) t1.setText(e.label);
                        if (t2 != null) t2.setText(e.className);
                    }
                    return v;
                }
            };
            listView.setAdapter(adapter);
            listView.setOnItemClickListener((parent, view, pos, id) -> {});
            return;
        }

        ArrayAdapter<ActivityEntry> adapter = new ArrayAdapter<ActivityEntry>(this,
                android.R.layout.simple_list_item_2, activities) {
            @Override
            public View getView(int pos, View convertView, ViewGroup parent) {
                View v = super.getView(pos, convertView, parent);
                ActivityEntry e = getItem(pos);
                if (e != null) {
                    TextView t1 = v.findViewById(android.R.id.text1);
                    TextView t2 = v.findViewById(android.R.id.text2);
                    if (t1 != null) t1.setText(e.label);
                    if (t2 != null) t2.setText(e.className);
                }
                return v;
            }
        };
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, pos, id) -> {
            if (pos < 0 || pos >= activities.size()) return;
            ActivityEntry e = activities.get(pos);
            // Send broadcast with selection (consumer listens for this action)
            Intent broadcast = new Intent("net.nhiroki.bluelineconsole.ACTION_ALIAS_COMPONENT_PICKED");
            broadcast.putExtra(EXTRA_COMPONENT, e.packageName + "/" + e.className);
            sendBroadcast(broadcast);
            finish();
        });
    }
}
