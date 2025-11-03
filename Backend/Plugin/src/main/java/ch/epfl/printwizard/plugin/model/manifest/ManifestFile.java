package ch.epfl.printwizard.plugin.model.manifest;

import ch.epfl.printwizard.plugin.utils.Preconditions;

import java.time.LocalDateTime;

/**
 * Represents the entire manifest file structure for manifest.json.
 * @param version Version of the manifest file
 * @param generatedAt Timestamp of when the manifest file was generated
 * @param fileLocations Locations of related files
 */
public record ManifestFile(
    String version,
    String generatedAt,
    FileLocations fileLocations
) {

    public ManifestFile {
        Preconditions.requireNonNull(version, "version is null");
        Preconditions.requireNonNull(generatedAt, "generatedAt is null");
        Preconditions.requireNonNull(fileLocations, "fileLocations is null");
        Preconditions.require(!version.isEmpty(), "version is empty");
        Preconditions.require(!generatedAt.isEmpty(), "generatedAt is empty");
    }

}
