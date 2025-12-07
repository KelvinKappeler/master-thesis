package ch.epfl.printwizard.plugin.model.trace;

import ch.epfl.printwizard.plugin.utils.Preconditions;

import java.util.List;

/**
 * Represents a trace span with various attributes.
 * @param spanId Span identifier
 * @param parentSpanId Parent span identifier
 * @param methodId Method identifier
 * @param thisRef Object id of the 'this' reference
 * @param args List of arguments
 * @param startEventId Start event identifier
 * @param endEventId End event identifier
 * @param startLoc Start location
 * @param endLoc End location
 */
public record TraceSpan(
    String spanId,
    String parentSpanId,
    String methodId,
    String thisRef,
    List<Arg> args,
    String startEventId,
    String endEventId,
    TraceLoc startLoc,
    TraceLoc endLoc
) {

    public TraceSpan {
        Preconditions.requireNonNull(spanId, "spanId is null");
        Preconditions.require(!spanId.isEmpty(), "spanId is empty");
        Preconditions.requireNonNull(methodId, "methodId is null");
        Preconditions.require(!methodId.isEmpty(), "methodId is empty");
        Preconditions.requireNonNull(args, "args is null");
        Preconditions.requireNonNull(startEventId, "startEventId is null");
        Preconditions.require(!startEventId.isEmpty(), "startEventId is empty");
        Preconditions.requireNonNull(endEventId, "endEventId is null");
        Preconditions.require(!endEventId.isEmpty(), "endEventId is empty");
        Preconditions.requireNonNull(startLoc, "startLoc is null");
        Preconditions.requireNonNull(endLoc, "endLoc is null");
    }

}
