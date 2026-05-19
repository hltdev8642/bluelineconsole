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
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void refreshExtrasList() {
        List<String> items = new ArrayList<>();
        for (IntentSpec.Extra e: extras) items.add(e.key + " (" + e.type + ") = " + e.value);
        extrasAdapter.clear(); extrasAdapter.addAll(items); extrasAdapter.notifyDataSetChanged();
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
}
