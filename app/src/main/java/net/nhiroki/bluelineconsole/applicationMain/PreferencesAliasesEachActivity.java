package net.nhiroki.bluelineconsole.applicationMain;

import android.app.AlertDialog;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import net.nhiroki.bluelineconsole.R;
import net.nhiroki.bluelineconsole.dataStore.persistent.AliasDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PreferencesAliasesEachActivity extends BaseWindowActivity {

    private EditText targetEdit;
    private Button browseAppsButton;

    public PreferencesAliasesEachActivity() {
        super(R.layout.preferences_aliases_each_body, false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setHeaderFooterTexts(getString(R.string.preferences_title_alias_edit), null);
        this.setWindowBoundarySize(ROOT_WINDOW_FULL_WIDTH_IN_MOBILE, 2);

        EditText keywordEdit = findViewById(R.id.aliasKeywordEdit);
        EditText titleEdit = findViewById(R.id.aliasTitleEdit);
        targetEdit = findViewById(R.id.aliasTargetEdit);
        RadioGroup typeGroup = findViewById(R.id.aliasTypeGroup);
        Button saveButton = findViewById(R.id.aliasSaveButton);
        browseAppsButton = findViewById(R.id.aliasBrowseAppsButton);

        String keyword = getIntent().getStringExtra("keyword");
        if (keyword != null) {
            keywordEdit.setText(keyword);
            titleEdit.setText(getIntent().getStringExtra("title"));
            targetEdit.setText(getIntent().getStringExtra("target"));
            String type = getIntent().getStringExtra("type");
            if ("app".equals(type)) {
                ((RadioButton) findViewById(R.id.aliasTypeApp)).setChecked(true);
                browseAppsButton.setVisibility(View.VISIBLE);
            }
        }

        typeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isApp = checkedId == R.id.aliasTypeApp;
            browseAppsButton.setVisibility(isApp ? View.VISIBLE : View.GONE);
        });

        browseAppsButton.setOnClickListener(v -> showAppPickerDialog());

        saveButton.setOnClickListener(v -> {
            String kw = keywordEdit.getText().toString().trim();
            String title = titleEdit.getText().toString().trim();
            String target = targetEdit.getText().toString().trim();
            int checkedId = typeGroup.getCheckedRadioButtonId();
            String type = checkedId == R.id.aliasTypeApp ? "app" : "url";

            if (kw.isEmpty() || title.isEmpty() || target.isEmpty()) {
                Toast.makeText(this, R.string.alias_fill_all_fields, Toast.LENGTH_SHORT).show();
                return;
            }

            AliasDatabase.Alias alias = new AliasDatabase.Alias(kw, title, target, type);
            new AliasDatabase().add(this, alias);
            setResult(RESULT_OK);
            finish();
        });
    }

    private void showAppPickerDialog() {
        // Load apps on a background thread to avoid blocking the UI
        new Thread(() -> {
            final List<String> labels = new ArrayList<>();
            final List<String> packages = new ArrayList<>();
            try {
                PackageManager pm = getPackageManager();
                Intent query = new Intent(Intent.ACTION_MAIN);
                query.addCategory(Intent.CATEGORY_LAUNCHER);
                List<ResolveInfo> resolved = pm.queryIntentActivities(query, 0);
                if (resolved != null) {
                    for (ResolveInfo ri : resolved) {
                        CharSequence lbl = ri.loadLabel(pm);
                        labels.add(lbl != null ? lbl.toString() : ri.activityInfo.packageName);
                        packages.add(ri.activityInfo != null ? ri.activityInfo.packageName : "");
                    }
                }
                // Sort by label
                List<int[]> indices = new ArrayList<>();
                for (int i = 0; i < labels.size(); i++) indices.add(new int[]{i});
                Collections.sort(indices, (a, b) -> labels.get(a[0]).compareToIgnoreCase(labels.get(b[0])));
                final List<String> sortedLabels = new ArrayList<>();
                final List<String> sortedPackages = new ArrayList<>();
                for (int[] idx : indices) {
                    sortedLabels.add(labels.get(idx[0]));
                    sortedPackages.add(packages.get(idx[0]));
                }
                runOnUiThread(() -> showAppDialog(sortedLabels, sortedPackages));
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, R.string.alias_picker_error_no_apps, Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void showAppDialog(List<String> labels, List<String> packageNames) {
        if (isFinishing()) return;
        String[] items = labels.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle(R.string.alias_picker_title)
                .setItems(items, (dialog, which) -> showActivityDialog(labels.get(which), packageNames.get(which)))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showActivityDialog(String appLabel, String packageName) {
        if (isFinishing()) return;
        final List<String> activityLabels = new ArrayList<>();
        final List<String> componentNames = new ArrayList<>();
        try {
            PackageManager pm = getPackageManager();
            PackageInfo pi = pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            if (pi.activities != null) {
                for (ActivityInfo ai : pi.activities) {
                    if (ai.exported) {
                        CharSequence lbl = ai.loadLabel(pm);
                        activityLabels.add(lbl != null ? lbl.toString() : ai.name);
                        componentNames.add(ai.packageName + "/" + ai.name);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Fall back to launch intent
        if (componentNames.isEmpty()) {
            try {
                Intent li = getPackageManager().getLaunchIntentForPackage(packageName);
                if (li != null && li.getComponent() != null) {
                    activityLabels.add(appLabel);
                    componentNames.add(packageName + "/" + li.getComponent().getClassName());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (componentNames.isEmpty()) {
            componentNames.add(packageName);
            activityLabels.add(appLabel + " (package)");
        }
        String[] items = activityLabels.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle(appLabel)
                .setItems(items, (dialog, which) -> {
                    targetEdit.setText(componentNames.get(which));
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}

