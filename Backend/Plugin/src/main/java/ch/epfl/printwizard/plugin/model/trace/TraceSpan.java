package ch.epfl.printwizard.plugin.model.trace;

import ch.epfl.printwizard.plugin.utils.Preconditions;

/**
 * Represents a trace span with various attributes.
 * @param spanId Span identifier
 * @param parentSpanId Parent span identifier
 * @param methodId Method identifier
 * @param startEventId Start event identifier
 * @param endEventId End event identifier
 * @param startLoc Start location
 * @param endLoc End location
 * @param status Status of the span (e.g., "OK", "ERROR")
 */
public record TraceSpan(
    String spanId,
    String parentSpanId,
    String methodId,
    String startEventId,
    String endEventId,
    TraceLoc startLoc,
    TraceLoc endLoc,
    String status
) {

    public TraceSpan {
        Preconditions.requireNonNull(spanId, "spanId is null");
        Preconditions.require(!spanId.isEmpty(), "spanId is empty");
        Preconditions.requireNonNull(methodId, "methodId is null");
        Preconditions.require(!methodId.isEmpty(), "methodId is empty");
        Preconditions.requireNonNull(startEventId, "startEventId is null");
        Preconditions.require(!startEventId.isEmpty(), "startEventId is empty");
        Preconditions.requireNonNull(endEventId, "endEventId is null");
        Preconditions.require(!endEventId.isEmpty(), "endEventId is empty");
        Preconditions.requireNonNull(startLoc, "startLoc is null");
        Preconditions.requireNonNull(endLoc, "endLoc is null");
        Preconditions.requireNonNull(status, "status is null");
    }

}
