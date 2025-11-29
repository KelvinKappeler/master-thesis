package ch.epfl.printwizard.plugin.model.trace.events;

import ch.epfl.printwizard.plugin.model.trace.TraceLoc;
import ch.epfl.printwizard.plugin.utils.Preconditions;

/**
 * A LocalEvent represents a modification to a local variable during the execution of a program.
 * @param eventId the unique identifier of the event
 * @param spanId the identifier of the span this event belongs to
 * @param frameId the identifier of the frame this event belongs to
 * @param location the location in the source code where the event occurred
 * @param method the method in which the modification took place
 * @param varName the name of the local variable being modified
 * @param value the new value assigned to the local variable
 * @param label ID of the label associated with this event
 * @param bodyEventId ID of the body event associated with this event, if any
 */
public record LocalEvent(
    String eventId,
    String spanId,
    String frameId,
    TraceLoc location,
    String method,
    String varName,
    EventValue value,
    String label,
    String bodyEventId
) implements TraceEvent {

    public LocalEvent {
        Preconditions.requireNonNull(eventId, "eventId is null");
        Preconditions.requireNonNull(spanId, "spanId is null");
        Preconditions.requireNonNull(frameId, "frameId is null");
        Preconditions.requireNonNull(location, "location is null");
        Preconditions.requireNonNull(method, "field is null");
        Preconditions.requireNonNull(varName, "description is null");
        Preconditions.requireNonNull(label, "label is null");
        Preconditions.requireNonNull(value, "value is null");
        Preconditions.require(!eventId.isEmpty(), "eventId is empty");
        Preconditions.require(!spanId.isEmpty(), "spanId is empty");
        Preconditions.require(!frameId.isEmpty(), "frameId is empty");
        Preconditions.require(!method.isEmpty(), "field is empty");
        Preconditions.require(!varName.isEmpty(), "description is empty");
        Preconditions.require(!label.isEmpty(), "label is empty");
    }

}
