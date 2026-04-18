package net.nhiroki.bluelineconsole.commandSearchers.eachSearcher;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.view.View;

import androidx.annotation.NonNull;

import net.nhiroki.bluelineconsole.applicationMain.MainActivity;
import net.nhiroki.bluelineconsole.interfaces.CandidateEntry;
import net.nhiroki.bluelineconsole.interfaces.CommandSearcher;
import net.nhiroki.bluelineconsole.interfaces.EventLauncher;

import java.util.ArrayList;
import java.util.List;

public class SystemCommandSearcher implements CommandSearcher {

    private static boolean torchOn = false;

    @Override public void refresh(Context context) {}
    @Override public void close() {}
    @Override public boolean isPrepared() { return true; }
    @Override public void waitUntilPrepared() {}

    @Override
    @NonNull
    public List<CandidateEntry> searchCandidateEntries(String query, Context context) {
        List<CandidateEntry> candidates = new ArrayList<>();
        String q = query.toLowerCase().trim();
        if (q.isEmpty()) return candidates;

        if (keywordMatches(q, "wifi", "wi-fi", "wireless", "internet")) {
            candidates.add(new SystemEntry("Toggle WiFi", activity -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    Intent intent = new Intent(Settings.Panel.ACTION_WIFI);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    activity.startActivity(intent);
                } else {
                    WifiManager wm = (WifiManager) activity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    if (wm != null) {
                        //noinspection deprecation
                        wm.setWifiEnabled(!wm.isWifiEnabled());
                    }
                }
                activity.finishIfNotHome();
            }));
        }

        if (keywordMatches(q, "bluetooth", "bt")) {
            candidates.add(new SystemEntry("Toggle Bluetooth", activity -> {
                Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(intent);
                activity.finishIfNotHome();
            }));
        }

        if (keywordMatches(q, "brightness", "display", "screen")) {
            candidates.add(new SystemEntry("Adjust Brightness", activity -> {
                Intent intent = new Intent(Settings.ACTION_DISPLAY_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(intent);
                activity.finishIfNotHome();
            }));
        }

        if (keywordMatches(q, "volume", "sound", "ringer", "audio")) {
            candidates.add(new SystemEntry("Adjust Volume", activity -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    Intent intent = new Intent(Settings.Panel.ACTION_VOLUME);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    activity.startActivity(intent);
                } else {
                    Intent intent = new Intent(Settings.ACTION_SOUND_SETTINGS);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    activity.startActivity(intent);
                }
                activity.finishIfNotHome();
            }));
        }

        if (keywordMatches(q, "airplane", "flight mode", "aeroplane", "plane")) {
            candidates.add(new SystemEntry("Airplane Mode Settings", activity -> {
                Intent intent = new Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(intent);
                activity.finishIfNotHome();
            }));
        }

        if (keywordMatches(q, "flashlight", "torch", "flash")) {
            candidates.add(new SystemEntry("Toggle Flashlight", activity -> {
                try {
                    android.hardware.camera2.CameraManager cm =
                        (android.hardware.camera2.CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
                    if (cm != null) {
                        String[] ids = cm.getCameraIdList();
                        if (ids.length > 0) {
                            torchOn = !torchOn;
                            cm.setTorchMode(ids[0], torchOn);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }));
        }

        if (keywordMatches(q, "do not disturb", "dnd", "silent", "mute", "quiet")) {
            candidates.add(new SystemEntry("Do Not Disturb Settings", activity -> {
                Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(intent);
                activity.finishIfNotHome();
            }));
        }

        return candidates;
    }

    private boolean keywordMatches(String query, String... keywords) {
        for (String kw : keywords) {
            if (kw.startsWith(query) || kw.contains(query)) return true;
        }
        return false;
    }

    private static class SystemEntry implements CandidateEntry {
        private final String title;
        private final EventLauncher launcher;

        SystemEntry(String title, EventLauncher launcher) {
            this.title = title;
            this.launcher = launcher;
        }

        @Override public String getTitle() { return title; }
        @Override public View getView(MainActivity a) { return null; }
        @Override public boolean hasLongView() { return false; }
        @Override public boolean hasEvent() { return true; }
        @Override public EventLauncher getEventLauncher(Context c) { return launcher; }
        @Override public Drawable getIcon(Context c) { return null; }
        @Override public boolean isSubItem() { return false; }
        @Override public boolean viewIsRecyclable() { return true; }
    }
}
