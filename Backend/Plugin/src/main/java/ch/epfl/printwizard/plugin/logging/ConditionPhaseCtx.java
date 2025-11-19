package ch.epfl.printwizard.plugin.logging;

import ch.epfl.printwizard.plugin.model.trace.events.LoopIterationEvent;
import ch.epfl.printwizard.plugin.model.trace.events.TraceEvent;
import ch.epfl.printwizard.plugin.utils.Preconditions;

/**
 * Represents the context of a condition phase.
 */
public class ConditionPhaseCtx implements ExecCtx {

    private ConditionCtx owner;
    private ConditionPhase phase;

    public ConditionPhaseCtx(ConditionCtx owner, ConditionPhase phase) {
        Preconditions.requireNonNull(owner, "owner is null");
        Preconditions.requireNonNull(phase, "phase is null");

        this.owner = owner;
        this.phase = phase;
    }

    public ConditionCtx getOwner() {
        return owner;
    }

    public void setOwner(ConditionCtx owner) {
        Preconditions.requireNonNull(owner, "owner is null");

        this.owner = owner;
    }

    public ConditionPhase getPhase() {
        return phase;
    }

    public void setPhase(ConditionPhase phase) {
        Preconditions.requireNonNull(phase, "phase is null");

        this.phase = phase;
    }

    @Override
    public boolean handleEvent(TraceEvent event) {
        if (event instanceof LoopIterationEvent) {

            return false;
        }

        switch (phase) {
            case CONDITION_EXPR -> owner.getConditionEventIds().add(event.eventId());
            case THEN_BLOCK -> owner.getThenEventIds().add(event.eventId());
            case ELSE_BLOCK -> owner.getElseEventIds().add(event.eventId());
        }

        return true;
    }
}
