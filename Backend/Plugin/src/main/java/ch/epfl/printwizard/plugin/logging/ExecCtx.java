package ch.epfl.printwizard.plugin.logging;

import ch.epfl.printwizard.plugin.model.trace.events.TraceEvent;

/**
 * Represents the base interface for a context of execution.
 */
public interface ExecCtx {

    /**
     * Handles the given event.
     * @param event the event to handle
     * @return true, if the context handled the event, false otherwise
     */
    boolean handleEvent(TraceEvent event);

}
