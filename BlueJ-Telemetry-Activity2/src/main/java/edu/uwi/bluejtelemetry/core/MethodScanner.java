package edu.uwi.bluejtelemetry.core;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Approximate method-definition detection, not a full Java AST parser. */
public final class MethodScanner {
    private static final Pattern SIGNATURE = Pattern.compile(
        "(?m)(?:^|[;{}]\\s*)(?:@[A-Za-z_][\\w.]*(?:\\([^)]*\\))?\\s*)*" +
        "(?:(?:public|protected|private|static|final|synchronized|abstract|native|default|strictfp)\\s+)*" +
        "(?:[A-Za-z_$][\\w$<>\\[\\],.? ]*?\\s+)([A-Za-z_$][\\w$]*)\\s*\\([^;{}]*\\)\\s*(?:throws\\s+[\\w., ]+)?\\s*\\{");
    private static final Set<String> CONTROL = Set.of("if", "for", "while", "catch", "switch", "try", "new");
    public Set<String> scan(String javaSource) {
        Set<String> result = new LinkedHashSet<>();
        Matcher matcher = SIGNATURE.matcher(javaSource == null ? "" : javaSource);
        while (matcher.find()) if (!CONTROL.contains(matcher.group(1))) result.add(matcher.group(1));
        return result;
    }
}
