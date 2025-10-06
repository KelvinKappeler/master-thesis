package ch.epfl.printwizard.shared.model.trace;

import ch.epfl.printwizard.shared.model.trace.events.TraceEvent;
import ch.epfl.printwizard.shared.utils.Preconditions;

import java.util.List;

public record TraceFile(
    String traceId,
    List<TraceSpan> spans,
    List<TraceFrame> frames,
    List<TraceEvent> events
) {
    public TraceFile {
        Preconditions.requireNonNull(traceId, "traceId is null");
        Preconditions.require(!traceId.isEmpty(), "traceId is empty");
        Preconditions.requireNonNull(spans, "spans is null");
        Preconditions.requireNonNull(frames, "frames is null");
        Preconditions.requireNonNull(events, "events is null");
    }
}
