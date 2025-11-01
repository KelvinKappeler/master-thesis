package ch.epfl.printwizard.plugin.model.trace.events;

import ch.epfl.printwizard.plugin.model.trace.TraceLoc;
import ch.epfl.printwizard.plugin.utils.Preconditions;

/**
 * A NewEvent represents the creation of a new object in the trace.
 * @param eventId the unique identifier of the event
 * @param spanId the identifier of the span this event belongs to
 * @param frameId the identifier of the frame this event belongs to
 * @param location the location in the source code where the event occurred
 * @param typeId the resultType ID of the created object
 * @param refId the reference ID of the created object
 */
public record NewEvent(
        String eventId,
        String spanId,
        String frameId,
        TraceLoc location,
        String typeId,
        String refId
) implements TraceEvent {

    public NewEvent {
        Preconditions.requireNonNull(eventId, "eventId is null");
        Preconditions.requireNonNull(spanId, "spanId is null");
        Preconditions.requireNonNull(frameId, "frameId is null");
        Preconditions.requireNonNull(location, "location is null");
        Preconditions.requireNonNull(typeId, "callerMethodId is null");
        Preconditions.requireNonNull(refId, "calleeMethodId is null");
        Preconditions.require(!eventId.isEmpty(), "eventId is empty");
        Preconditions.require(!spanId.isEmpty(), "spanId is empty");
        Preconditions.require(!frameId.isEmpty(), "frameId is empty");
        Preconditions.require(!typeId.isEmpty(), "callerMethodId is empty");
        Preconditions.require(!refId.isEmpty(), "calleeMethodId is empty");
    }

}
