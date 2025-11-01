package ch.epfl.printwizard.plugin.model.trace;

import ch.epfl.printwizard.plugin.utils.Preconditions;

import java.util.List;

public record TraceFrame(
    String frameId,
    String spanId,
    String methodId,
    String thisRef,
    List<Arg> args
) {

    public TraceFrame {
        Preconditions.requireNonNull(frameId, "frameId is null");
        Preconditions.requireNonNull(spanId, "spanId is null");
        Preconditions.requireNonNull(methodId, "methodId is null");
        Preconditions.requireNonNull(args, "args is null");
        Preconditions.require(!frameId.isEmpty(), "frameId is empty");
        Preconditions.require(!spanId.isEmpty(), "spanId is empty");
        Preconditions.require(!methodId.isEmpty(), "methodId is empty");
    }

}
