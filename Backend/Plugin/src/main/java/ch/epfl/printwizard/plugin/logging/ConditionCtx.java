package ch.epfl.printwizard.plugin.logging;

import ch.epfl.printwizard.plugin.model.trace.events.TraceEvent;
import ch.epfl.printwizard.plugin.utils.Preconditions;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the context of a condition
 */
public class ConditionCtx implements ExecCtx {

    private String conditionEventId;
    private final List<String> conditionEventIds = new ArrayList<>();
    private final List<String> thenEventIds = new ArrayList<>();
    private final List<String> elseEventIds = new ArrayList<>();

    public ConditionCtx(String conditionEventId) {
        Preconditions.requireNonNull(conditionEventId, "conditionEventId is null");
        Preconditions.require(!conditionEventId.isEmpty(), "conditionEventId is empty");

        this.conditionEventId = conditionEventId;
    }

    public String getConditionEventId() {
        return conditionEventId;
    }

    public void setConditionEventId(String conditionEventId) {
        Preconditions.requireNonNull(conditionEventId, "conditionEventId is null");
        Preconditions.require(!conditionEventId.isEmpty(), "conditionEventId is empty");

        this.conditionEventId = conditionEventId;
    }

    public List<String> getConditionEventIds() {
        return conditionEventIds;
    }

    public List<String> getThenEventIds() {
        return thenEventIds;
    }

    public List<String> getElseEventIds() {
        return elseEventIds;
    }

    @Override
    public boolean handleEvent(TraceEvent event) {

        return false;
    }
}
