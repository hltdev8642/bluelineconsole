package net.nhiroki.bluelineconsole.plugin;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class PluginDefinition {
    public String id;
    public String displayName;
    public String sourceFileName;
    public List<PatternDef> patterns = new ArrayList<>();
    public ActionDef action;

    public static class PatternDef {
        public String type;
        public String value;
    }

    public static class ActionDef {
        public String type;
        public String template;
    }

    public static PluginDefinition fromFile(File file) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (FileInputStream inputStream = new FileInputStream(file)) {
            byte[] chunk = new byte[4096];
            int readBytes;
            while ((readBytes = inputStream.read(chunk)) != -1) {
                buffer.write(chunk, 0, readBytes);
            }
        }

        if (buffer.size() == 0) {
            throw new Exception("empty");
        }

        JSONObject object = new JSONObject(buffer.toString(StandardCharsets.UTF_8.name()));
        PluginDefinition definition = new PluginDefinition();
        definition.id = object.optString("id", file.getName());
        definition.displayName = object.optString("displayName", definition.id);
        definition.sourceFileName = file.getName();

        if (object.has("patterns")) {
            JSONArray patternsArray = object.getJSONArray("patterns");
            for (int i = 0; i < patternsArray.length(); ++i) {
                JSONObject patternObject = patternsArray.getJSONObject(i);
                PatternDef pattern = new PatternDef();
                pattern.type = patternObject.optString("type", "always");
                pattern.value = patternObject.optString("value", "");
                definition.patterns.add(pattern);
            }
        } else {
            PatternDef pattern = new PatternDef();
            pattern.type = "always";
            pattern.value = "";
            definition.patterns.add(pattern);
        }

        if (object.has("action")) {
            JSONObject actionObject = object.getJSONObject("action");
            ActionDef pluginAction = new ActionDef();
            pluginAction.type = actionObject.optString("type", "text");
            pluginAction.template = actionObject.optString("template", "{query}");
            definition.action = pluginAction;
        }

        return definition;
    }
}
