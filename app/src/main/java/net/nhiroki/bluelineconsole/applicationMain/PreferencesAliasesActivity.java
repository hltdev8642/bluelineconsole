package net.nhiroki.bluelineconsole.applicationMain;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import net.nhiroki.bluelineconsole.R;
import net.nhiroki.bluelineconsole.dataStore.persistent.AliasDatabase;

import java.util.List;

public class PreferencesAliasesActivity extends BaseWindowActivity {

    public static final int REQUEST_ADD_ALIAS = 1;
    private AliasDatabase aliasDatabase;
    private List<AliasDatabase.Alias> aliases;
    private ListView listView;

    public PreferencesAliasesActivity() {
        super(R.layout.preferences_aliases_body, false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        aliasDatabase = new AliasDatabase();

        this.setHeaderFooterTexts(getString(R.string.preferences_title_aliases), null);
        this.setWindowBoundarySize(ROOT_WINDOW_FULL_WIDTH_IN_MOBILE, 2);

        listView = findViewById(R.id.aliasesListView);
        Button addButton = findViewById(R.id.aliasesAddButton);

        addButton.setOnClickListener(v -> {
            Intent i = new Intent(this, PreferencesAliasesEachActivity.class);
            //noinspection deprecation
            startActivityForResult(i, REQUEST_ADD_ALIAS);
        });

        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            AliasDatabase.Alias alias = aliases.get(position);
            new android.app.AlertDialog.Builder(this)
                .setTitle(alias.keyword)
                .setMessage(getString(R.string.alias_delete_confirm))
                .setPositiveButton(R.string.button_delete, (d, w) -> {
                    aliasDatabase.delete(this, alias.keyword);
                    refreshList();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
            return true;
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            Intent i = new Intent(this, PreferencesAliasesEachActivity.class);
            AliasDatabase.Alias alias = aliases.get(position);
            i.putExtra("keyword", alias.keyword);
            i.putExtra("title", alias.title);
            i.putExtra("target", alias.target);
            i.putExtra("type", alias.type);
            if (alias.icon != null) i.putExtra("icon", alias.icon);
            //noinspection deprecation
            startActivityForResult(i, REQUEST_ADD_ALIAS);
        });

        refreshList();
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) refreshList();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshList();
    }

    @Override
    protected void onStop() {
        super.onStop();
        this.finish();
    }

    private void refreshList() {
        aliases = aliasDatabase.getAll(this);
        String[] items = new String[aliases.size()];
        for (int i = 0; i < aliases.size(); i++) {
            items[i] = aliases.get(i).keyword + " → " + aliases.get(i).title + " (" + aliases.get(i).type + ")";
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items);
        listView.setAdapter(adapter);
    }
}
