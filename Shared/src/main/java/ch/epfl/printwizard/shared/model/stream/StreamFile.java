package ch.epfl.printwizard.shared.model.stream;

import ch.epfl.printwizard.shared.utils.Preconditions;

import java.util.List;

/**
 * Represents the entire stream file structure for stream.json.
 * @param entries List of BaseEntryStream objects
 */
public record StreamFile(
    List<BaseEntryStream> entries
) {
    public StreamFile {
        Preconditions.requireNonNull(entries, "entries");
    }
}
