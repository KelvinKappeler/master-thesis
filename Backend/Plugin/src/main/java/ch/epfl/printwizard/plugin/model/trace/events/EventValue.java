package ch.epfl.printwizard.plugin.model.trace.events;

import ch.epfl.printwizard.plugin.utils.Ids;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Represents the value of an event.
 * @param value the value, if the type is primitive, null or string
 * @param valueObjectId the ID of the object, if the type is an object
 */
public record EventValue(Object value, String valueObjectId, String javaTypeName, ValueKind kind) {
    private static final Map<Object, String> OBJECT_IDS = Collections.synchronizedMap(new WeakHashMap<>());

    /**
     * Creates an event value from a value.
     * @param value the value to create the event value from
     * @return the event value
     * @param <T> the type of the value
     */
    public static <T> EventValue of(T value) {
        if (value == null) {
            return new EventValue(null, null, "java.lang.Object", ValueKind.NULL);
        }

        Class<?> c = value.getClass();
        
        if (c.isArray()) {
            String objectId = getOrCreateObjectId(value);
            return new EventValue(
                null,
                objectId,
                c.getTypeName(),
                ValueKind.ARRAY
            );
        }
        
        if (c.isPrimitive() || Number.class.isAssignableFrom(c) || c == Boolean.class || c == Character.class || c == String.class) {
            return new EventValue(value, null, c.getTypeName(), ValueKind.PRIMITIVE);
        }
        
        String objectId = getOrCreateObjectId(value);
        return new EventValue(
            null,
            objectId,
            c.getTypeName(),
            ValueKind.OBJECT
        );
    }

    /**
     * Gets the object ID of an object, or creates a new one if it does not exist.
     * @param obj the object to get the ID of
     * @return the object ID
     */
    public static String getOrCreateObjectId(Object obj) {
        if (obj == null) return "null";

        return OBJECT_IDS.computeIfAbsent(obj, o -> Ids.nextObjectId());
    }

    /**
     * Gets the object by its ID.
     * @param id the ID of the object
     * @return the object
     */
    public static Object getObjectById(String id) {
        for (Map.Entry<Object, String> entry : OBJECT_IDS.entrySet()) {
            if (entry.getValue().equals(id)) return entry.getKey();
        }
        
        return null;
    }
}
