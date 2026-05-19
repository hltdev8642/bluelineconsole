package net.nhiroki.bluelineconsole.commandSearchers.eachSearcher;

import android.content.Context;
import android.content.Intent;
import android.util.Base64;

import net.nhiroki.bluelineconsole.interfaces.CandidateEntry;
import net.nhiroki.bluelineconsole.interfaces.CommandSearcher;
import net.nhiroki.bluelineconsole.interfaces.EventLauncher;
import net.nhiroki.bluelineconsole.plugin.IntentSpec;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

public class IntentAliasCommandSearcher implements CommandSearcher {
    private static final String PREFIX = "intentalias:";

    @Override
    public void refresh(Context context) { }

    @Override
    public void close() { }

    @Override
    public boolean isPrepared() { return true; }

    @Override
    public void waitUntilPrepared() { }

    @Override
    public List<CandidateEntry> searchCandidateEntries(String query, Context context) {
        List<CandidateEntry> results = new ArrayList<>();
        if (query == null || !query.startsWith(PREFIX)) return results;
        try {
            String payload = query.substring(PREFIX.length());
            byte[] decoded = Base64.decode(payload, Base64.DEFAULT);
            String json = new String(decoded, "UTF-8");
            JSONObject o = new JSONObject(json);
            String specJson = o.optString("spec", null);
            String rest = o.optString("rest", null);
            IntentSpec spec = IntentSpec.fromJson(specJson);
            if (spec == null) return results;
            String title = spec.action != null ? spec.action : (spec.componentPackage != null ? spec.componentPackage : "Intent");

            final IntentSpec fSpec = spec;
            final String fRest = rest;

            results.add(new CandidateEntry() {
                @Override public String getTitle() { return title; }
                @Override public android.view.View getView(net.nhiroki.bluelineconsole.applicationMain.MainActivity mainActivity) { return null; }
                @Override public boolean hasLongView() { return false; }
                @Override public EventLauncher getEventLauncher(Context context) {
                    return activity -> {
                        try {
                            Intent intent = fSpec.toIntent(activity, fRest);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            activity.startActivity(intent);
                        } catch (Exception e) { e.printStackTrace(); }
                    };
                }
                @Override public android.graphics.drawable.Drawable getIcon(Context context) { return null; }
                @Override public boolean hasEvent() { return true; }
                @Override public boolean isSubItem() { return false; }
                @Override public boolean viewIsRecyclable() { return true; }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }
}
