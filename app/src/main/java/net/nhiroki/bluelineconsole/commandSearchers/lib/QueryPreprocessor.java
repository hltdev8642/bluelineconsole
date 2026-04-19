package net.nhiroki.bluelineconsole.commandSearchers.lib;

import android.content.Context;

public class QueryPreprocessor {
    /**
     * Lightweight preprocessing: normalize whitespace and trim.
     * Future: expand aliases, resolve prefixes, pipeline parsing.
     */
    public static String preprocess(Context context, String query) {
        if (query == null) return "";
        // collapse multiple spaces and trim
        return query.replaceAll("\\s+", " ").trim();
    }
}
