package net.nhiroki.bluelineconsole.commandSearchers.lib;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Simple registry for AliasInterceptor instances. Can be extended to support dynamic
 * registration by plugins. For now it provides a place to register built-in interceptors.
 */
public class AliasInterceptorManager {
    private static final List<AliasInterceptor> interceptors = new ArrayList<>();
    private static volatile boolean initialized = false;

    private static void ensureInitialized(Context context) {
        if (initialized) return;
        synchronized (interceptors) {
            if (initialized) return;
            // Register built-in interceptors here. Keep this minimal.
            interceptors.add(new AliasDatabaseInterceptor());

            // Sort by priority
            Collections.sort(interceptors, Comparator.comparingInt(AliasInterceptor::getPriority));
            initialized = true;
        }
    }

    public static String applyInterceptors(Context context, String query) {
        ensureInitialized(context);
        String current = query;
        for (AliasInterceptor it : interceptors) {
            try {
                AliasInterceptor.InterceptResult r = it.intercept(context, current);
                if (r == null) continue;
                if (r.replacement != null) current = r.replacement;
                if (r.handled) return current;
            } catch (Throwable t) {
                // never let interceptor crash search
                t.printStackTrace();
            }
        }
        return current;
    }

    /**
     * Allows registering extra interceptors (e.g., by plugins). New interceptors are inserted
     * and the list is re-sorted.
     */
    public static void registerInterceptor(AliasInterceptor interceptor) {
        synchronized (interceptors) {
            interceptors.add(interceptor);
            Collections.sort(interceptors, Comparator.comparingInt(AliasInterceptor::getPriority));
        }
    }
}
