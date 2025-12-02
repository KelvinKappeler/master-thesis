package ch.epfl.printwizard.plugin.model.trace.events;

import ch.epfl.printwizard.plugin.model.trace.TraceLoc;
import ch.epfl.printwizard.plugin.utils.Preconditions;

/**
 * A PutFieldEvent represents an event where a field of an object is modified.
 * @param eventId the unique identifier of the event
 * @param spanId the identifier of the span this event belongs to
 * @param frameId the identifier of the frame this event belongs to
 * @param location the location in the source code where the event occurred
 * @param objectId the ID of the object whose field is being modified
 * @param fieldName the name of the field being modified
 * @param value the new value assigned to the field
 * @param fieldType the type of the field being modified
 * @param bodyEventId ID of the body event associated with this field write event, if any
 */
public record FieldWriteEvent(
    String eventId,
    String spanId,
    String frameId,
    TraceLoc location,
    String objectId,
    String fieldName,
    EventValue value,
    String fieldType,
    String bodyEventId
) implements TraceEvent {

    public FieldWriteEvent {
        Preconditions.requireNonNull(eventId, "eventId is null");
        Preconditions.requireNonNull(spanId, "spanId is null");
        Preconditions.requireNonNull(frameId, "frameId is null");
        Preconditions.requireNonNull(location, "location is null");
        Preconditions.requireNonNull(objectId, "valueObjectId is null");
        Preconditions.requireNonNull(fieldName, "fieldName is null");
        Preconditions.requireNonNull(fieldType, "fieldType is null");
        Preconditions.requireNonNull(value, "value is null");
        Preconditions.require(!eventId.isEmpty(), "eventId is empty");
        Preconditions.require(!spanId.isEmpty(), "spanId is empty");
        Preconditions.require(!frameId.isEmpty(), "frameId is empty");
        Preconditions.require(!objectId.isEmpty(), "valueObjectId is empty");
        Preconditions.require(!fieldName.isEmpty(), "fieldName is empty");
        Preconditions.require(!fieldType.isEmpty(), "fieldType is empty");
    }

}
