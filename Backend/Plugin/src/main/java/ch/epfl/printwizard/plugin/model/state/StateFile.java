package ch.epfl.printwizard.plugin.model.state;

import ch.epfl.printwizard.plugin.model.trace.events.ArrayStoreEvent;
import ch.epfl.printwizard.plugin.model.trace.events.FieldWriteEvent;
import ch.epfl.printwizard.plugin.model.trace.events.NewEvent;
import ch.epfl.printwizard.plugin.model.trace.events.TraceEvent;
import ch.epfl.printwizard.plugin.utils.Preconditions;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a state file containing a map of objects containing their timeline.
 * @param objects The map of objects.
 */
public record StateFile(
    Map<String, ObjectTimeline> objects
) {

    public StateFile {
        Preconditions.requireNonNull(objects, "objects cannot be null");
    }

    /**
     * Represents a builder for {@link StateFile}.
     */
    public static final class Builder {
        private final Map<String, ObjectTimeline> objects = new LinkedHashMap<>();

        /**
         * Modify the builder to include the given object, if any.
         * @param event The event to include.
         */
        public void onEvent(TraceEvent event) {
            if (event instanceof NewEvent ne) {
                handleNew(ne);
            } else if (event instanceof FieldWriteEvent fw) {
                handleFieldWrite(fw);
            }else if (event instanceof ArrayStoreEvent ase) {
                handleArrayStore(ase);
            }
        }

        /**
         * Builds the {@link StateFile}.
         * @return The built {@link StateFile}.
         */
        public StateFile build() {
            return new StateFile(Map.copyOf(objects));
        }

        private void handleNew(NewEvent ne) {
            String objectId = ne.objectId();
            ObjectTimeline objectTimeline = objects.get(objectId);

            if (objectTimeline == null) {
                List<StateSnapshot> timeline = new ArrayList<>();
                Map<String, FieldState> initialFields = new LinkedHashMap<>();

                timeline.add(new StateSnapshot(0, ne.eventId(), initialFields));

                objectTimeline = new ObjectTimeline(objectId, ne.typeName(), timeline);
                objects.put(objectId, objectTimeline);
            } else {
                List<StateSnapshot> timeline = objectTimeline.timeline();
                StateSnapshot last = timeline.getLast();

                Map<String, FieldState> fieldsCopy = new LinkedHashMap<>(last.fields());
                timeline.add(new StateSnapshot(last.version(), ne.eventId(), fieldsCopy));
            }
        }

        private void handleFieldWrite(FieldWriteEvent fw) {
            String objectId = fw.objectId();
            ObjectTimeline objectTimeline = objects.get(objectId);

            if (objectTimeline == null) {
                List<StateSnapshot> timeline = new ArrayList<>();
                Map<String, FieldState> initialFields = new LinkedHashMap<>();

                timeline.add(new StateSnapshot(0, fw.eventId(), initialFields));
                objectTimeline = new ObjectTimeline(objectId, "ERROR", timeline);
                objects.put(objectId, objectTimeline);
            }

            List<StateSnapshot> timeline = objectTimeline.timeline();
            StateSnapshot last = timeline.getLast();

            Map<String, FieldState> newFields = new LinkedHashMap<>(last.fields());

            FieldState fieldState = new FieldState(fw.fieldType(), fw.value(), fw.valueObjectId());
            newFields.put(fw.fieldName(), fieldState);

            int nextVersion = last.version() + 1;

            timeline.add(new StateSnapshot(nextVersion, fw.eventId(), newFields));
        }

        private void handleArrayStore(ArrayStoreEvent ase) {
            String objectId = ase.arrayObjectId() != null ? ase.arrayObjectId() : ase.label();
            String typeName = "ARRAY";
            ObjectTimeline objectTimeline = objects.get(objectId);

            if (objectTimeline == null) {
                List<StateSnapshot> timeline = new ArrayList<>();
                Map<String, FieldState> initialFields = new LinkedHashMap<>();

                timeline.add(new StateSnapshot(0, ase.eventId(), initialFields));

                objectTimeline = new ObjectTimeline(objectId, typeName, timeline);
                objects.put(objectId, objectTimeline);
            }

            List<StateSnapshot> timeline = objectTimeline.timeline();
            StateSnapshot last = timeline.getLast();

            Map<String, FieldState> newFields = new LinkedHashMap<>(last.fields());

            String fieldName = "[" + ase.index() + "]";

            Object value = ase.value();

            String fieldType;
            if (value != null) {
                fieldType = value.getClass().getTypeName();
            } else {
                fieldType = "java.lang.Object";
            }

            FieldState fieldState = new FieldState(fieldType, value, ase.valueObjectId());
            newFields.put(fieldName, fieldState);

            int nextVersion = last.version() + 1;

            timeline.add(new StateSnapshot(nextVersion, ase.eventId(), newFields));
        }

    }
}
