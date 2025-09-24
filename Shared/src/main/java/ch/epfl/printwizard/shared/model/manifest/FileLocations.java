package ch.epfl.printwizard.shared.model.manifest;

import ch.epfl.printwizard.shared.utils.Preconditions;

/**
 * Represents the file locations in the manifest.json file.
 * @param programFilePath Path to the program.json file
 * @param streamFilePath Path to the stream.json file
 * @param reportFilePath Path to the report.json file
 */
public record FileLocations(
    String programFilePath,
    String streamFilePath,
    String reportFilePath
) {
    public FileLocations {
        Preconditions.requireNonNull(programFilePath, "programFilePath is null");
        Preconditions.requireNonNull(streamFilePath, "streamFilePath is null");
        Preconditions.requireNonNull(reportFilePath, "reportFilePath is null");
        Preconditions.require(!programFilePath.isEmpty(), "programFilePath is empty");
        Preconditions.require(!streamFilePath.isEmpty(), "streamFilePath is empty");
        Preconditions.require(!reportFilePath.isEmpty(), "reportFilePath is empty");
    }
}
