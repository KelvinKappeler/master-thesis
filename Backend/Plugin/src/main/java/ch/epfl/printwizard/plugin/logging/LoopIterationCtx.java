package ch.epfl.printwizard.plugin.logging;

import ch.epfl.printwizard.plugin.model.trace.events.EventValue;
import ch.epfl.printwizard.plugin.model.trace.events.TraceEvent;
import ch.epfl.printwizard.plugin.utils.Ids;
import ch.epfl.printwizard.plugin.utils.Preconditions;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the context of a loop iteration.
 */
public class LoopIterationCtx implements ExecCtx {

    private LoopCtx loopCtx;
    private String iterationEventId;
    private int iterationIndex;
    private final List<String> conditionEventIds = new ArrayList<>();
    private final List<String> bodyEventIds = new ArrayList<>();
    private final List<String> updateEventIds = new ArrayList<>();
    
    private EventValue conditionValue;

    public LoopIterationCtx(LoopCtx loopCtx, int iterationIndex, String iterationEventId) {
        Preconditions.requireNonNull(loopCtx, "loopCtx is null");
        Preconditions.requireNonNull(iterationEventId, "iterationEventId is null");
        Preconditions.require(iterationIndex >= 0, "iterationIndex is negative");
        Preconditions.require(!iterationEventId.isEmpty(), "iterationEventId is empty");

        this.loopCtx = loopCtx;
        this.iterationIndex = iterationIndex;
        this.iterationEventId = iterationEventId;
    }

    public LoopCtx getLoopCtx() {
        return loopCtx;
    }

    public void setLoopCtx(LoopCtx loopCtx) {
        Preconditions.requireNonNull(loopCtx, "loopCtx is null");

        this.loopCtx = loopCtx;
    }

    public String getIterationEventId() {
        return iterationEventId;
    }

    public void setIterationEventId(String iterationEventId) {
        Preconditions.requireNonNull(iterationEventId, "iterationEventId is null");
        Preconditions.require(!iterationEventId.isEmpty(), "iterationEventId is empty");

        this.iterationEventId = iterationEventId;
    }

    public int getIterationIndex() {
        return iterationIndex;
    }

    public void setIterationIndex(int iterationIndex) {
        Preconditions.require(iterationIndex >= 0, "iterationIndex is negative");

        this.iterationIndex = iterationIndex;
    }

    public EventValue getConditionValue() {
        return conditionValue;
    }

    public void setConditionValue(EventValue conditionValue) {
        Preconditions.requireNonNull(conditionValue, "conditionValue is null");
        
        this.conditionValue = conditionValue;
    }

    public List<String> getConditionEventIds() {
        return conditionEventIds;
    }

    public List<String> getBodyEventIds() {
        return bodyEventIds;
    }

    public List<String> getUpdateEventIds() {
        return updateEventIds;
    }

    @Override
    public boolean handleEvent(TraceEvent event) {

        return false;
    }
}
