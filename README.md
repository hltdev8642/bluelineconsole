## <img src="./fastlane/metadata/android/en-US/images/icon.png" width="32" height="32"> Blue Line Console

A fast, keyboard-driven launcher for Android inspired by Wox / Alfred.  
Type anything — launch apps, search the web, control system settings, set timers, search files, and more.

### Releases

- [Google Play](https://play.google.com/store/apps/details?id=net.nhiroki.bluelineconsole)
- [F-Droid](https://f-droid.org/en/packages/net.nhiroki.bluelineconsole/)

### Screenshots

<img src="./fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" width="240"> <img src="./fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" width="240"> <img src="./fastlane/metadata/android/en-US/images/phoneScreenshots/3.png" width="240">

---

## Features

### App & URL Launcher
- Type part of an app name or package name to launch it
- Type a URL directly to open it in your browser
- Usage-based ranking surfaces your most-used items first

### Calculator
Evaluate math expressions inline:
```
2 + 3 * 5
1 inch in cm
1m + 1inch in cm
```

### Timer & Alarm
```
timer 5          → 5-minute timer
timer 1:30       → 1 min 30 sec timer
timer 1h 30m     → 1.5 hour timer
alarm 7:30       → set alarm at 07:30
alarm 9 am       → set alarm at 09:00
```

### File Search
Search media files on your device:
```
file:photo       → find files named "photo"
f:report         → shorthand syntax
```

### Web Search
```
google android tips
bing blue line console
duckduckgo privacy news
wikipedia kotlin
yahoo weather
```
Names can be abbreviated: `g query`, `d query`, `w query`

### System Shortcuts
Type any of these keywords (or abbreviations) to jump directly to a system setting:

| Keyword | Destination |
|---------|-------------|
| `wifi` | Wi-Fi settings |
| `bluetooth` / `bt` | Bluetooth settings |
| `volume` / `sound` | Volume panel |
| `brightness` / `display` | Display settings |
| `airplane` / `flight mode` | Airplane mode |
| `flashlight` / `torch` | Toggle flashlight |
| `dnd` / `do not disturb` | DND / notification policy |
| `mobile data` / `cellular` | Internet connectivity |
| `hotspot` / `tethering` | Hotspot & tethering |
| `location` / `gps` | Location services |
| `nfc` | NFC settings |
| `wallet` / `tap to pay` | NFC payment |
| `rotate` / `orientation` | Screen rotation |
| `battery saver` | Battery saver |
| `dark mode` / `night mode` | Display / dark theme |
| `cast` / `screen mirror` | Cast settings |
| `sync` | Auto sync |
| `data usage` | Data saver |
| `accessibility` | Accessibility settings |
| `vpn` | VPN settings |
| `notifications` | Notification settings |
| `privacy` | Privacy settings |
| `security` / `fingerprint` | Security settings |
| `storage` | Internal storage |
| `language` / `locale` | Language & input |
| `date time` / `time zone` | Date & time |
| `developer` / `dev options` | Developer options |
| `app manager` / `apps` | Manage applications |
| `sim` / `mobile network` | Mobile network |
| `screensaver` / `daydream` | Screensaver |
| `home settings` | Home app settings |
| `default apps` | Default app settings |

Apps that expose a Quick Settings `TileService` also appear automatically in search.

> Manage which system tiles appear via **config → System Command Tiles**.

### Keyword Aliases
Create short keywords that launch any URL or app activity:

1. Open **config → Keyword Aliases → +**
2. Enter a keyword (e.g. `gh`), display name (e.g. `GitHub`), and target
3. For **URL** type: enter a full URL (`https://github.com`)
4. For **App** type: enter a package name or use **Browse Apps…** to pick any exported activity

The **Browse Apps…** button lets you drill down to any individual activity within an installed app.

### Plugins
Extend search with custom actions (URL open, clipboard copy, text display).  
See **[doc/PLUGINS.md](doc/PLUGINS.md)** for the full plugin format and examples.

Manage plugins via **config → Plugins**.

### Network Utilities
```
ping google.com
ping6 ipv6.google.com
```

### Calendar
```
cal              → current month
cal 12 2025      → December 2025
```

### Contacts
Type a contact name to find and call/message them (requires Contacts permission).

### Color Inspector
Type a hex color code to preview it:
```
#4d68ff
#ff0000
```

---

## Tips

- **Assistant shortcut**: Set Blue Line Console as your Default Assistant App (Settings → Apps → Default Apps → Assistant) to open it with a long-press of the Home button.
- **Notification shortcut**: Enable the persistent notification in **config** to launch from the notification shade.
- **Abbreviate search engines**: `g query` = `google query`, `d query` = `duckduckgo query`, `w query` = `wikipedia query`.
- **System tile toggles**: Disable tiles you don't use via **config → System Command Tiles** to keep results clean.

---

## Building

```bash
./gradlew assembleDebug
```

Requires Android SDK (minSdk 23, targetSdk 36).

---

## Plugin Development

See [doc/PLUGINS.md](doc/PLUGINS.md) for how to create plugins.

