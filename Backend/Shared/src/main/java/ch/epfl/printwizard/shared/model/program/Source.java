package ch.epfl.printwizard.shared.model.program;

import ch.epfl.printwizard.shared.utils.Preconditions;

/**
 * Represents a source for the program.json file. A source is a file containing code.
 * @param sourceId ID of the source
 * @param path Path of the source
 * @param language Programming language of the source
 * @param lines Number of lines of code in the source
 */
public record Source(
    String sourceId,
    String path,
    String language,
    int lines,
    String sourceContent
) {
    
    public Source {
        Preconditions.requireNonNull(sourceId, "sourceId is null");
        Preconditions.requireNonNull(path, "path is null");
        Preconditions.requireNonNull(language, "language is null");
        Preconditions.requireNonNull(sourceContent, "sourceContent is null");
        Preconditions.require(!sourceId.isEmpty(), "sourceId is empty");
        Preconditions.require(!path.isEmpty(), "path is empty");
        Preconditions.require(!language.isEmpty(), "language is empty");
        Preconditions.require(lines >= 0, "lines is negative");
    }
    
}
