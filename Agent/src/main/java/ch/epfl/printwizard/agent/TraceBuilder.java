package ch.epfl.printwizard.agent;

import ch.epfl.printwizard.shared.IdGenerator;
import ch.epfl.printwizard.shared.model.trace.TraceFile;
import ch.epfl.printwizard.shared.model.trace.TraceFrame;
import ch.epfl.printwizard.shared.model.trace.TraceSpan;
import ch.epfl.printwizard.shared.model.trace.events.TraceEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
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
     * Writes the currently collected trace data, including spans, frames, and events,
     * to the specified file in JSON format.
     * @param f the file where the JSON output will be written
     * @throws Exception if an I/O error occurs or if the object mapping fails
     */
    public void writeJsonTo(File f) throws Exception {
        List<TraceSpan>  s;
        List<TraceFrame> fr;
        List<TraceEvent> ev;
        synchronized (spans)  { s  = List.copyOf(spans); }
        synchronized (frames) { fr = List.copyOf(frames); }
        synchronized (events) { ev = List.copyOf(events); }

        ObjectMapper om = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        om.writeValue(f, new TraceFile(IdGenerator.traceId("1"), s, fr, ev));
    }
}
