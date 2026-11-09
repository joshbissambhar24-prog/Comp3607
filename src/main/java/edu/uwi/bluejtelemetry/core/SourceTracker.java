package edu.uwi.bluejtelemetry.core;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Snapshots current sources. Changes imply edit batches, NOT individual keystrokes. */
public final class SourceTracker {
    private final Clock clock;
    private final TelemetryBus bus;
    private final EventCreator.Lifecycle lifecycle = new EventCreator.Lifecycle();
    private final EventCreator.Editor editor = new EventCreator.Editor();
    private final MethodScanner scanner = new MethodScanner();
    private final Map<String, String> previous = new HashMap<>();
    private final Map<String, Instant> lastEdit = new HashMap<>();
    private boolean seeded;
    public SourceTracker(Clock clock, TelemetryBus bus) { this.clock = clock; this.bus = bus; }
    public synchronized Map<String, String> knownSources() { return Map.copyOf(previous); }
    public synchronized void reset() { previous.clear(); lastEdit.clear(); seeded = false; }
    public synchronized void sample(Map<String, String> sources) {
        Instant now = clock.instant();
        Map<String, String> next = new LinkedHashMap<>(sources);
        for (String removed : new HashSet<>(previous.keySet())) {
            if (!next.containsKey(removed)) {
                bus.publish(lifecycle.create(EventType.CLASS_DELETED, removed, now, Map.of()));
                lastEdit.remove(removed);
            }
        }
        for (Map.Entry<String, String> entry : next.entrySet()) {
            String name = entry.getKey(); String text = entry.getValue();
            String old = previous.get(name);
            if (old == null) {
                EventType t = seeded ? EventType.CLASS_CREATED : EventType.CLASS_PRESENT;
                bus.publish(lifecycle.create(t, name, now, Map.of()));
                for (String method : scanner.scan(text)) {
                    bus.publish(lifecycle.create(EventType.METHOD_DEFINED, name, now,
                        Map.of("method", method, "existing_at_start", !seeded)));
                }
            } else if (!old.equals(text)) {
                bus.publish(lifecycle.create(EventType.CLASS_MODIFIED, name, now, Map.of()));
                int changes = approximateCharactersChanged(old, text);
                Instant prior = lastEdit.put(name, now);
                long interval = prior == null ? 0 : Math.max(0, java.time.Duration.between(prior, now).toSeconds());
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("changed_characters_estimate", changes);
                data.put("seconds_since_previous_edit_sample", interval);
                data.put("sampling_based", true);
                bus.publish(editor.create(EventType.EDIT_DETECTED, name, now, data));
                Set<String> oldMethods = scanner.scan(old);
                for (String newMethod : scanner.scan(text)) {
                    if (!oldMethods.contains(newMethod))
                        bus.publish(lifecycle.create(EventType.METHOD_DEFINED, name, now, Map.of("method", newMethod)));
                }
            }
        }
        previous.clear(); previous.putAll(next); seeded = true;
    }
    public synchronized void remove(String name) {
        if (previous.remove(name) != null) {
            bus.publish(lifecycle.create(EventType.CLASS_DELETED, name, clock.instant(), Map.of()));
            lastEdit.remove(name);
        }
    }
    public synchronized void rename(String oldName, String newName) {
        String oldSource = previous.remove(oldName);
        if (oldSource != null) {
            previous.put(newName, oldSource);
            Instant t = lastEdit.remove(oldName);
            if (t != null) lastEdit.put(newName, t);
        }
        bus.publish(lifecycle.create(EventType.CLASS_RENAMED, newName, clock.instant(), Map.of("old_name", oldName)));
    }
    /** Prefix/suffix diff reduces a bulk rewrite to one approximate edit batch. */
    static int approximateCharactersChanged(String a, String b) {
        int prefix = 0;
        while (prefix < a.length() && prefix < b.length() && a.charAt(prefix) == b.charAt(prefix)) prefix++;
        int suffix = 0;
        while (suffix < a.length()-prefix && suffix < b.length()-prefix &&
               a.charAt(a.length()-1-suffix) == b.charAt(b.length()-1-suffix)) suffix++;
        return (a.length()-prefix-suffix) + (b.length()-prefix-suffix);
    }
}
