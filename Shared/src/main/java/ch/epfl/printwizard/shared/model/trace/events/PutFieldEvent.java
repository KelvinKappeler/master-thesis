package ch.epfl.printwizard.shared.model.trace.events;

import ch.epfl.printwizard.shared.model.trace.TraceLoc;
import ch.epfl.printwizard.shared.utils.Preconditions;

/**
 * A PutFieldEvent represents an event where a field of an object is modified.
 * @param eventId the unique identifier of the event
 * @param spanId the identifier of the span this event belongs to
 * @param frameId the identifier of the frame this event belongs to
 * @param location the location in the source code where the event occurred
 * @param owner the class that owns the field being modified
 * @param field the name of the field being modified
 * @param description a description of the field being modified (e.g., its type)
 * @param instanceRef a reference to the instance whose field is being modified
 * @param value the new value being assigned to the field
 */
public record PutFieldEvent(
        String eventId,
        String spanId,
        String frameId,
        TraceLoc location,
        String owner,
        String field,
        String description,
        String instanceRef,
        Object value
) implements TraceEvent {

    public PutFieldEvent {
        Preconditions.requireNonNull(eventId, "eventId is null");
        Preconditions.requireNonNull(spanId, "spanId is null");
        Preconditions.requireNonNull(frameId, "frameId is null");
        Preconditions.requireNonNull(location, "location is null");
        Preconditions.requireNonNull(owner, "owner is null");
        Preconditions.requireNonNull(field, "field is null");
        Preconditions.requireNonNull(description, "description is null");
        Preconditions.requireNonNull(instanceRef, "instanceRef is null");
        Preconditions.require(!eventId.isEmpty(), "eventId is empty");
        Preconditions.require(!spanId.isEmpty(), "spanId is empty");
        Preconditions.require(!frameId.isEmpty(), "frameId is empty");
        Preconditions.require(!owner.isEmpty(), "owner is empty");
        Preconditions.require(!field.isEmpty(), "field is empty");
        Preconditions.require(!description.isEmpty(), "description is empty");
        Preconditions.require(!instanceRef.isEmpty(), "instanceRef is empty");
    }

}
