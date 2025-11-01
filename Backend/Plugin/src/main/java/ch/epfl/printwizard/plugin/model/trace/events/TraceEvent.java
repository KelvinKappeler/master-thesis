package ch.epfl.printwizard.plugin.model.trace.events;

import ch.epfl.printwizard.plugin.model.trace.TraceLoc;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Represents a trace event in the execution of a program.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ArrayStoreEvent.class, name = "ARRAYSTORE"),
    @JsonSubTypes.Type(value = CallEvent.class, name = "CALL"),
    @JsonSubTypes.Type(value = LocalEvent.class, name = "LOCAL"),
    @JsonSubTypes.Type(value = NewEvent.class, name = "NEW"),
    @JsonSubTypes.Type(value = PutFieldEvent.class, name = "PUTFIELD"),
    @JsonSubTypes.Type(value = ReturnEvent.class, name = "RETURN"),
    @JsonSubTypes.Type(value = ThrowEvent.class, name = "THROW"),
    @JsonSubTypes.Type(value = ArithmeticEvent.class, name = "ARITHMETIC"),
    @JsonSubTypes.Type(value = ConditionEvent.class, name = "CONDITION")
})
public sealed interface TraceEvent permits ArithmeticEvent, ArrayStoreEvent, CallEvent, ConditionEvent, LocalEvent, NewEvent, PutFieldEvent, ReturnEvent, ThrowEvent
{

    /**
     * Returns the unique identifier of the event.
     * @return the event ID
     */
    String eventId();

    /**
     * Returns the span ID associated with the event.
     * @return the span ID
     */
    String spanId();

    /**
     * Returns the frame ID associated with the event.
     * @return the frame ID
     */
    String frameId();

    /**
     * Returns the location of the event in the source code.
     * @return the trace location
     */
    TraceLoc location();
    
}
