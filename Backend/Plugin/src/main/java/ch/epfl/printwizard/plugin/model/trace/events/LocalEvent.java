package ch.epfl.printwizard.plugin.model.trace.events;

import ch.epfl.printwizard.plugin.model.trace.TraceLoc;
import ch.epfl.printwizard.plugin.utils.Preconditions;

/**
 * A LocalEvent represents a modification to a local variable or field within a specific method frame during the execution of a program.
 * @param eventId the unique identifier of the event
 * @param spanId the identifier of the span this event belongs to
 * @param frameId the identifier of the frame this event belongs to
 * @param location the location in the source code where the event occurred
 * @param owner the class that owns the field being modified
 * @param method the method in which the field modification occurs
 * @param varName the name of the field being modified
 * @param index the index of the variable in case of an array or list, -1 if not applicable
 * @param value the new value being assigned to the field
 */
public record LocalEvent(
        String eventId,
        String spanId,
        String frameId,
        TraceLoc location,
        String owner,
        String method,
        String varName,
        int index,
        Object value
) implements TraceEvent {

    public LocalEvent {
        Preconditions.requireNonNull(eventId, "eventId is null");
        Preconditions.requireNonNull(spanId, "spanId is null");
        Preconditions.requireNonNull(frameId, "frameId is null");
        Preconditions.requireNonNull(location, "location is null");
        Preconditions.requireNonNull(owner, "owner is null");
        Preconditions.requireNonNull(method, "field is null");
        Preconditions.requireNonNull(varName, "description is null");
        Preconditions.require(!eventId.isEmpty(), "eventId is empty");
        Preconditions.require(!spanId.isEmpty(), "spanId is empty");
        Preconditions.require(!frameId.isEmpty(), "frameId is empty");
        Preconditions.require(!owner.isEmpty(), "owner is empty");
        Preconditions.require(!method.isEmpty(), "field is empty");
        Preconditions.require(!varName.isEmpty(), "description is empty");
        Preconditions.require(index >= -1, "index is less than -1");
    }

}
