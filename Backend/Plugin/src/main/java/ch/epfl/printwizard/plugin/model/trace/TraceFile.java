package ch.epfl.printwizard.plugin.model.trace;

import ch.epfl.printwizard.plugin.model.trace.events.TraceEvent;
import ch.epfl.printwizard.plugin.utils.Preconditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a trace file containing a list of spans, frames, and events.
 * @param spans the list of spans in the trace
 * @param frames the list of frames in the trace
 * @param events the list of events in the trace
 */
public record TraceFile(
    List<TraceSpan> spans,
    List<TraceFrame> frames,
    List<TraceEvent> events
) {
    public TraceFile {
        Preconditions.requireNonNull(spans, "spans is null");
        Preconditions.requireNonNull(frames, "frames is null");
        Preconditions.requireNonNull(events, "events is null");
    }

    /**
     * Represents a builder for {@link TraceFile}.
     */
    public static final class Builder {
        private final List<TraceSpan> spans = Collections.synchronizedList(new ArrayList<>());
        private final List<TraceFrame> frames = Collections.synchronizedList(new ArrayList<>());
        private final List<TraceEvent> events = Collections.synchronizedList(new ArrayList<>());

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

        /**
         * Builds a {@link TraceFile} instance from the data collected in the builder.
         * @return the built {@link TraceFile} instance
         */
        public TraceFile build() {
            List<TraceSpan> spansSnapshot;
            List<TraceFrame> framesSnapshot;
            List<TraceEvent> eventsSnapshot;

            synchronized (spans) {
                spansSnapshot = List.copyOf(spans);
            }
            synchronized (frames) {
                framesSnapshot = List.copyOf(frames);
            }
            synchronized (events) {
                eventsSnapshot = List.copyOf(events);
            }

            return new TraceFile(spansSnapshot, framesSnapshot, eventsSnapshot);
        }
    }
}
