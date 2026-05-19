package net.nhiroki.bluelineconsole.plugin;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import net.nhiroki.bluelineconsole.interfaces.EventLauncher;

public class PluginHost {
    public static EventLauncher createLauncherFromDefinition(PluginDefinition.ActionDef action, String parameter) {
        if (action == null) return null;

        switch (action.type) {
            case "url":
                return activity -> {
                    try {
                        String url = action.template.replace("{query}", Uri.encode(parameter == null ? "" : parameter));
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        activity.startActivity(intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                };
            case "copy":
                return activity -> {
                    ClipboardManager clipboardManager = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clipboardManager != null) {
                        String text = action.template.replace("{query}", parameter == null ? "" : parameter);
                        clipboardManager.setPrimaryClip(ClipData.newPlainText("plugin", text));
                    }
                };
            case "intent":
                return activity -> {
                    try {
                        String uriStr = action.template.replace("{query}", parameter == null ? "" : parameter);
                        Intent intent = Intent.parseUri(uriStr, Intent.URI_INTENT_SCHEME);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        activity.startActivity(intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                };
            case "text":
            default:
                return activity -> {
                    String text = action.template.replace("{query}", parameter == null ? "" : parameter);
                    Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
                };
        }
    }
}
