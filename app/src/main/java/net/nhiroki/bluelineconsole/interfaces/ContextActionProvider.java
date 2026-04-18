package net.nhiroki.bluelineconsole.interfaces;

import android.content.Context;
import java.util.List;

public interface ContextActionProvider {
    List<ContextAction> getContextActions(Context context);
}
