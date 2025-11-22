package ch.epfl.printwizard.plugin.model.state;

import ch.epfl.printwizard.plugin.utils.Preconditions;

import java.util.List;

/**
 * Represents a timeline of a versioned object.
 * @param objectId The identifier of the object.
 * @param type The type of the object.
 * @param timeline The timeline of the object.
 */
public record ObjectTimeline(
    String objectId,
    String type,
    List<StateSnapshot> timeline
) {

    public ObjectTimeline {
        Preconditions.requireNonNull(objectId, "valueObjectId is null");
        Preconditions.requireNonNull(type, "type is null");
        Preconditions.requireNonNull(timeline, "timeline is null");
        Preconditions.require(!objectId.isEmpty(), "valueObjectId is empty");
        Preconditions.require(!type.isEmpty(), "type is empty");
    }

}
