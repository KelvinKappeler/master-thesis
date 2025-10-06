package ch.epfl.printwizard.agent;

import java.util.WeakHashMap;

/**
 * Utility class for generating and managing unique identifiers for objects.
 */
public final class ObjectIds {
    
    private static final long INITIAL_COUNTER = 10L;
    
    private static final WeakHashMap<Object, String> ids = new WeakHashMap<>();
    private static long counter = INITIAL_COUNTER;

    /**
     * Generates or retrieves a unique identifier for the given object.
     * @param o the object to generate an identifier for
     * @return the unique identifier for the object
     */
    public static String id(Object o) {
        if (o == null) return "null";
        
        String s = ids.get(o);
        if (s != null) return s;
        
        String ref = "o:" + counter;
        counter++;
        
        ids.put(o, ref);
        return ref;
    }

    private ObjectIds() {}
}
