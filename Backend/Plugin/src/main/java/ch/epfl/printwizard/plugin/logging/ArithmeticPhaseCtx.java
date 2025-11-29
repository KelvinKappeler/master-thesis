package ch.epfl.printwizard.plugin.logging;

import ch.epfl.printwizard.plugin.model.trace.events.TraceEvent;

/**
 * Represents the context of an arithmetic phase.
 */
public class ArithmeticPhaseCtx implements ExecCtx {

    private final ArithmeticCtx arithmeticCtx;
    private final ArithmeticPhase phase;

    public ArithmeticPhaseCtx(ArithmeticCtx arithmeticCtx, ArithmeticPhase phase) {
        this.arithmeticCtx = arithmeticCtx;
        this.phase = phase;
    }

    public ArithmeticCtx getArithmeticCtx() {
        return arithmeticCtx;
    }

    public ArithmeticPhase getPhase() {
        return phase;
    }

    @Override
    public boolean handleEvent(TraceEvent e) {
        switch (phase) {
            case LEFT -> arithmeticCtx.getLeftEventIds().add(e.eventId());
            case RIGHT -> arithmeticCtx.getRightEventIds().add(e.eventId());
        }

        return true;
    }
}
