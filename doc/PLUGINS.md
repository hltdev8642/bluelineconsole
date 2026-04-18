# Blue Line Console — Plugin Development Guide

Plugins extend search with custom actions. Each plugin is a single JSON file placed in the app's `plugins/` directory (managed via **config → Plugins**).

---

## Plugin File Format

```json
{
  "id": "my_plugin",
  "displayName": "My Plugin",
  "patterns": [
    { "type": "prefix", "value": "mp " }
  ],
  "action": {
    "type": "url",
    "template": "https://example.com/search?q={query}"
  }
}
```

### Top-level fields

| Field | Required | Description |
|-------|----------|-------------|
| `id` | No | Unique identifier (defaults to filename). |
| `displayName` | No | Name shown in search results (defaults to `id`). |
| `patterns` | No | Array of match rules. Defaults to `[{"type":"always"}]`. |
| `action` | Yes | What happens when the result is tapped. |

---

## Patterns

Each pattern object has a `type` and optional `value`. The plugin appears in results when **any** pattern matches the current query.

### `prefix`
Matches when the query starts with `value`. The text after the prefix is passed to the action as `{query}`.

```json
{ "type": "prefix", "value": "yt " }
```
Query `yt funny cats` → `{query}` = `funny cats`

### `contains`
Matches when the query contains `value` anywhere. The full query is passed as `{query}`.

```json
{ "type": "contains", "value": "@" }
```
Query `user@example.com` → `{query}` = `user@example.com`

### `regex`
Matches using a Java regular expression. If the pattern has a capture group, the first capture group is passed as `{query}`; otherwise the full query is used.

```json
{ "type": "regex", "value": "^(\\d{10,13})$" }
```
Query `9781234567890` → `{query}` = `9781234567890`

### `always`
Always matches. `{query}` is the full query string. Useful for plugins that should always appear.

```json
{ "type": "always" }
```

---

## Actions

### `url`
Opens a URL in the default browser. `{query}` is URL-encoded before substitution.

```json
{
  "type": "url",
  "template": "https://www.youtube.com/results?search_query={query}"
}
```

### `copy`
Copies text to the clipboard. `{query}` is substituted as plain text (not URL-encoded).

```json
{
  "type": "copy",
  "template": "mailto:{query}"
}
```

### `text`
Displays text in a Toast notification. Useful for quick look-up or computed output.

```json
{
  "type": "text",
  "template": "You searched for: {query}"
}
```

---

## Examples

### YouTube Search
```json
{
  "id": "youtube",
  "displayName": "YouTube",
  "patterns": [
    { "type": "prefix", "value": "yt " }
  ],
  "action": {
    "type": "url",
    "template": "https://www.youtube.com/results?search_query={query}"
  }
}
```
Usage: `yt funny cats`

---

### GitHub Search
```json
{
  "id": "github",
  "displayName": "GitHub",
  "patterns": [
    { "type": "prefix", "value": "gh " }
  ],
  "action": {
    "type": "url",
    "template": "https://github.com/search?q={query}"
  }
}
```
Usage: `gh android launcher`

---

### Copy Email Address
```json
{
  "id": "copy_email",
  "displayName": "Copy as Email",
  "patterns": [
    { "type": "regex", "value": "^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$" }
  ],
  "action": {
    "type": "copy",
    "template": "{query}"
  }
}
```
Usage: type any email address → tapping copies it to clipboard.

---

### ISBN Lookup
```json
{
  "id": "isbn",
  "displayName": "Look up book (ISBN)",
  "patterns": [
    { "type": "regex", "value": "^(\\d{10}|\\d{13})$" }
  ],
  "action": {
    "type": "url",
    "template": "https://openlibrary.org/search?isbn={query}"
  }
}
```
Usage: `9780134685991`

---

### Always-Visible Shortcut
```json
{
  "id": "devdocs",
  "displayName": "DevDocs",
  "patterns": [
    { "type": "always" }
  ],
  "action": {
    "type": "url",
    "template": "https://devdocs.io/#q={query}"
  }
}
```
This plugin always appears in results with the current query pre-filled.

---

## Installing a Plugin

1. Open **config → Plugins**
2. Tap the **+** button and select your `.json` file, **or** place the file manually in the app's `plugins/` directory
3. Enable the plugin with the toggle
4. Start typing — the plugin's display name will appear in search results when its pattern matches

## Tips

- Plugin files must be valid JSON; malformed files are silently skipped.
- The `id` field should be unique across all installed plugins to avoid conflicts.
- For `regex` patterns, use double-backslash (`\\d`) because JSON requires escaping backslashes.
- Combine multiple patterns in one plugin to match different query forms (e.g. both a prefix and a regex).
