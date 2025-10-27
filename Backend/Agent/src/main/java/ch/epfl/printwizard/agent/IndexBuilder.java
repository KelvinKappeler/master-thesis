package ch.epfl.printwizard.agent;

import ch.epfl.printwizard.shared.model.index.IndexFile;
import ch.epfl.printwizard.shared.model.trace.events.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a builder for creating and writing an event index to a JSON file.
 */
public final class IndexBuilder {
    private final Map<String, Map<String, List<String>>> byLine = new ConcurrentHashMap<>();
    private final Map<String, List<String>> byObject = new ConcurrentHashMap<>();
    private final Map<String, List<String>> bySpan = new ConcurrentHashMap<>();

    /**
     * Records an event in the index
     * @param event the event to index
     */
    public void addEvent(TraceEvent event) {
        String eventId = event.eventId();

        // Index by line
        if (event.location() != null) {
            String sourceId = event.location().sourceId();
            String line = String.valueOf(event.location().line());

            byLine.computeIfAbsent(sourceId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(line, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(eventId);
        }

        // Index by span
        if (event.spanId() != null) {
            bySpan.computeIfAbsent(event.spanId(), k -> Collections.synchronizedList(new ArrayList<>()))
                .add(eventId);

            if (event instanceof ConditionEvent conditionEvent) {
                for (String eId : conditionEvent.childrenEventIds()) {
                    bySpan.computeIfAbsent(event.spanId(), k -> Collections.synchronizedList(new ArrayList<>()))
                        .remove(eId);
                }
            }
        }

        // Index by object
        indexByObject(event, eventId);
    }

    /**
     * Replaces an event in the index with a new event.
     * @param event the new event to replace the old one with
     */
    public void replaceEvent(TraceEvent event) {
        String eventId = event.eventId();

        for (Map<String, List<String>> lineMap : byLine.values()) {
            for (List<String> eventIds : lineMap.values()) {
                eventIds.remove(eventId);
            }
        }

        for (List<String> eventIds : bySpan.values()) {
            eventIds.remove(eventId);
        }

        for (List<String> eventIds : byObject.values()) {
            eventIds.remove(eventId);
        }

        addEvent(event);
    }

    /**
     * Writes the index to a JSON file
     * @param file the file to write to
     * @throws Exception if writing fails
     */
    public void writeJsonTo(File file) throws Exception {
        Map<String, Map<String, List<String>>> byLineSnapshot;
        Map<String, List<String>> byObjectSnapshot;
        Map<String, List<String>> bySpanSnapshot;

        synchronized (byLine) { byLineSnapshot = deepCopyByLine(); }
        synchronized (byObject) { byObjectSnapshot = Map.copyOf(byObject); }
        synchronized (bySpan) { bySpanSnapshot = Map.copyOf(bySpan); }

        IndexFile indexFile = new IndexFile(byLineSnapshot, byObjectSnapshot, bySpanSnapshot);

        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(file, indexFile);
    }

    private void indexByObject(TraceEvent event, String eventId) {
        String objectId = null;

        switch (event) {
            case PutFieldEvent e -> objectId = e.eventId();
            case ArrayStoreEvent e -> objectId = e.eventId();
            case NewEvent e -> objectId = e.eventId();
            default -> {}
        }

        if (objectId != null) {
            byObject.computeIfAbsent(objectId, k -> Collections.synchronizedList(new ArrayList<>()))
                    .add(eventId);
        }
    }

    private Map<String, Map<String, List<String>>> deepCopyByLine() {
        Map<String, Map<String, List<String>>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, List<String>>> entry : byLine.entrySet()) {
            Map<String, List<String>> innerCopy = new LinkedHashMap<>();
            for (Map.Entry<String, List<String>> innerEntry : entry.getValue().entrySet()) {
                innerCopy.put(innerEntry.getKey(), List.copyOf(innerEntry.getValue()));
            }
            copy.put(entry.getKey(), innerCopy);
        }
        return copy;
    }
}