package net.nhiroki.bluelineconsole.applicationMain;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import net.nhiroki.bluelineconsole.R;
import net.nhiroki.bluelineconsole.plugin.IntentSpec;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class IntentEditorActivity extends BaseWindowActivity {
    public static final String EXTRA_INTENT_SPEC_JSON = "intent_spec_json";

    private EditText actionEdit;
    private EditText dataUriEdit;
    private EditText mimeTypeEdit;
    private EditText componentEdit;
    private EditText flagsEdit;
    private ListView extrasListView;
    private ArrayAdapter<String> extrasAdapter;
    private final List<IntentSpec.Extra> extras = new ArrayList<>();

    private ListView categoriesListView;
    private ArrayAdapter<String> categoriesAdapter;
    private final List<String> categories = new ArrayList<>();

    public IntentEditorActivity() { super(R.layout.preferences_intent_editor_body, false); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHeaderFooterTexts(getString(R.string.alias_edit_intent), null);

        actionEdit = findViewById(R.id.intentActionEdit);
        dataUriEdit = findViewById(R.id.intentDataUriEdit);
        mimeTypeEdit = findViewById(R.id.intentMimeTypeEdit);
        componentEdit = findViewById(R.id.intentComponentEdit);
        flagsEdit = findViewById(R.id.intentFlagsEdit);
        extrasListView = findViewById(R.id.intentExtrasListView);

        extrasAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        extrasListView.setAdapter(extrasAdapter);

        Button addExtraButton = findViewById(R.id.intentAddExtraButton);
        addExtraButton.setOnClickListener(v -> showAddExtraDialog());

        categoriesListView = findViewById(R.id.intentCategoriesListView);
        categoriesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        categoriesListView.setAdapter(categoriesAdapter);
        Button addCategoryButton = findViewById(R.id.intentAddCategoryButton);
        addCategoryButton.setOnClickListener(v -> showAddCategoryDialog());

        // Discover button to probe supported actions/mime types for the specified component
        Button discoverButton = findViewById(R.id.intentDiscoverButton);
        discoverButton.setOnClickListener(v -> {
            String comp = componentEdit.getText().toString().trim();
            if (comp.isEmpty()) {
                Toast.makeText(this, "Specify component (package/class) first", Toast.LENGTH_SHORT).show();
                return;
            }
            // Run discovery off the UI thread
            new Thread(() -> {
                try {
                    android.content.pm.PackageManager pm = getPackageManager();
                    String pkg = comp.contains("/") ? comp.split("/")[0] : comp;
                    String cls = comp.contains("/") ? comp.split("/")[1] : null;

                    final List<net.nhiroki.bluelineconsole.plugin.IntentSpec> discovered = new ArrayList<>();

                    String[] actions = new String[]{
                            android.content.Intent.ACTION_MAIN,
                            android.content.Intent.ACTION_VIEW,
                            android.content.Intent.ACTION_SEND,
                            android.content.Intent.ACTION_SENDTO,
                            android.content.Intent.ACTION_EDIT,
                            android.content.Intent.ACTION_PICK,
                            android.content.Intent.ACTION_SEARCH,
                            android.content.Intent.ACTION_DIAL,
                            android.content.Intent.ACTION_CALL,
                            android.content.Intent.ACTION_OPEN_DOCUMENT
                    };

                    String[] mimes = new String[]{null, "text/plain", "image/*", "video/*", "audio/*", "*/*"};

                    for (String action : actions) {
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
                                spec.componentPackage = comp;
                                spec.action = action;
                                spec.mimeType = mime;
                                discovered.add(spec);
                            }
                        }
                    }

                    runOnUiThread(() -> {
                        if (discovered.isEmpty()) {
                            Toast.makeText(this, "No discovered actions/mime for component", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        CharSequence[] labels = new CharSequence[discovered.size()];
                        for (int i = 0; i < discovered.size(); i++) {
                            net.nhiroki.bluelineconsole.plugin.IntentSpec s = discovered.get(i);
                            String lbl = (s.action == null ? "(no action)" : s.action) + (s.mimeType == null ? "" : " — " + s.mimeType);
                            labels[i] = lbl;
                        }

                        new AlertDialog.Builder(IntentEditorActivity.this)
                                .setTitle("Discovered actions / mime types")
                                .setItems(labels, (d2, which2) -> {
                                    net.nhiroki.bluelineconsole.plugin.IntentSpec chosen = discovered.get(which2);
                                    // Offer to set as action/mime or add as category
                                    String choice = "Set action/mime";
                                    String addAsCat = "Add as category/action";
                                    new AlertDialog.Builder(IntentEditorActivity.this)
                                            .setItems(new CharSequence[]{choice, addAsCat}, (d3, which3) -> {
                                                if (which3 == 0) {
                                                    actionEdit.setText(chosen.action == null ? "" : chosen.action);
                                                    mimeTypeEdit.setText(chosen.mimeType == null ? "" : chosen.mimeType);
                                                } else {
                                                    String candidate = chosen.action == null ? (chosen.mimeType == null ? "" : chosen.mimeType) : chosen.action;
                                                    if (candidate == null || candidate.isEmpty()) return;
                                                    // validate format before adding
                                                    if (!isValidCategoryFormat(candidate)) {
                                                        new AlertDialog.Builder(IntentEditorActivity.this)
                                                                .setTitle("Unusual format")
                                                                .setMessage("The value does not look like a typical action/category. Add anyway?")
                                                                .setPositiveButton(android.R.string.ok, (dd, ww) -> { categories.add(candidate); refreshCategoriesList(); })
                                                                .setNegativeButton(android.R.string.cancel, null)
                                                                .show();
                                                    } else {
                                                        categories.add(candidate);
                                                        refreshCategoriesList();
                                                    }
                                                }
                                            })
                                            .show();
                                })
                                .setNegativeButton(android.R.string.cancel, null)
                                .show();
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(this, "Discovery failed", Toast.LENGTH_SHORT).show());
                }
            }).start();
        });

        // Long-press to remove categories
        categoriesListView.setOnItemLongClickListener((parent, view, position, id) -> {
            String item = categories.get(position);
            new AlertDialog.Builder(this)
                    .setTitle("Remove")
                    .setMessage("Remove '" + item + "'?" )
                    .setPositiveButton(android.R.string.ok, (d, w) -> { categories.remove(position); refreshCategoriesList(); })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
            return true;
        });

        Button saveButton = findViewById(R.id.intentSaveButton);
        saveButton.setOnClickListener(v -> {
            IntentSpec spec = new IntentSpec();
            spec.action = actionEdit.getText().toString().trim();
            spec.dataUri = dataUriEdit.getText().toString().trim();
            spec.mimeType = mimeTypeEdit.getText().toString().trim();
            spec.componentPackage = componentEdit.getText().toString().trim();
            String flags = flagsEdit.getText().toString().trim();
            if (!flags.isEmpty()) {
                String[] fs = flags.split(",");
                for (String f: fs) spec.flags.add(f.trim());
            }
            spec.extras.addAll(extras);
            spec.categories.addAll(categories);
            try {
                String json = spec.toJson().toString();
                Intent i = new Intent();
                i.putExtra(EXTRA_INTENT_SPEC_JSON, json);
                setResult(RESULT_OK, i);
                finish();
            } catch (JSONException e) {
                e.printStackTrace();
                Toast.makeText(this, "Unable to serialize", Toast.LENGTH_SHORT).show();
            }
        });

        // Load existing spec if provided
        String json = getIntent().getStringExtra(EXTRA_INTENT_SPEC_JSON);
        if (json != null) {
            try {
                IntentSpec spec = IntentSpec.fromJson(json);
                if (spec != null) {
                    actionEdit.setText(spec.action == null ? "" : spec.action);
                    dataUriEdit.setText(spec.dataUri == null ? "" : spec.dataUri);
                    mimeTypeEdit.setText(spec.mimeType == null ? "" : spec.mimeType);
                    componentEdit.setText(spec.componentPackage == null ? "" : spec.componentPackage);
                    if (spec.flags != null && !spec.flags.isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        for (String f: spec.flags) { if (sb.length() != 0) sb.append(','); sb.append(f); }
                        flagsEdit.setText(sb.toString());
                    }
                    if (spec.extras != null) {
                        extras.clear();
                        extras.addAll(spec.extras);
                        refreshExtrasList();
                    }
                    if (spec.categories != null) {
                        categories.clear();
                        categories.addAll(spec.categories);
                        refreshCategoriesList();
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void refreshExtrasList() {
        List<String> items = new ArrayList<>();
        for (IntentSpec.Extra e: extras) items.add(e.key + " (" + e.type + ") = " + e.value);
        extrasAdapter.clear(); extrasAdapter.addAll(items); extrasAdapter.notifyDataSetChanged();
    }

    private void refreshCategoriesList() {
        categoriesAdapter.clear(); categoriesAdapter.addAll(categories); categoriesAdapter.notifyDataSetChanged();
    }

    private void showAddExtraDialog() {
        View v = getLayoutInflater().inflate(R.layout.intent_extra_dialog_body, null);
        EditText keyEdit = v.findViewById(R.id.extraKeyEdit);
        EditText valueEdit = v.findViewById(R.id.extraValueEdit);
        Spinner typeSpinner = v.findViewById(R.id.extraTypeSpinner);
        android.widget.CheckBox useQueryCheckbox = v.findViewById(R.id.extraUseQueryCheckbox);

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"string","int","boolean"});
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(typeAdapter);

        useQueryCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            valueEdit.setEnabled(!isChecked);
            if (isChecked) {
                valueEdit.setText("{query}");
            } else {
                if (valueEdit.getText().toString().equals("{query}")) valueEdit.setText("");
            }
        });

        new AlertDialog.Builder(this)
                .setTitle(R.string.intent_add_extra)
                .setView(v)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    IntentSpec.Extra ex = new IntentSpec.Extra();
                    ex.key = keyEdit.getText().toString().trim();
                    ex.type = (String) typeSpinner.getSelectedItem();
                    ex.value = valueEdit.getText().toString();
                    if (!ex.key.isEmpty()) {
                        extras.add(ex);
                        refreshExtrasList();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private boolean isValidCategoryFormat(String txt) {
        if (txt == null) return false;
        txt = txt.trim();
        if (txt.isEmpty()) return false;
        if (txt.startsWith("android.intent.")) return true;
        if (txt.contains(".")) return true; // package-like
        if (txt.contains("/")) return true; // component-like
        return false;
    }

    private void showAddCategoryDialog() {
        View v = getLayoutInflater().inflate(android.R.layout.simple_list_item_1, null);
        final EditText input = new EditText(this);
        input.setHint("e.g. com.mixplore.ACTION_OPEN_FOLDER");
        new AlertDialog.Builder(this)
                .setTitle("Add category/action")
                .setView(input)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    String txt = input.getText().toString().trim();
                    if (txt.isEmpty()) return;
                    if (!isValidCategoryFormat(txt)) {
                        new AlertDialog.Builder(this)
                                .setTitle("Unusual format")
                                .setMessage("The value does not look like a typical action/category. Add anyway?")
                                .setPositiveButton(android.R.string.ok, (dd, ww) -> { categories.add(txt); refreshCategoriesList(); })
                                .setNegativeButton(android.R.string.cancel, null)
                                .show();
                    } else {
                        categories.add(txt);
                        refreshCategoriesList();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
