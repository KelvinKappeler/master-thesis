package ch.epfl.printwizard.shared.model.program;

/**
 * Represents a class in the program.json file.
 */
public final class ClassInfo extends BaseTypeInfo {
    
    public ClassInfo(String id, String name, String packageName, String sourceId) {
        super(id, name, packageName, sourceId);
    }
}
