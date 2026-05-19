package net.nhiroki.bluelineconsole.commandSearchers.lib;

import android.content.Context;

import net.nhiroki.bluelineconsole.dataStore.persistent.AliasDatabase;
import net.nhiroki.bluelineconsole.plugin.IntentSpec;
import android.content.Intent;
import android.widget.Toast;

import java.util.List;

/**
 * Interceptor that expands user-configured keyword aliases from AliasDatabase.
 * Priority set to 100 so it runs after any very-high-priority interceptors.
 */
public class AliasDatabaseInterceptor implements AliasInterceptor {
    @Override
    public int getPriority() { return 100; }

    @Override
    public InterceptResult intercept(Context context, String query) {
        if (query == null) return InterceptResult.pass();
        String q = query.trim();
        if (q.isEmpty()) return InterceptResult.pass();

        try {
            List<AliasDatabase.Alias> aliases = new AliasDatabase().getAll(context);
            String lower = q.toLowerCase();
            for (AliasDatabase.Alias a : aliases) {
                if (a == null || a.keyword == null) continue;
                String ak = a.keyword.toLowerCase();
                if (lower.equals(ak) || lower.startsWith(ak + " ")) {
                    String rest = "";
                    if (q.length() > a.keyword.length()) {
                        rest = q.substring(a.keyword.length()).trim();
                    }
                    if ("app".equals(a.type)) {
                        // rewrite to package name + rest so existing app searchers match
                        String replaced = a.target + (rest.isEmpty() ? "" : " " + rest);
                        return InterceptResult.handled(replaced);
                    } else if ("url".equals(a.type)) {
                        if (a.target.contains("{query}")) {
                            return InterceptResult.handled(a.target.replace("{query}", rest));
                        }
                        String replaced = a.target + (rest.isEmpty() ? "" : " " + rest);
                        return InterceptResult.handled(replaced);
                    } else if ("intent".equals(a.type)) {
                        // For intent aliases, prepare a payload so a dedicated searcher can show an actionable candidate.
                        try {
                            if (a.intentSpecJson != null) {
                                // Ensure spec contains a {query} extra if none present
                                IntentSpec spec = IntentSpec.fromJson(a.intentSpecJson);
                                if (spec != null) {
                                    boolean hasQueryExtra = false;
                                    if (spec.extras != null) {
                                        for (IntentSpec.Extra e : spec.extras) {
                                            if (e != null && e.value != null && e.value.contains("{query}")) { hasQueryExtra = true; break; }
                                        }
                                    }
                                    if (!hasQueryExtra) {
                                        IntentSpec.Extra ex = new IntentSpec.Extra();
                                        ex.key = "query";
                                        ex.type = "string";
                                        ex.value = "{query}";
                                        spec.extras.add(ex);
                                    }
                                    // Build JSON payload containing spec and rest, base64-encode to avoid special chars
                                    org.json.JSONObject payload = new org.json.JSONObject();
                                    payload.put("spec", spec.toJson().toString());
                                    payload.put("rest", rest == null ? "" : rest);
                                    String enc = android.util.Base64.encodeToString(payload.toString().getBytes("UTF-8"), android.util.Base64.NO_WRAP);
                                    return InterceptResult.handled("intentalias:" + enc);
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return InterceptResult.pass();
    }
}
