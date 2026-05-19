package net.nhiroki.bluelineconsole.commandSearchers.lib;

import android.content.Context;

/**
 * Interceptor interface for query transformations, modeled after typical "interceptor" patterns.
 * Implementations can inspect and modify the incoming query. Interceptors with lower priority
 * values are applied first.
 */
public interface AliasInterceptor {
    /**
     * Priority controls ordering; lower values run earlier.
     */
    int getPriority();

    /**
     * Inspect and possibly transform the input query.
     * @param context Android context
     * @param query the incoming query
     * @return InterceptResult describing whether the interceptor handled the query and an optional replacement
     */
    InterceptResult intercept(Context context, String query);

    class InterceptResult {
        public final boolean handled; // if true, stop processing further interceptors
        public final String replacement; // if non-null, use replacement as new query

        public InterceptResult(boolean handled, String replacement) {
            this.handled = handled;
            this.replacement = replacement;
        }

        public static InterceptResult pass() { return new InterceptResult(false, null); }
        public static InterceptResult replace(String newQuery) { return new InterceptResult(false, newQuery); }
        public static InterceptResult handled(String newQuery) { return new InterceptResult(true, newQuery); }
    }
}
