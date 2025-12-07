package ch.epfl.printwizard.plugin.logging;

import ch.epfl.printwizard.plugin.model.trace.TraceLoc;
import ch.epfl.printwizard.plugin.model.trace.events.TraceEvent;
import ch.epfl.printwizard.plugin.utils.Preconditions;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the context of a local variable.
 */
public class LocalCtx implements ExecCtx {
    private final String localEventId;
    private final String spanId;
    private final String methodId;
    private final TraceLoc location;
    private final String varName;
    private final String label;

    private final List<String> childEventIds = new ArrayList<>();

    public LocalCtx(String localEventId, String spanId, String methodId, TraceLoc location, String varName, String label) {
        Preconditions.requireNonNull(localEventId, "localEventId is null");
        Preconditions.requireNonNull(spanId, "spanId is null");
        Preconditions.requireNonNull(methodId, "methodId is null");
        Preconditions.requireNonNull(location, "location is null");
        Preconditions.requireNonNull(varName, "varName is null");
        Preconditions.requireNonNull(label, "label is null");

        this.localEventId = localEventId;
        this.spanId = spanId;
        this.methodId = methodId;
        this.location = location;
        this.varName = varName;
        this.label = label;
    }

    public String getLocalEventId() {
        return localEventId;
    }

    public String getSpanId() {
        return spanId;
    }

    public String getMethodId() {
        return methodId;
    }

    public TraceLoc getLocation() {
        return location;
    }

    public String getVarName() {
        return varName;
    }

    public String getLabel() {
        return label;
    }

    public List<String> getChildEventIds() {
        return childEventIds;
    }

    @Override
    public boolean handleEvent(TraceEvent e) {
        childEventIds.add(e.eventId());

        return true;
    }
}
