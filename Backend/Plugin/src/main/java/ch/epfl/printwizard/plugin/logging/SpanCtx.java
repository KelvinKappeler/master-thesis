package ch.epfl.printwizard.plugin.logging;

import ch.epfl.printwizard.plugin.utils.Preconditions;

/**
 * Represents the context of a frame
 * @param spanId the identifier of the span that contains the frame
 * @param methodId the identifier of the method that contains the frame
 */
public record SpanCtx(String spanId, String methodId) {

    public SpanCtx {
        Preconditions.requireNonNull(spanId, "spanId is null");
        Preconditions.requireNonNull(methodId, "methodId is null");
        Preconditions.require(!spanId.isEmpty(), "spanId is empty");
        Preconditions.require(!methodId.isEmpty(), "methodId is empty");
    }

}
