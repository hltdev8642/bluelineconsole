package net.nhiroki.bluelineconsole.commandSearchers.lib;

import android.content.Context;

import net.nhiroki.bluelineconsole.dataStore.persistent.AliasDatabase;
import net.nhiroki.bluelineconsole.commandSearchers.lib.AliasInterceptorManager;

import java.util.List;

public class QueryPreprocessor {
    /**
     * Lightweight preprocessing: normalize whitespace and trim.
     * Also run interceptor pipeline (alias expansion etc.).
     */
    public static String preprocess(Context context, String query) {
        if (query == null) return "";
        // collapse multiple spaces and trim
        String q = query.replaceAll("\\s+", " ").trim();

        if (q.isEmpty()) return q;

        // Let interceptors inspect/transform the query. Interceptors may stop further processing.
        try {
            String r = AliasInterceptorManager.applyInterceptors(context, q);
            if (r != null && !r.equals(q)) return r;
        } catch (Exception ignored) {}

        return q;
    }
}
