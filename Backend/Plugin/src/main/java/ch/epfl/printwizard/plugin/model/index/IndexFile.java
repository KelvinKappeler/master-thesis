package ch.epfl.printwizard.plugin.model.index;

import ch.epfl.printwizard.plugin.model.trace.events.*;
import ch.epfl.printwizard.plugin.utils.Preconditions;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * Represents an index file containing mappings of various elements by line, object, and span.
 * @param byLine Events mapped by line
 * @param byLocal Events mapped by a local variable
 * @param byObject Events mapped by an object
 * @param bySpan Events mapped by span
 */
public record IndexFile(
    Map<String, Map<String, List<String>>> byLine,
    Map<String, List<String>> byLocal,
    Map<String, List<String>> byObject,
    Map<String, List<String>> bySpan
) {

    public IndexFile {
        Preconditions.requireNonNull(byLine, "byLine cannot be null");
        Preconditions.requireNonNull(byLocal, "byLocal cannot be null");
        Preconditions.requireNonNull(bySpan, "bySpan cannot be null");
    }

    /**
     * Represents a builder for {@link IndexFile}.
     */
    public static final class Builder {
        private final Map<String, Map<String, List<String>>> byLine;
        private final Map<String, List<String>> byLocal;
        private final Map<String, List<String>> byObject;
        private final Map<String, List<String>> bySpan;

        public Builder() {
            this.byLine = new LinkedHashMap<>();
            this.byLocal = new LinkedHashMap<>();
            this.byObject = new LinkedHashMap<>();
            this.bySpan = new LinkedHashMap<>();
        }

        /**
         * Adds an event to the index file.
         * @param event The event to be added
         */
        public void addEvent(TraceEvent event) {
            addEventByLine(event);
            addEventByLocal(event);
            addEventByObject(event);
            addEventBySpan(event);
        }

        /**
         * Builds the index file.
         * @return The index file
         */
        public IndexFile build() {
            Map<String, Map<String, List<String>>> byLineSnapshot;
            Map<String, List<String>> byLocalSnapshot;
            Map<String, List<String>> byObjectSnapshot;
            Map<String, List<String>> bySpanSnapshot;

            synchronized (byLine) {
                byLineSnapshot = Map.copyOf(byLine);
            }
            synchronized (byLocal) {
                byLocalSnapshot = Map.copyOf(byLocal);
            }
            synchronized (bySpan) {
                bySpanSnapshot = Map.copyOf(bySpan);
            }
            synchronized (byObject) {
                byObjectSnapshot = Map.copyOf(byObject);
            }

            return new IndexFile(byLineSnapshot, byLocalSnapshot, byObjectSnapshot, bySpanSnapshot);
        }

        private void addEventByLine(TraceEvent event) {
            if (event.location() != null) {
                String sourceId = event.location().sourceId();
                String line = String.valueOf(event.location().line());

                byLine.computeIfAbsent(sourceId, k -> new ConcurrentHashMap<>())
                    .computeIfAbsent(line, k -> Collections.synchronizedList(new ArrayList<>()))
                    .add(event.eventId());
            }
        }

        private void addEventByLocal(TraceEvent event) {
            if (event instanceof LocalEvent le) {
                String localVarId = le.label();
                String frameId = le.frameId();

                String key = localVarId + "#" + frameId;

                byLocal.computeIfAbsent(key, k -> Collections.synchronizedList(new ArrayList<>())).add(le.eventId());
            }
            else if (event instanceof ArrayStoreEvent ase) {
                String localVarId = ase.label();
                String frameId = ase.frameId();

                String key = localVarId + "#" + frameId;

                byLocal.computeIfAbsent(key, k -> Collections.synchronizedList(new ArrayList<>())).add(ase.eventId());
            }
        }

        private void addEventByObject(TraceEvent event) {
            final BiConsumer<String, String> indexObject = (objectId, eventId) -> {
                if (objectId == null) return;
                
                byObject.computeIfAbsent(objectId, k -> Collections.synchronizedList(new ArrayList<>())).add(eventId);
            };

            if (event instanceof NewEvent ne) {
                indexObject.accept(ne.objectId(), ne.eventId());
            }
            else if (event instanceof FieldWriteEvent fwe) {
                indexObject.accept(fwe.objectId(), fwe.eventId());
                
                if (fwe.value() != null && fwe.value().valueObjectId() != null) {
                    indexObject.accept(fwe.value().valueObjectId(), fwe.eventId());
                }
            }
            else if (event instanceof ArrayStoreEvent ase) {
                indexObject.accept(ase.arrayObjectId(), ase.eventId());
                
                if (ase.value() != null && ase.value().valueObjectId() != null) {
                    indexObject.accept(ase.value().valueObjectId(), ase.eventId());
                }
            }
            else if (event instanceof LocalEvent le) {
                if (le.value() != null && le.value().valueObjectId() != null) {
                    indexObject.accept(le.value().valueObjectId(), le.eventId());
                }
            }
            else if (event instanceof ReturnEvent re) {
                if (re.value() != null && re.value().valueObjectId() != null) {
                    indexObject.accept(re.value().valueObjectId(), re.eventId());
                }
            }
        }

        private void addEventBySpan(TraceEvent event) {
            if (event.spanId() != null && event instanceof CallEvent callEvent && !callEvent.external()) {
                bySpan.computeIfAbsent(event.spanId(), k -> Collections.synchronizedList(new ArrayList<>())).add(event.eventId());
            }
        }
    }

}
