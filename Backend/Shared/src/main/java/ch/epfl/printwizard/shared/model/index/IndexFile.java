package ch.epfl.printwizard.shared.model.index;

import ch.epfl.printwizard.shared.utils.Preconditions;

import java.util.List;
import java.util.Map;

/**
 * Represents an index file containing mappings of various elements by line, object, and span.
 * @param byLine Events mapped by line
 * @param byObject Events mapped by object
 * @param bySpan Events mapped by span
 */
public record IndexFile(
    Map<String, Map<String, List<String>>> byLine,
    Map<String, List<String>> byObject,
    Map<String, List<String>> bySpan
) {

    public IndexFile {
        Preconditions.requireNonNull(byLine, "byLine cannot be null");
        Preconditions.requireNonNull(byObject, "byObject cannot be null");
        Preconditions.requireNonNull(bySpan, "bySpan cannot be null");
    }

}
