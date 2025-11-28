package ch.epfl.printwizard.plugin.logging;

import ch.epfl.printwizard.plugin.model.trace.TraceLoc;
import ch.epfl.printwizard.plugin.model.trace.events.CallEvent;
import ch.epfl.printwizard.plugin.model.trace.events.TraceEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents the context of an array store operation in the execution trace.
 */
public class ArrayStoreCtx implements ExecCtx {

    private final String arrayEventId;
    private final String spanId;
    private final String frameId;
    private final String methodId;
    private final TraceLoc location;
    private final String arrayVarName;
    private final String arrayObjectId;
    private final int index;
    private final String label;

    private final List<String> childEventIds = new ArrayList<>();

    public ArrayStoreCtx(String arrayEventId, String spanId, String frameId, String methodId, TraceLoc location, String arrayVarName, String arrayObjectId, int index, String label) {
        this.arrayEventId = arrayEventId;
        this.spanId = spanId;
        this.frameId = frameId;
        this.methodId = methodId;
        this.location = location;
        this.arrayVarName = arrayVarName;
        this.arrayObjectId = arrayObjectId;
        this.index = index;
        this.label = label;
    }

    @Override
    public boolean handleEvent(TraceEvent e) {
        if (e instanceof CallEvent ce && Objects.equals(ce.callerMethodId(), methodId)) {
            childEventIds.add(ce.eventId());
            return false;
        }

        if (Objects.equals(e.spanId(), spanId) && Objects.equals(e.frameId(), frameId)) {
            childEventIds.add(e.eventId());
        }

        return false;
    }

    public List<String> getChildEventIds() {
        return childEventIds;
    }

    public String getArrayEventId() {
        return arrayEventId;
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

    public String getArrayVarName() {
        return arrayVarName;
    }

    public String getArrayObjectId() {
        return arrayObjectId;
    }

    public int getIndex() {
        return index;
    }

    public String getLabel() {
        return label;
    }
}
