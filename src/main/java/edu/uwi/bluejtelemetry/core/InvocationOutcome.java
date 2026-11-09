package edu.uwi.bluejtelemetry.core;

import java.util.Locale;

/** Interprets BlueJ invocation outcome names without relying on a live GUI. */
public final class InvocationOutcome {
    private InvocationOutcome() { }

    public static boolean isRuntimeException(String status) {
        return status != null && status.toUpperCase(Locale.ROOT).equals("EXCEPTION_EXIT");
    }

    public static String methodName(String name) {
        return name == null || name.isBlank() ? "<constructor>" : name;
    }
}
