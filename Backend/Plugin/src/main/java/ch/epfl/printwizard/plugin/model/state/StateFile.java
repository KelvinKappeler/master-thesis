package ch.epfl.printwizard.plugin.model.state;

import ch.epfl.printwizard.plugin.model.trace.events.*;
import ch.epfl.printwizard.plugin.utils.Preconditions;

import java.lang.reflect.Array;
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
            } else if (event instanceof ArrayStoreEvent ase) {
                handleArrayStore(ase);
            } else if (event instanceof LocalEvent le) {
                handleLocal(le);
            } else if (event instanceof CallEvent ce) {
                // Do nothing
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

            FieldState fieldState = new FieldState(fw.fieldType(), fw.value().value(), fw.value().valueObjectId());
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

            String fieldType;
            if (ase.value().value() != null) {
                fieldType = ase.value().value().getClass().getTypeName();
            } else {
                fieldType = "java.lang.Object";
            }

            FieldState fieldState = new FieldState(fieldType, ase.value().value(), ase.value().valueObjectId());
            newFields.put(fieldName, fieldState);

            int nextVersion = last.version() + 1;

            timeline.add(new StateSnapshot(nextVersion, ase.eventId(), newFields));
        }

        private void handleLocal(LocalEvent le) {
            EventValue ev = le.value();
            
            String objectId = ev.valueObjectId();
            if (objectId == null) {
                return;
            }

            Object obj = EventValue.getObjectById(objectId);
            if (obj == null) {
                return;
            }

            Class<?> cls = obj.getClass();
            if (!cls.isArray()) {
                return;
            }
            
            int nextVersion = 0;
            if (objects.containsKey(objectId)) {
                List<StateSnapshot> timeline = objects.get(objectId).timeline();
                nextVersion = timeline.getLast().version() + 1;
            }

            int length = Array.getLength(obj);
            Map<String, FieldState> initialFields = new LinkedHashMap<>();

            for (int i = 0; i < length; i++) {
                Object elem = Array.get(obj, i);
                String fieldName = "[" + i + "]";

                String fieldType = (elem != null) ? elem.getClass().getTypeName() : "java.lang.Object";
                
                String elemObjectId = null;
                if (elem != null
                    && !(elem instanceof Number)
                    && !(elem instanceof Boolean)
                    && !(elem instanceof Character)
                    && !(elem instanceof String)) {
                    
                    elemObjectId = EventValue.getOrCreateObjectId(elem);
                }

                FieldState fieldState = new FieldState(fieldType, elem, elemObjectId);
                initialFields.put(fieldName, fieldState);
            }

            List<StateSnapshot> timeline = new ArrayList<>();
            timeline.add(new StateSnapshot(nextVersion, le.eventId(), initialFields));

            String typeName = cls.getTypeName();
            ObjectTimeline objectTimeline = new ObjectTimeline(objectId, typeName, timeline);
            objects.put(objectId, objectTimeline);
        }
    }
}
