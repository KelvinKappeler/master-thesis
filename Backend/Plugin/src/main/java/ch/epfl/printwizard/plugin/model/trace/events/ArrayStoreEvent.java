package ch.epfl.printwizard.plugin.model.trace.events;

import ch.epfl.printwizard.plugin.model.trace.TraceLoc;
import ch.epfl.printwizard.plugin.utils.Preconditions;

/**
 * An ArrayStoreEvent represents an event where a value is stored into an array at a specific index.
 * @param eventId the unique identifier of the event
 * @param spanId the identifier of the span this event belongs to
 * @param frameId the identifier of the frame this event belongs to
 * @param location the location in the source code where the event occurred
 * @param arrayVarName the name of the array variable
 * @param arrayObjectId the ID of the array object
 * @param index the index in the array where the value is stored
 * @param value the value being stored in the array
 * @param valueObjectId the ID of the object representing the value being stored, if applicable
 * @param label ID of the label associated with this event
 * @param bodyEventId ID of the body event associated with this event, if any
 */
public record ArrayStoreEvent(
    String eventId,
    String spanId,
    String frameId,
    TraceLoc location,
    String arrayVarName,
    String arrayObjectId,
    int index,
    Object value,
    String valueObjectId,
    String label,
    String bodyEventId
) implements TraceEvent {

    public ArrayStoreEvent {
        Preconditions.requireNonNull(eventId, "eventId is null");
        Preconditions.requireNonNull(spanId, "spanId is null");
        Preconditions.requireNonNull(frameId, "frameId is null");
        Preconditions.requireNonNull(location, "location is null");
        Preconditions.requireNonNull(arrayVarName, "arrayVarName is null");
        Preconditions.requireNonNull(value, "value is null");
        Preconditions.require(!eventId.isEmpty(), "eventId is empty");
        Preconditions.require(!spanId.isEmpty(), "spanId is empty");
        Preconditions.require(!frameId.isEmpty(), "frameId is empty");
        Preconditions.require(index >= 0, "index is negative");
        Preconditions.require(!label.isEmpty(), "label is empty");
    }

}

