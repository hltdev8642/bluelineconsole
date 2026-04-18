package net.nhiroki.bluelineconsole.applicationMain;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import net.nhiroki.bluelineconsole.R;
import net.nhiroki.bluelineconsole.dataStore.persistent.AliasDatabase;

public class PreferencesAliasesEachActivity extends BaseWindowActivity {

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
        EditText targetEdit = findViewById(R.id.aliasTargetEdit);
        RadioGroup typeGroup = findViewById(R.id.aliasTypeGroup);
        Button saveButton = findViewById(R.id.aliasSaveButton);

        String keyword = getIntent().getStringExtra("keyword");
        if (keyword != null) {
            keywordEdit.setText(keyword);
            titleEdit.setText(getIntent().getStringExtra("title"));
            targetEdit.setText(getIntent().getStringExtra("target"));
            String type = getIntent().getStringExtra("type");
            if ("app".equals(type)) {
                ((RadioButton) findViewById(R.id.aliasTypeApp)).setChecked(true);
            }
        }

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
}
