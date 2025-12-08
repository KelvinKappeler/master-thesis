package ch.epfl.printwizard.plugin.logging;

import ch.epfl.printwizard.plugin.model.trace.TraceLoc;
import ch.epfl.printwizard.plugin.model.trace.events.TraceEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the context of a field write operator.
 */
public class FieldWriteCtx implements ExecCtx {

    private final String fieldWriteEventId;
    private final String spanId;
    private final TraceLoc location;
    private final String objectId;
    private final String fieldName;
    private final String fieldType;

    private final List<String> childEventIds = new ArrayList<>();

    public FieldWriteCtx(
        String eventId, String spanId,
        TraceLoc location, String objectId,
        String fieldName, String fieldType
    ) {
        this.fieldWriteEventId = eventId;
        this.spanId = spanId;
        this.location = location;
        this.objectId = objectId;
        this.fieldName = fieldName;
        this.fieldType = fieldType;
    }

    public String getFieldWriteEventId() {
        return fieldWriteEventId;
    }

    public String getSpanId() {
        return spanId;
    }

    public TraceLoc getLocation() {
        return location;
    }

    public String getObjectId() {
        return objectId;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getFieldType() {
        return fieldType;
    }

    public List<String> getChildEventIds() {
        return childEventIds;
    }
    
    @Override
    public boolean handleEvent(TraceEvent event) {
        if (event != null) {
            childEventIds.add(event.eventId());
        }
        
        return true;
    }
}
