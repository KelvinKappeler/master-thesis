package ch.epfl.printwizard.plugin.model.trace.events;

import ch.epfl.printwizard.plugin.model.trace.TraceLoc;
import ch.epfl.printwizard.plugin.utils.Preconditions;

/**
 * A LoopEvent represents an event where a loop is executed.
 * @param eventId the unique identifier of the event
 * @param spanId the identifier of the span this event belongs to
 * @param frameId the identifier of the frame this event belongs to
 * @param location the location in the source code where the event occurred
 * @param loopKind the kind of loop that is executed
 * @param initEventIds the identifiers of events executed in the loop initialization
 * @param iterationsEventIds the identifiers of the loop iteration events
 */
public record LoopEvent(
    String eventId,
    String spanId,
    String frameId,
    TraceLoc location,
    LoopKind loopKind,
    String[] initEventIds,
    String[] iterationsEventIds
) implements TraceEvent {
    
    public LoopEvent {
        Preconditions.requireNonNull(eventId, "eventId is null");
        Preconditions.requireNonNull(spanId, "spanId is null");
        Preconditions.requireNonNull(frameId, "frameId is null");
        Preconditions.requireNonNull(location, "location is null");
        Preconditions.requireNonNull(loopKind, "loopKind is null");
        Preconditions.requireNonNull(initEventIds, "init is null");
        Preconditions.requireNonNull(iterationsEventIds, "iterations is null");
        Preconditions.require(!eventId.isEmpty(), "eventId is empty");
        Preconditions.require(!spanId.isEmpty(), "spanId is empty");
        Preconditions.require(!frameId.isEmpty(), "frameId is empty");
    }
    
}
