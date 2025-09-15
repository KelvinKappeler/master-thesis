package ch.epfl.printwizard.model;

import ch.epfl.printwizard.utils.Preconditions;

/**
 * Represents the base information of a type (class, interface, record, etc.) in the program.
 */
public abstract class BaseTypeInfo {

    private final String id;
    private final String name;
    private final String packageName;
    private final String sourceId;
    
    protected BaseTypeInfo(String id, String name, String packageName, String sourceId) {
        Preconditions.requireNonNull(id, "id cannot be null");
        Preconditions.requireNonNull(name, "name cannot be null");
        Preconditions.requireNonNull(packageName, "package name cannot be null");
        Preconditions.requireNonNull(sourceId, "source id cannot be null");
        Preconditions.require(!id.isEmpty(), "type id cannot be empty");
        
        this.id = id;
        this.name = name;
        this.packageName = packageName;
        this.sourceId = sourceId;
    }

    /**
     * Gets the unique identifier of the type.
     * @return the type ID
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the simple name of the type.
     * @return the type name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the package name of the type.
     * @return the package name
     */
    public String getPackageName() {
        return packageName;
    }

    /**
     * Gets the ID of the source file where the type is defined.
     * @return the source file ID
     */
    public String getSourceId() {
        return sourceId;
    }
}
