package edu.uwi.bluejtelemetry.export;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;

/** Minimal strict JSON emitter: handles objects, arrays, escaped strings, booleans, numbers and null. */
public final class JsonWriter {
    public String stringify(Object value) {
        StringBuilder out = new StringBuilder(); append(value, out); return out.toString();
    }
    private void append(Object value, StringBuilder out) {
        if (value == null) { out.append("null"); return; }
        if (value instanceof CharSequence || value instanceof Enum<?>) { quote(value.toString(), out); return; }
        if (value instanceof Boolean) { out.append(value); return; }
        if (value instanceof Number n) {
            if (n instanceof Double d && !Double.isFinite(d)) { out.append("null"); return; }
            if (n instanceof Float f && !Float.isFinite(f)) { out.append("null"); return; }
            out.append(n); return;
        }
        if (value instanceof Map<?, ?> map) {
            out.append('{'); boolean first = true;
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (!first) out.append(','); first = false;
                quote(String.valueOf(e.getKey()), out); out.append(':'); append(e.getValue(), out);
            }
            out.append('}'); return;
        }
        if (value instanceof Collection<?> collection) {
            out.append('['); boolean first = true;
            for (Object item : collection) { if (!first) out.append(','); first = false; append(item, out); }
            out.append(']'); return;
        }
        if (value.getClass().isArray()) {
            out.append('['); int n = Array.getLength(value);
            for (int i=0; i<n; i++) { if (i>0) out.append(','); append(Array.get(value, i), out); }
            out.append(']'); return;
        }
        throw new IllegalArgumentException("Unsupported JSON value: " + value.getClass());
    }
    private void quote(String str, StringBuilder out) {
        out.append('"');
        for (int i=0; i<str.length(); i++) {
            char c = str.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                default -> {
                    if (c < 0x20 || Character.isSurrogate(c)) out.append(String.format("\\u%04x", (int)c));
                    else out.append(c);
                }
            }
        }
        out.append('"');
    }
}
