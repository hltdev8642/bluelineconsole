package net.nhiroki.bluelineconsole.commandSearchers.eachSearcher;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.hardware.camera2.CameraManager;
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

    public static final String PREFS_NAME = "blc_system_commands";
    private static boolean torchOn = false;
    private static List<SystemCommandDef> cachedBuiltIn = null;
    private static List<SystemCommandDef> cachedAppTiles = null;

    public static class SystemCommandDef {
        public final String id;
        public final String title;
        public final String[] keywords;
        public final boolean isBuiltIn;
        final EventLauncher action;

        public SystemCommandDef(String id, String title, String[] keywords, boolean isBuiltIn, EventLauncher action) {
            this.id = id;
            this.title = title;
            this.keywords = keywords;
            this.isBuiltIn = isBuiltIn;
            this.action = action;
        }
    }

    @NonNull
    public static List<SystemCommandDef> getBuiltInCommandDefs() {
        if (cachedBuiltIn != null) return cachedBuiltIn;
        List<SystemCommandDef> list = new ArrayList<>();

        list.add(def("wifi", "WiFi Settings", new String[]{"wifi", "wi-fi", "wireless", "internet"}, activity -> {
            Intent i = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                    ? new Intent(Settings.Panel.ACTION_WIFI)
                    : new Intent(Settings.ACTION_WIFI_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(i);
            activity.finishIfNotHome();
        }));

        list.add(def("bluetooth", "Bluetooth Settings", new String[]{"bluetooth", "bt"}, activity -> {
            Intent i = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(i);
            activity.finishIfNotHome();
        }));

        list.add(def("volume", "Adjust Volume", new String[]{"volume", "sound", "ringer", "audio"}, activity -> {
            Intent i = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                    ? new Intent(Settings.Panel.ACTION_VOLUME)
                    : new Intent(Settings.ACTION_SOUND_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(i);
            activity.finishIfNotHome();
        }));

        list.add(def("brightness", "Adjust Brightness", new String[]{"brightness", "display", "screen"}, activity -> {
            Intent i = new Intent(Settings.ACTION_DISPLAY_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(i);
            activity.finishIfNotHome();
        }));

        list.add(def("airplane", "Airplane Mode", new String[]{"airplane", "flight mode", "aeroplane", "plane"}, activity -> {
            Intent i = new Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(i);
            activity.finishIfNotHome();
        }));

        list.add(def("flashlight", "Toggle Flashlight", new String[]{"flashlight", "torch", "flash"}, activity -> {
            try {
                CameraManager cm = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
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

        list.add(def("dnd", "Do Not Disturb", new String[]{"do not disturb", "dnd", "silent", "mute", "quiet"}, activity -> {
            Intent i = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(i);
            activity.finishIfNotHome();
        }));

        list.add(def("mobile_data", "Mobile Data / Internet", new String[]{"mobile data", "cellular", "cell data", "mobile network"}, activity -> {
            Intent i = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                    ? new Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
                    : new Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(i);
            activity.finishIfNotHome();
        }));

        list.add(def("hotspot", "Hotspot & Tethering", new String[]{"hotspot", "tethering", "wifi hotspot", "personal hotspot"}, activity -> {
            Intent i = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(i);
            activity.finishIfNotHome();
        }));

        list.add(def("location", "Location / GPS", new String[]{"location", "gps", "location services"}, activity -> {
            Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(i);
            activity.finishIfNotHome();
        }));

        list.add(def("nfc", "NFC Settings", new String[]{"nfc", "near field", "contactless"}, activity -> {
            Intent i = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                    ? new Intent(Settings.Panel.ACTION_NFC)
                    : new Intent(Settings.ACTION_NFC_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(i);
            activity.finishIfNotHome();
        }));

        list.add(def("nfc_pay", "NFC Payment / Wallet", new String[]{"wallet", "nfc pay", "tap to pay", "payment"}, activity -> {
            Intent i = new Intent(Settings.ACTION_NFC_PAYMENT_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(i);
            activity.finishIfNotHome();
        }));

        list.add(def("auto_rotate", "Screen Rotation", new String[]{"rotate", "rotation", "auto rotate", "screen rotation", "orientation"}, activity -> {
            Intent i = new Intent(Settings.ACTION_DISPLAY_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(i);
            activity.finishIfNotHome();
        }));

        list.add(def("battery_saver", "Battery Saver", new String[]{"battery saver", "power saver", "low power", "battery"}, activity -> {
            Intent i = new Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(i);
            activity.finishIfNotHome();
        }));

        list.add(def("dark_mode", "Dark Mode / Display Theme", new String[]{"dark mode", "night mode", "dark theme"}, activity -> {
            Intent i = new Intent(Settings.ACTION_DISPLAY_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(i);
            activity.finishIfNotHome();
        }));

        list.add(def("cast", "Cast / Screen Mirror", new String[]{"cast", "screen cast", "screen mirror", "miracast"}, activity -> {
            Intent i = new Intent(Settings.ACTION_CAST_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(i);
            activity.finishIfNotHome();
        }));

        list.add(def("sync", "Sync Settings", new String[]{"sync", "auto sync", "synchronize"}, activity -> {
            Intent i = new Intent(Settings.ACTION_SYNC_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(i);
            activity.finishIfNotHome();
        }));

        list.add(def("data_usage", "Data Usage / Data Saver", new String[]{"data usage", "data saver", "background data"}, activity -> {
            Intent i = new Intent(Settings.ACTION_DATA_ROAMING_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(i);
            activity.finishIfNotHome();
        }));

        list.add(def("accessibility", "Accessibility Settings", new String[]{"accessibility", "talkback", "screen reader"}, activity -> {
            Intent i = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(i);
            activity.finishIfNotHome();
        }));

        list.add(def("vpn", "VPN Settings", new String[]{"vpn", "virtual private network"}, activity -> {
            Intent i = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                    ? new Intent(Settings.ACTION_VPN_SETTINGS)
                    : new Intent(Settings.ACTION_WIRELESS_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(i);
            activity.finishIfNotHome();
        }));

        list.add(def("notifications", "Notification Settings", new String[]{"notifications", "notification settings", "alerts"}, activity -> {
            Intent i = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    ? new Intent("android.settings.NOTIFICATION_SETTINGS")
                    : new Intent(Settings.ACTION_SOUND_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(i);
            activity.finishIfNotHome();
        }));

        list.add(def("privacy", "Privacy Settings", new String[]{"privacy", "privacy settings", "permissions"}, activity -> {
            Intent i = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                    ? new Intent(Settings.ACTION_PRIVACY_SETTINGS)
                    : new Intent(Settings.ACTION_SECURITY_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(i);
            activity.finishIfNotHome();
        }));

        list.add(def("security", "Security Settings", new String[]{"security", "screen lock", "lock screen", "fingerprint", "biometric", "face unlock"}, activity -> {
            Intent i = new Intent(Settings.ACTION_SECURITY_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(i);
            activity.finishIfNotHome();
        }));

        list.add(def("storage", "Storage Settings", new String[]{"storage", "disk space", "internal storage"}, activity -> {
            Intent i = new Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(i);
            activity.finishIfNotHome();
        }));

        list.add(def("language", "Language & Input", new String[]{"language", "locale", "region", "input language"}, activity -> {
            Intent i = new Intent(Settings.ACTION_LOCALE_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(i);
            activity.finishIfNotHome();
        }));

        list.add(def("datetime", "Date & Time Settings", new String[]{"date time", "date and time", "time settings", "time zone"}, activity -> {
            Intent i = new Intent(Settings.ACTION_DATE_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(i);
            activity.finishIfNotHome();
        }));

        list.add(def("developer", "Developer Options", new String[]{"developer", "dev options", "developer options"}, activity -> {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(i);
            activity.finishIfNotHome();
        }));

        list.add(def("apps", "Manage Applications", new String[]{"app manager", "manage apps", "installed apps", "applications"}, activity -> {
            Intent i = new Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(i);
            activity.finishIfNotHome();
        }));

        list.add(def("network", "Mobile Network / SIM", new String[]{"mobile network", "network settings", "sim", "carrier"}, activity -> {
            Intent i = new Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(i);
            activity.finishIfNotHome();
        }));

        list.add(def("screensaver", "Screensaver / Daydream", new String[]{"screensaver", "screen saver", "daydream"}, activity -> {
            Intent i = new Intent(Settings.ACTION_DREAM_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(i);
            activity.finishIfNotHome();
        }));

        list.add(def("home_settings", "Home App Settings", new String[]{"home settings", "launcher settings", "home app"}, activity -> {
            Intent i = new Intent(Settings.ACTION_HOME_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(i);
            activity.finishIfNotHome();
        }));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            list.add(def("default_apps", "Default Apps", new String[]{"default apps", "default applications", "defaults"}, activity -> {
                Intent i = new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(i);
                activity.finishIfNotHome();
            }));
        }

        cachedBuiltIn = list;
        return list;
    }

    @NonNull
    public static List<SystemCommandDef> getAppTileCommandDefs(Context context) {
        if (cachedAppTiles != null) return cachedAppTiles;
        List<SystemCommandDef> list = new ArrayList<>();
        try {
            PackageManager pm = context.getPackageManager();
            Intent tileIntent = new Intent("android.service.quicksettings.action.QS_TILE");
            List<ResolveInfo> services = pm.queryIntentServices(tileIntent, 0);
            for (ResolveInfo info : services) {
                String pkg = info.serviceInfo.packageName;
                String svcName = info.serviceInfo.name;
                String label = info.loadLabel(pm).toString();
                // Stable ID: use package + service class
                String id = "apptile_" + pkg + "/" + svcName;
                String keyword = label.toLowerCase();
                list.add(new SystemCommandDef(id, label + " (Tile)", new String[]{keyword}, false, activity -> {
                    Intent launchIntent = pm.getLaunchIntentForPackage(pkg);
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        activity.startActivity(launchIntent);
                        activity.finishIfNotHome();
                    }
                }));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        cachedAppTiles = list;
        return list;
    }

    private static SystemCommandDef def(String id, String title, String[] keywords, EventLauncher action) {
        return new SystemCommandDef(id, title, keywords, true, action);
    }

    @Override
    public void refresh(Context context) {
        cachedAppTiles = null; // re-discover after app installs/uninstalls
    }

    @Override public void close() {}
    @Override public boolean isPrepared() { return true; }
    @Override public void waitUntilPrepared() {}

    @Override
    @NonNull
    public List<CandidateEntry> searchCandidateEntries(String query, Context context) {
        List<CandidateEntry> candidates = new ArrayList<>();
        String q = query.toLowerCase().trim();
        if (q.length() < 2) return candidates;

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        for (SystemCommandDef cmd : getBuiltInCommandDefs()) {
            if (!prefs.getBoolean("syscmd_" + cmd.id, true)) continue;
            if (keywordMatches(q, cmd.keywords)) {
                candidates.add(new SystemEntry(cmd.title, cmd.action));
            }
        }
        for (SystemCommandDef cmd : getAppTileCommandDefs(context)) {
            if (!prefs.getBoolean("syscmd_" + cmd.id, true)) continue;
            if (keywordMatches(q, cmd.keywords)) {
                candidates.add(new SystemEntry(cmd.title, cmd.action));
            }
        }
        return candidates;
    }

    private static boolean keywordMatches(String query, String[] keywords) {
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
