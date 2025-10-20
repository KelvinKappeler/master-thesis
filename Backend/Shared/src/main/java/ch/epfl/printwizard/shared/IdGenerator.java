package ch.epfl.printwizard.shared;

import ch.epfl.printwizard.shared.utils.Preconditions;

/**
 * Represents a generator for unique IDs.
 */
public final class IdGenerator {

    private IdGenerator() {}

    /**
     * Generates a unique ID for a source.
     * @param source the source string (e.g., file name)
     * @return the unique ID for the source (e.g., "src:MyClass.java")
     */
    public static String sourceId(String source) {
        Preconditions.requireNonNull(source, "source is null");
        Preconditions.require(!source.isEmpty(), "source is empty");

        return "src:" + source;
    }

    /**
     * Generates a unique ID for a trace.
     * @param trace the trace string (e.g., trace identifier)
     * @return the unique ID for the trace (e.g., "trc:trace123")
     */
    public static String traceId(String trace) {
        Preconditions.requireNonNull(trace, "trace is null");
        Preconditions.require(!trace.isEmpty(), "trace is empty");

        return "trc:" + trace;
    }

    /**
     * Generates a unique ID for an event.
     * @param id the event identifier
     * @return the unique ID for the event (e.g., "ev:event456")
     */
    public static String eventId(String id) {
        Preconditions.requireNonNull(id, "id is null");
        Preconditions.require(!id.isEmpty(), "id is empty");

        return "eve:" + id;
    }

    /**
     * Generates a unique ID for a span.
     * @param id the span identifier
     * @return the unique ID for the span (e.g., "spn:span789")
     */
    public static String spanId(String id) {
        Preconditions.requireNonNull(id, "id is null");
        Preconditions.require(!id.isEmpty(), "id is empty");

        return "spn:" + id;
    }

    /**
     * Generates a unique ID for a frame.
     * @param id the frame identifier
     * @return the unique ID for the frame (e.g., "frm:frame012")
     */
    public static String frameId(String id) {
        Preconditions.requireNonNull(id, "id is null");
        Preconditions.require(!id.isEmpty(), "id is empty");

        return "frm:" + id;
    }

    /**
     * Generates a unique ID for a resultType.
     * @param id the resultType identifier
     * @return the unique ID for the resultType (e.g., "t:TypeName")
     */
    public static String typeId(String id) {
        Preconditions.requireNonNull(id, "id is null");
        Preconditions.require(!id.isEmpty(), "id is empty");

        return "t:" + id;
    }

    /**
     * Generates a unique ID for an object based on its identity hash code.
     * @param object the object to generate the ID for
     * @return the unique ID for the object (e.g., "o:1a2b3c4d")
     */
    public static String objectId(Object object) {
        Preconditions.requireNonNull(object, "object is null");

        String id = Integer.toHexString(System.identityHashCode(object));

        return "o:" + id;
    }

    /**
     * Generates a unique ID for a class based on its internal name.
     * @param internal the internal name of the class (e.g., "java/lang/String")
     * @return the unique ID for the class (e.g., "cls:java/lang/String")
     */
    public static String classId(String internal) {
        Preconditions.requireNonNull(internal, "internal is null");
        Preconditions.require(!internal.isEmpty(), "internal is empty");
        
        return "cls:" + internal;
    }

    /**
     * Generates a unique ID for a method.
     * @param classInternal the internal name of the class containing the method
     * @param name the name of the method
     * @param params the parameter types
     * @param returnType the return resultType of the method
     * @return the unique ID for the method (e.g., "m:java/lang/String.charAt(I)")
     */
    public static String methodId(String classInternal, String name, String params, String returnType) {
        Preconditions.requireNonNull(classInternal, "classInternal is null");
        Preconditions.require(!classInternal.isEmpty(), "classInternal is empty");
        Preconditions.requireNonNull(name, "name is null");
        Preconditions.require(!name.isEmpty(), "name is empty");
        Preconditions.requireNonNull(params, "params is null");
        Preconditions.requireNonNull(returnType, "returnType is null");

        return "m:" + classInternal + "." + name + "(" + params + ")" + returnType;
    }

    /**
     * Generates a unique ID for a field.
     * @param classInternal the internal name of the class containing the field
     * @param name the name of the field
     * @param desc the descriptor of the field
     * @return the unique ID for the field (e.g., "f:java/lang/String.value:C")
     */
    public static String fieldId(String classInternal, String name, String desc) {
        Preconditions.requireNonNull(classInternal, "classInternal is null");
        Preconditions.require(!classInternal.isEmpty(), "classInternal is empty");
        Preconditions.requireNonNull(name, "name is null");
        Preconditions.require(!name.isEmpty(), "name is empty");
        Preconditions.requireNonNull(desc, "desc is null");
        
        return "f:" + classInternal + "." + name + ":" + desc;
    }
    
}
