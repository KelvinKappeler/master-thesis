package ch.epfl.printwizard.plugin.logging;

import ch.epfl.printwizard.plugin.utils.Preconditions;

/**
 * Represents the context of a frame
 * @param spanId the identifier of the span that contains the frame
 * @param frameId the identifier of the frame
 * @param methodId the identifier of the method that contains the frame
 */
public record FrameCtx(String spanId, String frameId, String methodId) {

    public FrameCtx {
        Preconditions.requireNonNull(spanId, "spanId is null");
        Preconditions.requireNonNull(frameId, "frameId is null");
        Preconditions.requireNonNull(methodId, "methodId is null");
        Preconditions.require(!spanId.isEmpty(), "spanId is empty");
        Preconditions.require(!frameId.isEmpty(), "frameId is empty");
        Preconditions.require(!methodId.isEmpty(), "methodId is empty");
    }

}
