package net.nhiroki.bluelineconsole.applicationMain;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.graphics.Typeface;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;

import net.nhiroki.bluelineconsole.commandSearchers.eachSearcher.SystemCommandSearcher;

import net.nhiroki.bluelineconsole.BuildConfig;
import net.nhiroki.bluelineconsole.R;
import net.nhiroki.bluelineconsole.applicationMain.lib.EditTextConfigurations;
import net.nhiroki.bluelineconsole.applicationMain.theming.AppThemeDirectory;
import net.nhiroki.bluelineconsole.commandSearchers.lib.StringMatchStrategy;
import net.nhiroki.bluelineconsole.commands.urls.WebSearchEngine;
import net.nhiroki.bluelineconsole.commands.urls.WebSearchEnginesDatabase;
import net.nhiroki.bluelineconsole.widget.LauncherWidgetProvider;

import java.util.List;

import static android.provider.Settings.ACTION_VOICE_INPUT_SETTINGS;

public class PreferencesFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public void onResume() {
        super.onResume();

        final List<WebSearchEngine> urlListForLocale = new WebSearchEnginesDatabase(PreferencesFragment.this.getContext()).getURLListForLocale(PreferencesFragment.this.getContext().getResources().getConfiguration().locale, true);

        int webSearchEngineCount = 0;
        for (WebSearchEngine e : urlListForLocale) {
            if (e.has_query) {
                ++webSearchEngineCount;
            }
        }

        CharSequence[] search_engine_entries = new CharSequence[webSearchEngineCount + 1];
        search_engine_entries[0] = getString(R.string.preferences_item_default_search_option_none);

        CharSequence[] search_engine_entry_values = new CharSequence[webSearchEngineCount + 1];
        search_engine_entry_values[0] = "none";

        int searchEnginePos = 1;

        for (WebSearchEngine e : urlListForLocale) {
            if (e.has_query) {
                search_engine_entry_values[searchEnginePos] = e.id_for_preference_value;
                search_engine_entries[searchEnginePos] = e.display_name_locale_independent;
                ++searchEnginePos;
            }
        }

        ((ListPreference) findPreference(WebSearchEnginesDatabase.PREF_KEY_DEFAULT_SEARCH)).setEntries(search_engine_entries);
        ((ListPreference) findPreference(WebSearchEnginesDatabase.PREF_KEY_DEFAULT_SEARCH)).setEntryValues(search_engine_entry_values);

        findPreference("dummy_pref_app_info").setSummary(String.format(this.getString(R.string.displayedFullVersionString), BuildConfig.VERSION_NAME));
        findPreference("dummy_pref_app_info").setSelectable(false);

        int stringMatchStrategySize = StringMatchStrategy.STRATEGY_LIST.length;

        CharSequence[] string_match_strategy_entries = new CharSequence[stringMatchStrategySize];
        CharSequence[] string_match_strategy_entry_values = new CharSequence[stringMatchStrategySize];

        for (int i = 0; i < stringMatchStrategySize; ++i) {
            string_match_strategy_entries[i] = StringMatchStrategy.getStrategyName(this.getActivity(), StringMatchStrategy.STRATEGY_LIST[i]);
            string_match_strategy_entry_values[i] = StringMatchStrategy.getStrategyPrefValue(StringMatchStrategy.STRATEGY_LIST[i]);
        }

        ((ListPreference) findPreference(StringMatchStrategy.PREF_NAME)).setEntries(string_match_strategy_entries);
        ((ListPreference) findPreference(StringMatchStrategy.PREF_NAME)).setEntryValues(string_match_strategy_entry_values);

        findPreference("pref_default_assist_app").setOnPreferenceClickListener(
                preference -> {
                    // Not a perfect behavior, main window disappears
                    // This config is not to be used everyday, it is enough if just not too confusing
                    ((PreferencesActivity) PreferencesFragment.this.getActivity()).setComingBackFlag();
                    Intent intent = new Intent(ACTION_VOICE_INPUT_SETTINGS);
                    PreferencesFragment.this.startActivity(intent);
                    return true;
                }
        );

        if (findPreference("pref_settings_export") != null) {
            findPreference("pref_settings_export").setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.setType("application/json");
                intent.putExtra(Intent.EXTRA_TITLE, "bluelineconsole_settings.json");
                try {
                    ((PreferencesActivity) PreferencesFragment.this.getActivity()).setComingBackFlag();
                    PreferencesFragment.this.startActivityForResult(intent, PreferencesActivity.PREF_EXPORT_REQUEST_CODE);
                    return true;
                } catch (Exception exception) {
                    exception.printStackTrace();
                    return false;
                }
            });
        }

        if (findPreference("pref_settings_import") != null) {
            findPreference("pref_settings_import").setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.setType("application/json");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                try {
                    ((PreferencesActivity) PreferencesFragment.this.getActivity()).setComingBackFlag();
                    PreferencesFragment.this.startActivityForResult(intent, PreferencesActivity.PREF_IMPORT_REQUEST_CODE);
                    return true;
                } catch (Exception exception) {
                    exception.printStackTrace();
                    return false;
                }
            });
        }

        ((ListPreference) findPreference(AppThemeDirectory.PREF_NAME_THEME)).setEntries(AppThemeDirectory.getThemePreferenceTitles(this.getContext()));
        ((ListPreference) findPreference(AppThemeDirectory.PREF_NAME_THEME)).setEntryValues(AppThemeDirectory.getThemePreferenceKeys());

        findPreference(AppThemeDirectory.PREF_NAME_THEME).setOnPreferenceChangeListener(
                (preference, newValue) -> {
                    LauncherWidgetProvider.updateTheme(PreferencesFragment.this.getContext(), (String)newValue);
                    PreferencesFragment.this.getActivity().finish();
                    return true;
                }
        );

        // Ensure sub-screens keep the parent PreferencesActivity alive by setting the coming-back flag before launching
        if (findPreference("pref_aliases") != null) {
            findPreference("pref_aliases").setOnPreferenceClickListener(preference -> {
                try {
                    ((PreferencesActivity) PreferencesFragment.this.getActivity()).setComingBackFlag();
                    Intent intent = new Intent();
                    intent.setClassName(PreferencesFragment.this.getContext(), "net.nhiroki.bluelineconsole.applicationMain.PreferencesAliasesActivity");
                    PreferencesFragment.this.startActivityForResult(intent, PreferencesActivity.PREF_EXPORT_REQUEST_CODE);
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            });
        }

        if (findPreference("pref_system_commands") != null) {
            findPreference("pref_system_commands").setOnPreferenceClickListener(preference -> {
                try {
                    // Show system commands in a dialog instead of launching an activity to avoid preferences closing
                    Context ctx = PreferencesFragment.this.getContext();
                    if (ctx == null) return false;

                    SharedPreferences prefs = ctx.getSharedPreferences(SystemCommandSearcher.PREFS_NAME, Context.MODE_PRIVATE);
                    final List<SystemCommandSearcher.SystemCommandDef> builtIn = SystemCommandSearcher.getBuiltInCommandDefs();
                    final List<SystemCommandSearcher.SystemCommandDef> appTiles = SystemCommandSearcher.getAppTileCommandDefs(ctx);

                    final List<Object> rows = new java.util.ArrayList<>();
                    rows.add(ctx.getString(R.string.system_cmd_section_builtin));
                    rows.addAll(builtIn);
                    if (!appTiles.isEmpty()) {
                        rows.add(ctx.getString(R.string.system_cmd_section_apps));
                        rows.addAll(appTiles);
                    }

                    ListView listView = new ListView(ctx);
                    final BaseAdapter adapter = new BaseAdapter() {
                        private final int TYPE_HEADER = 0;
                        private final int TYPE_ITEM = 1;
                        @Override public int getCount() { return rows.size(); }
                        @Override public Object getItem(int pos) { return rows.get(pos); }
                        @Override public long getItemId(int pos) { return pos; }
                        @Override public int getViewTypeCount() { return 2; }
                        @Override public int getItemViewType(int pos) { return rows.get(pos) instanceof String ? TYPE_HEADER : TYPE_ITEM; }
                        @Override public boolean isEnabled(int pos) { return rows.get(pos) instanceof SystemCommandSearcher.SystemCommandDef; }
                        @Override
                        public android.view.View getView(int pos, android.view.View convertView, android.view.ViewGroup parent) {
                            Object row = rows.get(pos);
                            if (row instanceof String) {
                                TextView tv = convertView instanceof TextView ? (TextView) convertView : new TextView(ctx);
                                tv.setText((String) row);
                                tv.setTypeface(null, Typeface.BOLD);
                                tv.setPadding(32, 28, 32, 8);
                                tv.setTextSize(12);
                                tv.setAllCaps(true);
                                return tv;
                            }
                            SystemCommandSearcher.SystemCommandDef def = (SystemCommandSearcher.SystemCommandDef) row;
                            CheckedTextView ctv = convertView instanceof CheckedTextView ? (CheckedTextView) convertView : (CheckedTextView) LayoutInflater.from(ctx).inflate(android.R.layout.simple_list_item_multiple_choice, parent, false);
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

                    // Build a container view with Select All / Deselect All buttons + the list
                    android.widget.LinearLayout container = new android.widget.LinearLayout(ctx);
                    container.setOrientation(android.widget.LinearLayout.VERTICAL);

                    android.widget.LinearLayout btnRow = new android.widget.LinearLayout(ctx);
                    btnRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);

                    android.widget.Button selectAll = new android.widget.Button(ctx);
                    selectAll.setText(ctx.getString(R.string.select_all));
                    selectAll.setOnClickListener(v -> {
                        for (Object r : rows) {
                            if (r instanceof SystemCommandSearcher.SystemCommandDef) {
                                SystemCommandSearcher.SystemCommandDef d = (SystemCommandSearcher.SystemCommandDef) r;
                                prefs.edit().putBoolean("syscmd_" + d.id, true).apply();
                            }
                        }
                        adapter.notifyDataSetChanged();
                    });

                    android.widget.Button deselectAll = new android.widget.Button(ctx);
                    deselectAll.setText(ctx.getString(R.string.deselect_all));
                    deselectAll.setOnClickListener(v -> {
                        for (Object r : rows) {
                            if (r instanceof SystemCommandSearcher.SystemCommandDef) {
                                SystemCommandSearcher.SystemCommandDef d = (SystemCommandSearcher.SystemCommandDef) r;
                                prefs.edit().putBoolean("syscmd_" + d.id, false).apply();
                            }
                        }
                        adapter.notifyDataSetChanged();
                    });

                    btnRow.addView(selectAll, new android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                    btnRow.addView(deselectAll, new android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

                    container.addView(btnRow);
                    container.addView(listView, new android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));

                    new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.preferences_item_system_commands_title)
                            .setView(container)
                            .setNegativeButton(android.R.string.cancel, null)
                            .show();

                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            });
        }

        if (Build.VERSION.SDK_INT < 24) {
            findPreference(EditTextConfigurations.PREF_KEY_MAIN_EDITTEXT_HINT_LOCALE_ENGLISH).setVisible(false);
        }
    }
}
