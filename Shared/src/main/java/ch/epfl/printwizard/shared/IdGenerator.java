package ch.epfl.printwizard.shared;

import ch.epfl.printwizard.shared.utils.Preconditions;

/**
 * Represents a generator for unique IDs.
 */
public final class IdGenerator {

    private IdGenerator() {}
    
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
     * @param desc the descriptor of the method
     * @return the unique ID for the method (e.g., "m:java/lang/String.charAt(I)")
     */
    public static String methodId(String classInternal, String name, String desc) {
        Preconditions.requireNonNull(classInternal, "classInternal is null");
        Preconditions.require(!classInternal.isEmpty(), "classInternal is empty");
        Preconditions.requireNonNull(name, "name is null");
        Preconditions.require(!name.isEmpty(), "name is empty");
        Preconditions.requireNonNull(desc, "desc is null");
        
        return "m:" + classInternal + "." + name + "(" + desc + ")";
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
