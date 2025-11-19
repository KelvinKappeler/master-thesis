package ch.epfl.printwizard.plugin.logging;

import ch.epfl.printwizard.plugin.model.trace.events.TraceEvent;
import ch.epfl.printwizard.plugin.utils.Preconditions;

/**
 * Represents the context of a loop phase.
 */
public class LoopPhaseCtx implements ExecCtx {

    private LoopCtx loopCtx;
    private LoopIterationCtx iterationCtx; // null pour CONDITION/INIT
    private LoopPhase phase;

    public LoopPhaseCtx(LoopCtx loopCtx, LoopIterationCtx iterationCtx, LoopPhase phase) {
        Preconditions.requireNonNull(loopCtx, "loopCtx is null");
        Preconditions.requireNonNull(phase, "phase is null");

        this.loopCtx = loopCtx;
        this.iterationCtx = iterationCtx;
        this.phase = phase;
    }

    public LoopCtx getLoopCtx() {
        return loopCtx;
    }

    public void setLoopCtx(LoopCtx loopCtx) {
        Preconditions.requireNonNull(loopCtx, "loopCtx is null");

        this.loopCtx = loopCtx;
    }

    public LoopIterationCtx getIterationCtx() {
        return iterationCtx;
    }

    public void setIterationCtx(LoopIterationCtx iterationCtx) {
        Preconditions.requireNonNull(iterationCtx, "iterationCtx is null");

        this.iterationCtx = iterationCtx;
    }

    public LoopPhase getPhase() {
        return phase;
    }

    public void setPhase(LoopPhase phase) {
        Preconditions.requireNonNull(phase, "phase is null");

        this.phase = phase;
    }

    @Override
    public boolean handleEvent(TraceEvent event) {
        switch (phase) {
            case CONDITION -> loopCtx.getPendingConditionEvents().add(event.eventId());
            case INIT -> loopCtx.getInitEventIds().add(event.eventId());
            case BODY -> {
                if (iterationCtx != null) {
                    iterationCtx.getBodyEventIds().add(event.eventId());
                }
            }
            case UPDATE    -> {
                if (iterationCtx != null) {
                    iterationCtx.getUpdateEventIds().add(event.eventId());
                }
            }
        }

        return true;
    }
}
