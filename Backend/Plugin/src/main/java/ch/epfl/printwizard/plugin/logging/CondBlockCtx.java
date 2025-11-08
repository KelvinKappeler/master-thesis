package ch.epfl.printwizard.plugin.logging;

import ch.epfl.printwizard.plugin.utils.Preconditions;

import java.util.List;

/**
 * Represents the context of a conditional block
 * @param conditionEventId the identifier of the condition event
 * @param conditionEvents the identifiers of the condition events in the block
 * @param thenEvents the identifiers of the 'then' events in the block
 * @param elseEvents the identifiers of the 'else' events in the block
 */
public record CondBlockCtx(String conditionEventId, List<String> conditionEvents, List<String> thenEvents, List<String> elseEvents) {
    public CondBlockCtx {
        Preconditions.requireNonNull(conditionEventId, "conditionEventId is null");
        Preconditions.requireNonNull(conditionEvents, "conditionEvents is null");
        Preconditions.requireNonNull(thenEvents, "thenEvents is null");
        Preconditions.requireNonNull(elseEvents, "elseEvents is null");
        Preconditions.require(!conditionEventId.isEmpty(), "conditionEventId is empty");
    }

}
