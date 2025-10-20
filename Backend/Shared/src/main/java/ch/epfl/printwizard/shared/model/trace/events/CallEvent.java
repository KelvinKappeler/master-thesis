package ch.epfl.printwizard.shared.model.trace.events;

import ch.epfl.printwizard.shared.model.trace.TraceLoc;
import ch.epfl.printwizard.shared.utils.Preconditions;

/**
 * A CallEvent represents a method call in the trace.
 * @param eventId the unique identifier of the event
 * @param spanId the identifier of the span this event belongs to
 * @param frameId the identifier of the frame this event belongs to
 * @param location the location in the source code where the event occurred
 * @param callerMethodId the method ID of the caller
 * @param calleeMethodId the method ID of the callee
 */
public record CallEvent(
    String eventId,
    String spanId,
    String frameId,
    TraceLoc location,
    String callerMethodId,
    String calleeMethodId,
    String name
) implements TraceEvent {

    public CallEvent {
        Preconditions.requireNonNull(eventId, "eventId is null");
        Preconditions.requireNonNull(spanId, "spanId is null");
        Preconditions.requireNonNull(frameId, "frameId is null");
        Preconditions.requireNonNull(location, "location is null");
        Preconditions.requireNonNull(calleeMethodId, "calleeMethodId is null");
        Preconditions.requireNonNull(name, "name is null");
        Preconditions.require(!eventId.isEmpty(), "eventId is empty");
        Preconditions.require(!spanId.isEmpty(), "spanId is empty");
        Preconditions.require(!frameId.isEmpty(), "frameId is empty");
        Preconditions.require(!calleeMethodId.isEmpty(), "calleeMethodId is empty");
        Preconditions.require(!name.isEmpty(), "name is empty");
    }

}
