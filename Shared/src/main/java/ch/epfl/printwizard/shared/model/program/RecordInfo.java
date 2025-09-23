package ch.epfl.printwizard.shared.model.program;

/**
 * Represents a record in the program.json file.
 */
public final class RecordInfo extends BaseTypeInfo {

    public RecordInfo(String id, String name, String packageName, String sourceId) {
        super(id, name, packageName, sourceId);
    }
}
