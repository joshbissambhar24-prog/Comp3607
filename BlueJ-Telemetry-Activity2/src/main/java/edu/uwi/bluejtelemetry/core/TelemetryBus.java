package edu.uwi.bluejtelemetry.core;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** Observer pattern; collection sources and session recording do not depend on one another. */
public final class TelemetryBus {
    private final CopyOnWriteArrayList<Consumer<TelemetryEvent>> listeners = new CopyOnWriteArrayList<>();
    public void subscribe(Consumer<TelemetryEvent> listener) { listeners.add(Objects.requireNonNull(listener)); }
    public void unsubscribe(Consumer<TelemetryEvent> listener) { listeners.remove(listener); }
    public void publish(TelemetryEvent event) {
        Objects.requireNonNull(event);
        for (Consumer<TelemetryEvent> listener : listeners) listener.accept(event);
    }
}
