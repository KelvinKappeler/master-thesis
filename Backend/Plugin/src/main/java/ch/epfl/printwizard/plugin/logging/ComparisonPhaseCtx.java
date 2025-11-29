package ch.epfl.printwizard.plugin.logging;

import ch.epfl.printwizard.plugin.model.trace.events.CallEvent;
import ch.epfl.printwizard.plugin.model.trace.events.TraceEvent;

import java.util.Objects;

/**
 * Represents the context of a comparison phase.
 */
public class ComparisonPhaseCtx implements ExecCtx {

    private final ComparisonCtx comparisonCtx;
    private final ComparisonPhase phase;

    public ComparisonPhaseCtx(ComparisonCtx comparisonCtx, ComparisonPhase phase) {
        this.comparisonCtx = comparisonCtx;
        this.phase = phase;
    }

    public ComparisonCtx getComparisonCtx() {
        return comparisonCtx;
    }

    public ComparisonPhase getPhase() {
        return phase;
    }

    @Override
    public boolean handleEvent(TraceEvent e) {
        switch (phase) {
            case LEFT -> comparisonCtx.getLeftEventIds().add(e.eventId());
            case RIGHT -> comparisonCtx.getRightEventIds().add(e.eventId());
        }

        return true;
    }
}
