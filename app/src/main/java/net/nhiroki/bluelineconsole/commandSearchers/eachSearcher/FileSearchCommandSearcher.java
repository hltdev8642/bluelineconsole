package net.nhiroki.bluelineconsole.commandSearchers.eachSearcher;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import net.nhiroki.bluelineconsole.applicationMain.MainActivity;
import net.nhiroki.bluelineconsole.interfaces.CandidateEntry;
import net.nhiroki.bluelineconsole.interfaces.CommandSearcher;
import net.nhiroki.bluelineconsole.interfaces.ContextAction;
import net.nhiroki.bluelineconsole.interfaces.ContextActionProvider;
import net.nhiroki.bluelineconsole.interfaces.EventLauncher;

import java.util.ArrayList;
import java.util.List;

public class FileSearchCommandSearcher implements CommandSearcher {

    @Override public void refresh(Context context) {}
    @Override public void close() {}
    @Override public boolean isPrepared() { return true; }
    @Override public void waitUntilPrepared() {}

    @Override
    @NonNull
    public List<CandidateEntry> searchCandidateEntries(String query, Context context) {
        List<CandidateEntry> candidates = new ArrayList<>();
        String lq = query.toLowerCase().trim();

        String searchTerm = null;
        if (lq.startsWith("file:")) {
            searchTerm = query.substring(5).trim();
        } else if (lq.startsWith("f:")) {
            searchTerm = query.substring(2).trim();
        }

        if (searchTerm == null || searchTerm.isEmpty()) return candidates;

        try {
            Uri uri = MediaStore.Files.getContentUri("external");
            String[] projection = {
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.MIME_TYPE,
            };
            String selection = MediaStore.Files.FileColumns.DISPLAY_NAME + " LIKE ?";
            String[] selectionArgs = { "%" + searchTerm + "%" };
            String sortOrder = MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC";

            Cursor cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder);
            if (cursor != null) {
                int nameCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME);
                int idCol = cursor.getColumnIndex(MediaStore.Files.FileColumns._ID);
                int mimeCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE);

                int count = 0;
                while (cursor.moveToNext() && count < 10) {
                    String name = nameCol >= 0 ? cursor.getString(nameCol) : "Unknown";
                    long id = idCol >= 0 ? cursor.getLong(idCol) : 0;
                    String mime = mimeCol >= 0 ? cursor.getString(mimeCol) : "*/*";

                    Uri fileUri = Uri.withAppendedPath(uri, String.valueOf(id));
                    candidates.add(new FileCandidateEntry(name, fileUri, mime));
                    count++;
                }
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return candidates;
    }

    private static class FileCandidateEntry implements CandidateEntry, ContextActionProvider {
        private final String name;
        private final Uri fileUri;
        private final String mimeType;

        FileCandidateEntry(String name, Uri uri, String mimeType) {
            this.name = name;
            this.fileUri = uri;
            this.mimeType = mimeType != null ? mimeType : "*/*";
        }

        @Override public String getTitle() { return name; }
        @Override public View getView(MainActivity a) { return null; }
        @Override public boolean hasLongView() { return false; }
        @Override public boolean hasEvent() { return true; }
        @Override public Drawable getIcon(Context c) { return null; }
        @Override public boolean isSubItem() { return false; }
        @Override public boolean viewIsRecyclable() { return true; }

        @Override
        public EventLauncher getEventLauncher(Context context) {
            return activity -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(fileUri, mimeType);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    activity.startActivity(intent);
                    activity.finishIfNotHome();
                } catch (Exception e) {
                    Toast.makeText(activity,
                        "Cannot open file: " + name, Toast.LENGTH_SHORT).show();
                }
            };
        }

        @Override
        public List<ContextAction> getContextActions(Context context) {
            List<ContextAction> actions = new ArrayList<>();

            actions.add(new ContextAction("Open", activity -> {
                EventLauncher el = getEventLauncher(context);
                if (el != null) {
                    try {
                        el.launch((MainActivity) activity);
                    } catch (ClassCastException e) {
                        // fallback if activity is not MainActivity
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(fileUri, mimeType);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                        try { activity.startActivity(intent); } catch (Exception ignored) {}
                    }
                }
            }));

            actions.add(new ContextAction("Open with…", activity -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(fileUri, mimeType);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                Intent chooser = Intent.createChooser(intent, "Open with");
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(chooser);
            }));

            actions.add(new ContextAction("Copy URI", activity -> {
                ClipboardManager clipboardManager = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboardManager != null) {
                    clipboardManager.setPrimaryClip(ClipData.newPlainText("file-uri", fileUri.toString()));
                    Toast.makeText(activity, "Copied", Toast.LENGTH_SHORT).show();
                }
            }));

            actions.add(new ContextAction("Share", activity -> {
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType(mimeType != null ? mimeType : "*/*");
                share.putExtra(Intent.EXTRA_STREAM, fileUri);
                share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                Intent chooser = Intent.createChooser(share, "Share");
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(chooser);
            }));

            // Add apps that can handle this file type as separate actions
            try {
                Intent probe = new Intent(Intent.ACTION_VIEW);
                probe.setDataAndType(fileUri, mimeType);
                List<ResolveInfo> resolveInfos = context.getPackageManager().queryIntentActivities(probe, 0);
                for (ResolveInfo ri : resolveInfos) {
                    String label = ri.loadLabel(context.getPackageManager()).toString();
                    actions.add(new ContextAction("Open with: " + label, activity -> {
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setDataAndType(fileUri, mimeType);
                        i.setPackage(ri.activityInfo.packageName);
                        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                        try { activity.startActivity(i); } catch (Exception ignored) {}
                    }));
                }
            } catch (Exception ignored) {}

            return actions;
        }
    }
}
