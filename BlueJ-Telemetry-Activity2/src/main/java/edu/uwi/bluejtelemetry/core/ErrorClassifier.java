package edu.uwi.bluejtelemetry.core;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** BlueJ does not expose stable compiler diagnostic codes: category is inferred, raw code remains null. */
public final class ErrorClassifier {
    private static final Pattern CODE = Pattern.compile("\\b(?:compiler\\.err\\.[a-z0-9._-]+|[A-Z]{2,6}\\d{2,6})\\b", Pattern.CASE_INSENSITIVE);
    public String category(String message) {
        String m = message == null ? "" : message.toLowerCase(Locale.ROOT);
        if (m.contains("cannot find symbol") || m.contains("unknown symbol")) return "UNKNOWN_SYMBOL";
        if (m.contains("incompatible types")) return "TYPE_MISMATCH";
        if (m.contains("';' expected") || m.contains("expected") || m.contains("illegal start")) return "SYNTAX";
        if (m.contains("already defined") || m.contains("duplicate")) return "DUPLICATE_DECLARATION";
        if (m.contains("cannot be applied") || m.contains("argument")) return "METHOD_ARGUMENTS";
        return "OTHER";
    }
    public String explicitCode(String message) {
        Matcher matcher = CODE.matcher(message == null ? "" : message);
        return matcher.find() ? matcher.group() : null;
    }
}
