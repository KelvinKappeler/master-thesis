package ch.epfl.printwizard.plugin.model.manifest;


import ch.epfl.printwizard.plugin.utils.Preconditions;

/**
 * Represents the file locations in the manifest.json file.
 * @param programFilePath Path to the program.json file
 * @param traceFilePath Path to the trace.json file
 * @param indexFilePath Path to the index.json file
 * @param stateFilePath Path to the state.json file
 */
public record FileLocations(
    String programFilePath,
    String traceFilePath,
    String indexFilePath,
    String stateFilePath
) {
    public FileLocations {
        Preconditions.requireNonNull(programFilePath, "programFilePath is null");
        Preconditions.requireNonNull(traceFilePath, "traceFilePath is null");
        Preconditions.requireNonNull(indexFilePath, "indexFilePath is null");
        Preconditions.requireNonNull(stateFilePath, "stateFilePath is null");
        Preconditions.require(!programFilePath.isEmpty(), "programFilePath is empty");
        Preconditions.require(!traceFilePath.isEmpty(), "traceFilePath is empty");
        Preconditions.require(!indexFilePath.isEmpty(), "indexFilePath is empty");
        Preconditions.require(!stateFilePath.isEmpty(), "stateFilePath is empty");
    }
}
