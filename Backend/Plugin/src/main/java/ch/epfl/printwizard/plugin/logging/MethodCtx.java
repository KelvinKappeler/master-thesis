package ch.epfl.printwizard.plugin.logging;

import ch.epfl.printwizard.plugin.model.trace.events.TraceEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the context of a method.
 */
public class MethodCtx implements ExecCtx {
    private final String callEventId;
    private final String spanId;
    private final String frameId;
    private final List<String> bodyEventIds = new ArrayList<>();

    public MethodCtx(String callEventId, String spanId, String frameId) {
        this.callEventId = callEventId;
        this.spanId = spanId;
        this.frameId = frameId;
    }

    @Override
    public boolean handleEvent(TraceEvent event) {
        bodyEventIds.add(event.eventId());
        
        return true;
    }

    /**
     * Gets the event id of the method call.
     * @return the event id of the method call
     */
    public String getCallEventId() {
        return callEventId;
    }

    /**
     * Gets the span id of the method.
     * @return the span id of the method
     */
    public String getSpanId() {
        return spanId;
    }

    /**
     * Gets the frame id of the method.
     * @return the frame id of the method
     */
    public String getFrameId() {
        return frameId;
    }

    /**
     * Gets the list of event ids associated with the method.
     * @return the list of event ids associated with the method
     */
    public List<String> getBodyEventIds() {
        return bodyEventIds;
    }
}
