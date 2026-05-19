package net.nhiroki.bluelineconsole.applicationMain;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Overlay receiver deprecated — receiver removed from manifest to reduce footprint.
 * Kept as a minimal stub for compatibility; it no longer performs actions.
 */
@Deprecated
public class OverlayIntentReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // No-op: overlay feature removed to conserve resources.
    }
}
