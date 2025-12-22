package ch.epfl.printwizard.plugin.logging;

import ch.epfl.printwizard.plugin.model.trace.TraceLoc;
import ch.epfl.printwizard.plugin.model.trace.events.LoopIterationEvent;
import ch.epfl.printwizard.plugin.model.trace.events.LoopKind;
import ch.epfl.printwizard.plugin.model.trace.events.TraceEvent;
import ch.epfl.printwizard.plugin.utils.Preconditions;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the context of a loop.
 */
public class LoopCtx implements ExecCtx {

    private final String loopEventId;
    private String spanId;
    private TraceLoc location;
    private final LoopKind kind;

    private int nextIterationIndex;

    private final List<String> initEventIds = new ArrayList<>();
    private final List<String> iterationEventIds = new ArrayList<>();
    private final List<String> pendingConditionEvents = new ArrayList<>();

    public LoopCtx(String loopEventId, String spanId, TraceLoc location, LoopKind kind) {
        Preconditions.requireNonNull(loopEventId, "loopEventId is null");
        Preconditions.requireNonNull(spanId, "spanId is null");
        Preconditions.requireNonNull(location, "location is null");
        Preconditions.requireNonNull(kind, "kind is null");
        Preconditions.require(!loopEventId.isEmpty(), "loopEventId is empty");
        Preconditions.require(!spanId.isEmpty(), "spanId is empty");

        this.loopEventId = loopEventId;
        this.spanId = spanId;
        this.location = location;
        this.kind = kind;
        this.nextIterationIndex = 0;
    }

    public String getLoopEventId() {
        return loopEventId;
    }

    public String getSpanId() {
        return spanId;
    }

    public void setSpanId(String spanId) {
        Preconditions.requireNonNull(spanId, "spanId is null");
        Preconditions.require(!spanId.isEmpty(), "spanId is empty");

        this.spanId = spanId;
    }

    public TraceLoc getLocation() {
        return location;
    }

    public void setLocation(TraceLoc location) {
        Preconditions.requireNonNull(location, "location is null");

        this.location = location;
    }

    public LoopKind getKind() {
        return kind;
    }

    public int getNextIterationIndex() {
        return nextIterationIndex;
    }

    public void setNextIterationIndex(int nextIterationIndex) {
        Preconditions.require(nextIterationIndex >= 0, "nextIterationIndex is negative");

        this.nextIterationIndex = nextIterationIndex;
    }

    public List<String> getInitEventIds() {
        return initEventIds;
    }

    public List<String> getIterationEventIds() {
        return iterationEventIds;
    }

    public List<String> getPendingConditionEvents() {
        return pendingConditionEvents;
    }

    @Override
    public boolean handleEvent(TraceEvent event) {
        if (event instanceof LoopIterationEvent lie) {
            iterationEventIds.add(lie.eventId());
            
            return true;
        }
        
        return false;
    }
}
