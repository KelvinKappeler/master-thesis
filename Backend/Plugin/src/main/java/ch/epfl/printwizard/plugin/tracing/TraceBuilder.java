package ch.epfl.printwizard.plugin.tracing;

import ch.epfl.printwizard.plugin.model.trace.TraceFrame;
import ch.epfl.printwizard.plugin.model.trace.TraceSpan;
import ch.epfl.printwizard.plugin.model.trace.events.TraceEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a builder for creating and writing trace data to a JSON file.
 */
public final class TraceBuilder {
    private final List<TraceSpan> spans = new ArrayList<>();
    private final List<TraceFrame> frames = new ArrayList<>();
    private final List<TraceEvent> events = new ArrayList<>();

    /**
     * Adds a trace span to the list of spans in the builder.
     * @param s the trace span to be added
     */
    public void addSpan(TraceSpan s) {
        spans.add(s);
    }

    /**
     * Retrieves the list of trace spans maintained by the builder. The returned list
     * contains all the trace spans added to the builder through {@code addSpan}.
     * @return the list of trace spans
     */
    public List<TraceSpan> getSpans() {
        return spans;
    }

    /**
     * Adds a trace frame to the list of frames in the builder.
     * @param fr the trace frame to be added
     */
    public void addFrame(TraceFrame fr)
    {
        frames.add(fr);
    }

    /**
     * Adds a trace event to the list of events in the builder.
     * @param ev the trace event to be added
     */
    public void addEvent(TraceEvent ev)
    {
        events.add(ev);
    }

    /**
     * Retrieves the list of trace events maintained by the builder.
     * @return the list of trace events
     */
    public List<TraceEvent> getEvents() {
        return events;
    }
}
