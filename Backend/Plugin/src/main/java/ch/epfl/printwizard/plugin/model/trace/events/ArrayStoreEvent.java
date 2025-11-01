package ch.epfl.printwizard.plugin.model.trace.events;

import ch.epfl.printwizard.plugin.model.trace.TraceLoc;
import ch.epfl.printwizard.plugin.utils.Preconditions;

/**
 * An ArrayStoreEvent represents an event where a value is stored into an array at a specific index.
 * @param eventId the unique identifier of the event
 * @param spanId the identifier of the span this event belongs to
 * @param frameId the identifier of the frame this event belongs to
 * @param location the location in the source code where the event occurred
 * @param arrayRef the reference ID of the array being modified
 * @param index the index in the array where the value is stored
 * @param value the value being stored in the array
 */
public record ArrayStoreEvent(
        String eventId,
        String spanId,
        String frameId,
        TraceLoc location,
        String arrayRef,
        int index,
        Object value
) implements TraceEvent {

    public ArrayStoreEvent {
        Preconditions.requireNonNull(eventId, "eventId is null");
        Preconditions.requireNonNull(spanId, "spanId is null");
        Preconditions.requireNonNull(frameId, "frameId is null");
        Preconditions.requireNonNull(location, "location is null");
        Preconditions.requireNonNull(arrayRef, "arrayRef is null");
        Preconditions.require(!eventId.isEmpty(), "eventId is empty");
        Preconditions.require(!spanId.isEmpty(), "spanId is empty");
        Preconditions.require(!frameId.isEmpty(), "frameId is empty");
        Preconditions.require(index >= 0, "index is negative");
    }

}

