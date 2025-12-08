package ch.epfl.printwizard.plugin.logging;

import ch.epfl.printwizard.plugin.model.trace.TraceLoc;
import ch.epfl.printwizard.plugin.model.trace.events.TraceEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the context of an arithmetic operator.
 */
public class ArithmeticCtx implements ExecCtx {

    private final String arithmeticEventId;
    private final String spanId;
    private final TraceLoc location;
    private final String operation;

    private final List<String> leftEventIds = new ArrayList<>();
    private final List<String> rightEventIds = new ArrayList<>();

    public ArithmeticCtx(String arithmeticEventId, String spanId, TraceLoc location, String operation) {
        this.arithmeticEventId = arithmeticEventId;
        this.spanId = spanId;
        this.location = location;
        this.operation = operation;
    }

    public String getArithmeticEventId() {
        return arithmeticEventId;
    }

    public String getSpanId() {
        return spanId;
    }

    public TraceLoc getLocation() {
        return location;
    }

    public String getOperation() {
        return operation;
    }

    public List<String> getLeftEventIds() {
        return leftEventIds;
    }

    public List<String> getRightEventIds() {
        return rightEventIds;
    }

    @Override
    public boolean handleEvent(TraceEvent e) {
        return false;
    }
}
