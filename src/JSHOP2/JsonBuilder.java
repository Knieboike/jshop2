package JSHOP2;

/**
 * Helper class for building JSON structures in a more readable way
 */
public class JsonBuilder {
    private StringBuilder json = new StringBuilder();
    private int indentLevel = 0;
    private boolean needsComma = false;

    public JsonBuilder() {}

    public JsonBuilder startObject() {
        addCommaIfNeeded();
        addIndent();
        json.append("{\n");
        indentLevel++;
        needsComma = false;
        return this;
    }

    public JsonBuilder endObject() {
        indentLevel--;
        json.append("\n");
        addIndent();
        json.append("}");
        needsComma = true;
        return this;
    }

    public JsonBuilder startArray() {
        addCommaIfNeeded();
        json.append("[\n");
        indentLevel++;
        needsComma = false;
        return this;
    }

    public JsonBuilder endArray() {
        indentLevel--;
        json.append("\n");
        addIndent();
        json.append("]");
        needsComma = true;
        return this;
    }

    public JsonBuilder addProperty(String key, String value) {
        addCommaIfNeeded();
        addIndent();
        json.append("\"").append(key).append("\": \"").append(escapeJson(value)).append("\"");
        needsComma = true;
        return this;
    }

    public JsonBuilder addProperty(String key, int value) {
        addCommaIfNeeded();
        addIndent();
        json.append("\"").append(key).append("\": ").append(value);
        needsComma = true;
        return this;
    }

    public JsonBuilder addTitle(String value) {
        addCommaIfNeeded();
        addIndent();
        json.append("\"").append(escapeJson(value)).append("\": ");
        return this;
    }

    public JsonBuilder addRawProperty(String key, String rawJson) {
        addCommaIfNeeded();
        addIndent();
        json.append("\"").append(key).append("\": ").append(rawJson);
        needsComma = true;
        return this;
    }

    public JsonBuilder addArrayElement(String value) {
        addCommaIfNeeded();
        addIndent();
        json.append("\"").append(escapeJson(value)).append("\"");
        needsComma = true;
        return this;
    }

    public JsonBuilder addRawArrayElement(String rawJson) {
        addCommaIfNeeded();
        addIndent();
        json.append(rawJson);
        needsComma = true;
        return this;
    }

    private JsonBuilder addCommaIfNeeded() {
        if (needsComma) {
            json.append(",\n");
        }
        return this;
    }

    private void addIndent() {
        for (int i = 0; i < indentLevel; i++) {
            json.append("  ");
        }
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    @Override
    public String toString() {
        return json.toString();
    }
}
