package ch.epfl.printwizard.plugin.model.index;

import ch.epfl.printwizard.plugin.model.trace.events.*;
import ch.epfl.printwizard.plugin.utils.Preconditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents an index file containing mappings of various elements by line, object, and span.
 * @param byLine Events mapped by line
 * @param byObject Events mapped by an object
 * @param bySpan Events mapped by span
 */
public record IndexFile(
    Map<String, Map<String, List<String>>> byLine,
    Map<String, List<String>> byObject,
    Map<String, List<String>> bySpan
) {

    public IndexFile {
        Preconditions.requireNonNull(byLine, "byLine cannot be null");
        Preconditions.requireNonNull(byObject, "byObject cannot be null");
        Preconditions.requireNonNull(bySpan, "bySpan cannot be null");
    }

    /**
     * Represents a builder for {@link IndexFile}.
     */
    public static final class Builder {
        private final Map<String, Map<String, List<String>>> byLine;
        private final Map<String, List<String>> byObject;
        private final Map<String, List<String>> bySpan;

        public Builder() {
            this.byLine = new java.util.LinkedHashMap<>();
            this.byObject = new java.util.LinkedHashMap<>();
            this.bySpan = new java.util.LinkedHashMap<>();
        }

        /**
         * Adds an event to the index file by line.
         * @param event The event to be added
         */
        public void addEventByLine(TraceEvent event) {
            if (event.location() != null) {
                String sourceId = event.location().sourceId();
                String line = String.valueOf(event.location().line());

                byLine.computeIfAbsent(sourceId, k -> new ConcurrentHashMap<>())
                    .computeIfAbsent(line, k -> Collections.synchronizedList(new ArrayList<>()))
                    .add(event.eventId());
            }
        }

        /**
         * Adds an event to the index file by object.
         * @param event The event to be added
         */
        public void addEventByObject(TraceEvent event) {
            if (event instanceof LocalEvent le) {
                String localVarId = le.label();
                String frameId = le.frameId();

                String key = localVarId + "#" + frameId;

                byObject.computeIfAbsent(key, k -> Collections.synchronizedList(new ArrayList<>()))
                    .add(le.eventId());
            }
            else if (event instanceof ArrayStoreEvent ase) {
                String localVarId = ase.label();
                String frameId = ase.frameId();

                String key = localVarId + "#" + frameId;

                byObject.computeIfAbsent(key, k -> Collections.synchronizedList(new ArrayList<>()))
                    .add(ase.eventId());
            }
        }

        /**
         * Adds an event to the index file by span.
         * @param event The event to be added
         */
        public void addEventBySpan(TraceEvent event) {
            if (event.spanId() != null) {
                bySpan.computeIfAbsent(event.spanId(), k -> Collections.synchronizedList(new ArrayList<>()))
                    .add(event.eventId());
            }
        }

        /**
         * Adds an event to the index file.
         * @param event The event to be added
         * @param shouldBeAddedInSpan Whether the event should be added in span
         */
        public void addEvent(TraceEvent event, boolean shouldBeAddedInSpan) {
            addEventByLine(event);
            addEventByObject(event);

            if (shouldBeAddedInSpan) {
                addEventBySpan(event);
            }
        }

        /**
         * Builds the index file.
         * @return The index file
         */
        public IndexFile build() {
            return new IndexFile(byLine, byObject, bySpan);
        }
    }

}
