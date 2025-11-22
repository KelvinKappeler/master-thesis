package ch.epfl.printwizard.plugin.model.state;

import ch.epfl.printwizard.plugin.utils.Preconditions;

import java.util.Map;

/**
 * Represents a snapshot of the state for a versioned object.
 * @param version The version of the snapshot.
 * @param eventId The identifier of the event that caused the snapshot.
 * @param fields The fields of the versioned object at the time of the snapshot.
 */
public record StateSnapshot(
    int version,
    String eventId,
    Map<String, FieldState> fields
) {

    public StateSnapshot {
        Preconditions.requireNonNull(eventId, "eventId is null");
        Preconditions.requireNonNull(fields, "fields is null");
        Preconditions.require(version >= 0, "version is not positive");
        Preconditions.require(!eventId.isEmpty(), "eventId is empty");
    }

}
