package ch.epfl.printwizard.plugin.model.trace.events;

import ch.epfl.printwizard.plugin.model.trace.TraceLoc;
import ch.epfl.printwizard.plugin.utils.Preconditions;

/**
 * A NewEvent represents the creation of a new object in the trace.
 * @param eventId the unique identifier of the event
 * @param spanId the identifier of the span this event belongs to
 * @param location the location in the source code where the event occurred
 * @param objectId the ID of the created object
 * @param typeName the type name of the created object
 */
public record NewEvent(
    String eventId,
    String spanId,
    TraceLoc location,
    String objectId,
    String typeName
) implements TraceEvent {

    public NewEvent {
        Preconditions.requireNonNull(eventId, "eventId is null");
        Preconditions.requireNonNull(spanId, "spanId is null");
        Preconditions.requireNonNull(location, "location is null");
        Preconditions.requireNonNull(objectId, "valueObjectId is null");
        Preconditions.requireNonNull(typeName, "typeName is null");
        Preconditions.require(!eventId.isEmpty(), "eventId is empty");
        Preconditions.require(!spanId.isEmpty(), "spanId is empty");
        Preconditions.require(!objectId.isEmpty(), "valueObjectId is empty");
        Preconditions.require(!typeName.isEmpty(), "typeName is empty");
    }

}
