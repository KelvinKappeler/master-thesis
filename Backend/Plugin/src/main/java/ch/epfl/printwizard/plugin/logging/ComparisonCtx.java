package ch.epfl.printwizard.plugin.logging;

import ch.epfl.printwizard.plugin.model.trace.TraceLoc;
import ch.epfl.printwizard.plugin.model.trace.events.TraceEvent;
import ch.epfl.printwizard.plugin.utils.Preconditions;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the context of a comparison operation.
 */
public class ComparisonCtx implements ExecCtx {

    private final String comparisonEventId;
    private final String spanId;
    private final String frameId;
    private final String methodId;
    private final TraceLoc location;
    private final String operator;

    private final List<String> leftEventIds = new ArrayList<>();
    private final List<String> rightEventIds = new ArrayList<>();

    public ComparisonCtx(String comparisonEventId, String spanId, String frameId, String methodId, TraceLoc location, String operator) {
        Preconditions.requireNonNull(comparisonEventId, "comparisonEventId is null");
        Preconditions.requireNonNull(spanId, "spanId is null");
        Preconditions.requireNonNull(frameId, "frameId is null");
        Preconditions.requireNonNull(methodId, "methodId is null");
        Preconditions.requireNonNull(location, "location is null");
        Preconditions.requireNonNull(operator, "operator is null");

        this.comparisonEventId = comparisonEventId;
        this.spanId = spanId;
        this.frameId = frameId;
        this.methodId = methodId;
        this.location = location;
        this.operator = operator;
    }

    public String getComparisonEventId() {
        return comparisonEventId;
    }

    public String getSpanId() {
        return spanId;
    }

    public String getFrameId() {
        return frameId;
    }

    public String getMethodId() {
        return methodId;
    }

    public TraceLoc getLocation() {
        return location;
    }

    public String getOperator() {
        return operator;
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
