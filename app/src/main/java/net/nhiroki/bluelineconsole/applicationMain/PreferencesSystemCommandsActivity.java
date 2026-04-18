package net.nhiroki.bluelineconsole.applicationMain;

import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.TextView;

import net.nhiroki.bluelineconsole.R;
import net.nhiroki.bluelineconsole.commandSearchers.eachSearcher.SystemCommandSearcher;

import java.util.ArrayList;
import java.util.List;

public class PreferencesSystemCommandsActivity extends BaseWindowActivity {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    public PreferencesSystemCommandsActivity() {
        super(R.layout.preferences_system_commands_body, false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHeaderFooterTexts(getString(R.string.preferences_title_system_commands), null);
        setWindowBoundarySize(ROOT_WINDOW_FULL_WIDTH_IN_MOBILE, 2);

        final SharedPreferences prefs = getSharedPreferences(SystemCommandSearcher.PREFS_NAME, MODE_PRIVATE);
        final List<SystemCommandSearcher.SystemCommandDef> builtIn = SystemCommandSearcher.getBuiltInCommandDefs();
        final List<SystemCommandSearcher.SystemCommandDef> appTiles = SystemCommandSearcher.getAppTileCommandDefs(this);

        final List<Object> rows = new ArrayList<>();
        rows.add(getString(R.string.system_cmd_section_builtin));
        rows.addAll(builtIn);
        if (!appTiles.isEmpty()) {
            rows.add(getString(R.string.system_cmd_section_apps));
            rows.addAll(appTiles);
        }

        final ListView listView = findViewById(R.id.systemCommandsListView);

        final BaseAdapter adapter = new BaseAdapter() {
            @Override public int getCount() { return rows.size(); }
            @Override public Object getItem(int pos) { return rows.get(pos); }
            @Override public long getItemId(int pos) { return pos; }
            @Override public int getViewTypeCount() { return 2; }
            @Override public int getItemViewType(int pos) {
                return rows.get(pos) instanceof String ? TYPE_HEADER : TYPE_ITEM;
            }
            @Override public boolean isEnabled(int pos) {
                return rows.get(pos) instanceof SystemCommandSearcher.SystemCommandDef;
            }
            @Override
            public View getView(int pos, View convertView, ViewGroup parent) {
                Object row = rows.get(pos);
                if (row instanceof String) {
                    TextView tv = convertView instanceof TextView
                            ? (TextView) convertView
                            : new TextView(PreferencesSystemCommandsActivity.this);
                    tv.setText((String) row);
                    tv.setTypeface(null, Typeface.BOLD);
                    tv.setPadding(32, 28, 32, 8);
                    tv.setTextSize(12);
                    tv.setAllCaps(true);
                    return tv;
                }
                SystemCommandSearcher.SystemCommandDef def = (SystemCommandSearcher.SystemCommandDef) row;
                CheckedTextView ctv;
                if (convertView instanceof CheckedTextView) {
                    ctv = (CheckedTextView) convertView;
                } else {
                    ctv = (CheckedTextView) LayoutInflater.from(PreferencesSystemCommandsActivity.this)
                            .inflate(android.R.layout.simple_list_item_multiple_choice, parent, false);
                }
                ctv.setText(def.title);
                ctv.setChecked(prefs.getBoolean("syscmd_" + def.id, true));
                return ctv;
            }
        };

        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            Object row = rows.get(position);
            if (!(row instanceof SystemCommandSearcher.SystemCommandDef)) return;
            SystemCommandSearcher.SystemCommandDef def = (SystemCommandSearcher.SystemCommandDef) row;
            boolean current = prefs.getBoolean("syscmd_" + def.id, true);
            prefs.edit().putBoolean("syscmd_" + def.id, !current).apply();
            adapter.notifyDataSetChanged();
        });
    }

}
