package ch.epfl.printwizard.plugin.logging;

import ch.epfl.printwizard.plugin.model.trace.TraceLoc;
import ch.epfl.printwizard.plugin.model.trace.events.LoopKind;
import ch.epfl.printwizard.plugin.model.trace.events.TraceEvent;
import ch.epfl.printwizard.plugin.utils.Preconditions;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the context of a loop.
 */
public class LoopCtx implements ExecCtx {

    private String loopEventId;
    private String spanId;
    private String frameId;
    private TraceLoc location;
    private LoopKind kind;

    private int nextIterationIndex;

    private final List<String> initEventIds = new ArrayList<>();
    private final List<String> iterationEventIds = new ArrayList<>();
    private final List<String> pendingConditionEvents = new ArrayList<>();

    public LoopCtx(String loopEventId, String spanId, String frameId, TraceLoc location, LoopKind kind) {
        Preconditions.requireNonNull(loopEventId, "loopEventId is null");
        Preconditions.requireNonNull(spanId, "spanId is null");
        Preconditions.requireNonNull(frameId, "frameId is null");
        Preconditions.requireNonNull(location, "location is null");
        Preconditions.requireNonNull(kind, "kind is null");
        Preconditions.require(!loopEventId.isEmpty(), "loopEventId is empty");
        Preconditions.require(!spanId.isEmpty(), "spanId is empty");
        Preconditions.require(!frameId.isEmpty(), "frameId is empty");

        this.loopEventId = loopEventId;
        this.spanId = spanId;
        this.frameId = frameId;
        this.location = location;
        this.kind = kind;
        this.nextIterationIndex = 0;
    }

    public String getLoopEventId() {
        return loopEventId;
    }

    public void setLoopEventId(String loopEventId) {
        Preconditions.requireNonNull(loopEventId, "loopEventId is null");
        Preconditions.require(!loopEventId.isEmpty(), "loopEventId is empty");

        this.loopEventId = loopEventId;
    }

    public String getSpanId() {
        return spanId;
    }

    public void setSpanId(String spanId) {
        Preconditions.requireNonNull(spanId, "spanId is null");
        Preconditions.require(!spanId.isEmpty(), "spanId is empty");

        this.spanId = spanId;
    }

    public String getFrameId() {
        return frameId;
    }

    public void setFrameId(String frameId) {
        Preconditions.requireNonNull(frameId, "frameId is null");
        Preconditions.require(!frameId.isEmpty(), "frameId is empty");

        this.frameId = frameId;
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

    public void setKind(LoopKind kind) {
        this.kind = kind;
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

        return false;
    }
}
