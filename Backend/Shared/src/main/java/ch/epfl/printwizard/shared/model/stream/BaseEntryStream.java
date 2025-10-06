package ch.epfl.printwizard.shared.model.stream;

/**
 * Represents a base entry in the stream.json file.
 */
public class BaseEntryStream {

    private final StreamOperation operation;

    public BaseEntryStream(StreamOperation operation) {
        this.operation = operation;
    }

    /**
     * Gets the operation of the stream entry.
     * @return the operation
     */
    public StreamOperation getOperation() {
        return operation;
    }
}
