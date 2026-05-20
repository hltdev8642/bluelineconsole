package net.nhiroki.bluelineconsole.applicationMain;

import android.app.AlertDialog;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import net.nhiroki.bluelineconsole.R;
import net.nhiroki.bluelineconsole.dataStore.persistent.AliasDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.LinkedHashSet;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import android.graphics.drawable.Drawable;

public class PreferencesAliasesEachActivity extends BaseWindowActivity {

    private EditText targetEdit;
    private Button browseAppsButton;
    private ImageView aliasIconPreview;
    private Button aliasUseAppIconButton;
    private Button aliasChooseIconButton;

    private static final int REQUEST_PICK_ICON = 3001;
    private String chosenIconUri = null; // raw content URI string
    private boolean useAppIcon = true;

    // Intent editor state (moved to fields so onActivityResult can update)
    private String currentIntentJson = null;
    private android.widget.TextView aliasIntentSummary;
    private Button aliasEditIntentButton;

    // whether this activity is creating a new alias (vs editing existing)
    private boolean creatingNewAlias = false;

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

        aliasIconPreview = findViewById(R.id.aliasIconPreview);
        aliasUseAppIconButton = findViewById(R.id.aliasUseAppIconButton);
        aliasChooseIconButton = findViewById(R.id.aliasChooseIconButton);

        aliasEditIntentButton = findViewById(R.id.aliasEditIntentButton);
        aliasIntentSummary = findViewById(R.id.aliasIntentSummary);

        String keyword = getIntent().getStringExtra("keyword");
        creatingNewAlias = (keyword == null);
        if (keyword != null) {
            keywordEdit.setText(keyword);
            titleEdit.setText(getIntent().getStringExtra("title"));
            targetEdit.setText(getIntent().getStringExtra("target"));
            String type = getIntent().getStringExtra("type");
            if ("app".equals(type)) {
                ((RadioButton) findViewById(R.id.aliasTypeApp)).setChecked(true);
                browseAppsButton.setVisibility(View.VISIBLE);
            } else if ("intent".equals(type)) {
                ((RadioButton) findViewById(R.id.aliasTypeIntent)).setChecked(true);
                aliasEditIntentButton.setVisibility(View.VISIBLE);
                String intentJson = getIntent().getStringExtra("intentSpecJson");
                currentIntentJson = intentJson;
                if (intentJson != null) {
                    aliasIntentSummary.setVisibility(View.VISIBLE);
                    aliasIntentSummary.setText(intentJson);
                }
            }
            String iconExtra = getIntent().getStringExtra("icon");
            if (iconExtra != null) {
                // iconExtra may be in form "uri:<uri>"
                if (iconExtra.startsWith("uri:")) {
                    chosenIconUri = iconExtra.substring(4);
                    useAppIcon = false;
                    try {
                        aliasIconPreview.setImageURI(Uri.parse(chosenIconUri));
                    } catch (Exception ignored) {}
                } else {
                    // unknown format, treat as app icon default
                    useAppIcon = true;
                }
            } else {
                useAppIcon = true;
            }
        } else {
            useAppIcon = true;
        }

        typeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isApp = checkedId == R.id.aliasTypeApp;
            boolean isIntent = checkedId == R.id.aliasTypeIntent;
            browseAppsButton.setVisibility((isApp || isIntent) ? View.VISIBLE : View.GONE);
            aliasEditIntentButton.setVisibility(isIntent ? View.VISIBLE : View.GONE);
            aliasIntentSummary.setVisibility(isIntent && currentIntentJson != null ? View.VISIBLE : View.GONE);

            // If user is creating a new alias and just checked Intent, open app picker to help build intent
            if (isIntent && creatingNewAlias) {
                creatingNewAlias = false; // avoid reopening repeatedly
                showAppPickerDialog();
            }
        });

        aliasEditIntentButton.setOnClickListener(v -> {
            Intent i = new Intent(PreferencesAliasesEachActivity.this, IntentEditorActivity.class);
            if (currentIntentJson != null) i.putExtra(IntentEditorActivity.EXTRA_INTENT_SPEC_JSON, currentIntentJson);
            startActivityForResult(i, 4001);
        });

        browseAppsButton.setOnClickListener(v -> showAppPickerDialog());

        aliasUseAppIconButton.setOnClickListener(v -> {
            useAppIcon = true;
            chosenIconUri = null;
            // if target contains package, update preview to that app's icon
            String target = targetEdit.getText().toString().trim();
            if (target.contains("/")) {
                String pkg = target.split("/")[0];
                updateIconPreviewForPackage(pkg);
            }
        });

        aliasChooseIconButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("image/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try {
                startActivityForResult(intent, REQUEST_PICK_ICON);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        saveButton.setOnClickListener(v -> {
            String kw = keywordEdit.getText().toString().trim();
            String title = titleEdit.getText().toString().trim();
            String target = targetEdit.getText().toString().trim();
            int checkedId = typeGroup.getCheckedRadioButtonId();
            String type;
            if (checkedId == R.id.aliasTypeApp) type = "app";
            else if (checkedId == R.id.aliasTypeIntent) type = "intent";
            else type = "url";

            if (kw.isEmpty() || title.isEmpty() || (type.equals("intent") ? false : target.isEmpty())) {
                // For intent type, target may be empty
                Toast.makeText(this, R.string.alias_fill_all_fields, Toast.LENGTH_SHORT).show();
                return;
            }

            String iconValue = null;
            if (!useAppIcon && chosenIconUri != null) {
                iconValue = "uri:" + chosenIconUri;
            }

            String intentJson = null;
            if (type.equals("intent")) intentJson = currentIntentJson;

            AliasDatabase.Alias alias = new AliasDatabase.Alias(kw, title, target, type, iconValue, intentJson);
            new AliasDatabase().add(this, alias);
            setResult(RESULT_OK);
            finish();
        });
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_ICON && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                // Persist read permission
                final int takeFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION);
                getContentResolver().takePersistableUriPermission(uri, takeFlags);
            } catch (Exception ignored) {}
            chosenIconUri = uri.toString();
            useAppIcon = false;
            try {
                aliasIconPreview.setImageURI(Uri.parse(chosenIconUri));
            } catch (Exception ignored) {}
        } else if (requestCode == 4001 && resultCode == RESULT_OK && data != null) {
            String json = data.getStringExtra(IntentEditorActivity.EXTRA_INTENT_SPEC_JSON);
            if (json != null) {
                currentIntentJson = json;
                aliasIntentSummary.setVisibility(View.VISIBLE);
                aliasIntentSummary.setText(json);
            }
        }
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
                        String actLabel;
                        if (ai.labelRes != 0) {
                            actLabel = ai.loadLabel(pm).toString() + "\n" + ai.name;
                        } else {
                            actLabel = ai.name;
                        }
                        activityLabels.add(actLabel);
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
                    String selectedComponent = componentNames.get(which);
                    targetEdit.setText(selectedComponent);

                    // If using app icon mode, update preview to the selected package's icon
                    if (useAppIcon) {
                        String sel = selectedComponent;
                        String pkg = sel.contains("/") ? sel.split("/")[0] : sel;
                        updateIconPreviewForPackage(pkg);
                    }

                    // Start discovery of supported actions/mime types on a background thread
                    final String selComp = selectedComponent;
                    new Thread(() -> {
                        try {
                            android.content.pm.PackageManager pm = getPackageManager();
                            String pkg = selComp.contains("/") ? selComp.split("/")[0] : selComp;
                            String cls = selComp.contains("/") ? selComp.split("/")[1] : null;

                            final List<net.nhiroki.bluelineconsole.plugin.IntentSpec> discovered = new ArrayList<>();

                            List<String> actionsList = new ArrayList<>();
                            // Common Android actions
                            actionsList.add(android.content.Intent.ACTION_MAIN);
                            actionsList.add(android.content.Intent.ACTION_VIEW);
                            actionsList.add(android.content.Intent.ACTION_SEND);
                            actionsList.add(android.content.Intent.ACTION_SENDTO);
                            actionsList.add(android.content.Intent.ACTION_EDIT);
                            actionsList.add(android.content.Intent.ACTION_PICK);
                            actionsList.add(android.content.Intent.ACTION_SEARCH);
                            actionsList.add(android.content.Intent.ACTION_DIAL);
                            actionsList.add(android.content.Intent.ACTION_CALL);
                            actionsList.add(android.content.Intent.ACTION_OPEN_DOCUMENT);

                            // Heuristics: package-specific action patterns
                            String pkgPref = pkg;
                            if (pkgPref != null && !pkgPref.isEmpty()) {
                                actionsList.add(pkgPref + ".ACTION_OPEN");
                                actionsList.add(pkgPref + ".ACTION_OPEN_FOLDER");
                                actionsList.add(pkgPref + ".ACTION_VIEW");
                                actionsList.add(pkgPref + ".VIEW");
                                actionsList.add(pkgPref + ".action.OPEN");
                                actionsList.add(pkgPref + ".action.VIEW");
                                if (cls != null && !cls.isEmpty()) {
                                    String simpleCls = cls.contains(".") ? cls.substring(cls.lastIndexOf('.')+1) : cls;
                                    actionsList.add(pkgPref + "." + simpleCls + ".ACTION");
                                    actionsList.add(pkgPref + "." + simpleCls + ".VIEW");
                                }
                            }

                            // Attempt to parse pm dump for exact intent-filters (may require shell permissions)
                            List<String> pmActions = parsePmDumpForActions(pkg);
                            if (pmActions != null && !pmActions.isEmpty()) {
                                // prepend pmActions to the list so exact actions are tried first
                                List<String> merged = new ArrayList<>();
                                merged.addAll(pmActions);
                                merged.addAll(actionsList);
                                actionsList = merged;
                            }

                            // Deduplicate while preserving order
                            Set<String> seen = new HashSet<>();
                            List<String> finalActions = new ArrayList<>();
                            for (String a : actionsList) {
                                if (a != null && !a.isEmpty() && !seen.contains(a)) { seen.add(a); finalActions.add(a); }
                            }

                            String[] mimes = new String[]{null, "text/plain", "image/*", "video/*", "audio/*", "*/*"};

                            for (String action : finalActions) {
                                for (String mime : mimes) {
                                    Intent test = new Intent();
                                    if (action != null) test.setAction(action);
                                    if (mime != null) test.setType(mime);
                                    if (cls != null) test.setClassName(pkg, cls);

                                    List<android.content.pm.ResolveInfo> ris = pm.queryIntentActivities(test, 0);
                                    boolean matches = false;
                                    if (ris != null) {
                                        for (android.content.pm.ResolveInfo ri : ris) {
                                            if (ri.activityInfo != null && ri.activityInfo.packageName != null && ri.activityInfo.packageName.equals(pkg)) {
                                                if (cls == null || ri.activityInfo.name.equals(cls)) {
                                                    matches = true;
                                                    break;
                                                }
                                            }
                                        }
                                    }

                                    if (matches) {
                                        net.nhiroki.bluelineconsole.plugin.IntentSpec spec = new net.nhiroki.bluelineconsole.plugin.IntentSpec();
                                        spec.componentPackage = selComp;
                                        spec.action = action;
                                        spec.mimeType = mime;
                                        discovered.add(spec);
                                    }
                                }
                            }

                            // Post results to UI thread
                            runOnUiThread(() -> {
                                if (discovered.isEmpty()) {
                                    // fallback: create simple ACTION_MAIN spec
                                    try {
                                        net.nhiroki.bluelineconsole.plugin.IntentSpec spec = new net.nhiroki.bluelineconsole.plugin.IntentSpec();
                                        spec.componentPackage = selComp;
                                        spec.action = android.content.Intent.ACTION_MAIN;
                                        String json = spec.toJson().toString();
                                        currentIntentJson = json;
                                        ((RadioButton) findViewById(R.id.aliasTypeIntent)).setChecked(true);
                                        aliasEditIntentButton.setVisibility(View.VISIBLE);
                                        aliasIntentSummary.setVisibility(View.VISIBLE);
                                        aliasIntentSummary.setText(json);
                                    } catch (org.json.JSONException e) { e.printStackTrace(); }
                                    return;
                                }

                                // Show a chooser dialog of discovered intents
                                CharSequence[] labels = new CharSequence[discovered.size()];
                                for (int i = 0; i < discovered.size(); i++) {
                                    net.nhiroki.bluelineconsole.plugin.IntentSpec s = discovered.get(i);
                                    String lbl = (s.action == null ? "(no action)" : s.action) + (s.mimeType == null ? "" : " — " + s.mimeType);
                                    labels[i] = lbl;
                                }

                                new AlertDialog.Builder(PreferencesAliasesEachActivity.this)
                                        .setTitle(R.string.alias_picker_title)
                                        .setItems(labels, (d2, which2) -> {
                                            try {
                                                net.nhiroki.bluelineconsole.plugin.IntentSpec chosen = discovered.get(which2);
                                                String json = chosen.toJson().toString();
                                                currentIntentJson = json;
                                                ((RadioButton) findViewById(R.id.aliasTypeIntent)).setChecked(true);
                                                aliasEditIntentButton.setVisibility(View.VISIBLE);
                                                aliasIntentSummary.setVisibility(View.VISIBLE);
                                                aliasIntentSummary.setText(json);
                                            } catch (org.json.JSONException e) { e.printStackTrace(); }
                                        })
                                        .setNegativeButton(android.R.string.cancel, null)
                                        .show();
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }).start();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private List<String> parsePmDumpForActions(String pkg) {
        if (pkg == null || pkg.isEmpty()) return null;
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"sh","-c","pm dump " + pkg});
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            List<String> found = new ArrayList<>();
            while ((line = br.readLine()) != null) {
                if (line.toLowerCase().contains("filter")) {
                    StringBuilder block = new StringBuilder();
                    for (int i = 0; i < 20; i++) {
                        String l = br.readLine();
                        if (l == null) break;
                        block.append(l).append('\n');
                    }
                    String b = block.toString();
                    // extract tokens in single quotes first
                    Pattern p1 = Pattern.compile("'([^']+)'");
                    Matcher m1 = p1.matcher(b);
                    while (m1.find()) {
                        String tok = m1.group(1);
                        if (tok != null && (tok.contains(".") || tok.contains("/"))) found.add(tok);
                    }
                    // also try action=NAME patterns
                    Pattern p2 = Pattern.compile("action=([^\\s,]+)");
                    Matcher m2 = p2.matcher(b);
                    while (m2.find()) {
                        String tok = m2.group(1);
                        if (tok != null) found.add(tok);
                    }
                    // mime patterns like type=... or mime=...
                    Pattern p3 = Pattern.compile("(mime|type)=([^\\s,]+)");
                    Matcher m3 = p3.matcher(b);
                    while (m3.find()) {
                        String tok = m3.group(2);
                        if (tok != null && tok.contains("/")) found.add(tok);
                    }
                }
            }
            // dedupe preserving order
            LinkedHashSet<String> set = new LinkedHashSet<>(found);
            return new ArrayList<>(set);
        } catch (Exception e) {
            return null;
        }
    }

    private void updateIconPreviewForPackage(String packageName) {
        try {
            Drawable d = getPackageManager().getApplicationIcon(packageName);
            aliasIconPreview.setImageDrawable(d);
        } catch (Exception e) {
            // ignore, leave default
        }
    }
}

