package ch.epfl.printwizard.plugin.logging;

import ch.epfl.printwizard.plugin.model.trace.TraceLoc;
import ch.epfl.printwizard.plugin.model.trace.events.TraceEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the context of a return operation.
 */
public class ReturnCtx implements ExecCtx {

    private final String returnEventId;
    private final String spanId;
    private final String frameId;
    private final String methodId;
    private final TraceLoc location;
    
    private final List<String> childEventIds = new ArrayList<>();

    public ReturnCtx(
        String returnEventId,
        String spanId,
        String frameId,
        String methodId,
        TraceLoc location
    ) {
        this.returnEventId = returnEventId;
        this.spanId = spanId;
        this.frameId = frameId;
        this.methodId = methodId;
        this.location = location;
    }

    public String getReturnEventId() {
        return returnEventId;
    }

    public String getSpanId() {
        return spanId;
    }

    public String getFrameId() {
        return frameId;
    }

    public String getMethodId() {
        return methodId;
    }

    public TraceLoc getLocation() {
        return location;
    }

    public List<String> getChildEventIds() {
        return childEventIds;
    }
    
    @Override
    public boolean handleEvent(TraceEvent event) {
        if (event != null) {
            childEventIds.add(event.eventId());
        }

        return true;
    }
}
