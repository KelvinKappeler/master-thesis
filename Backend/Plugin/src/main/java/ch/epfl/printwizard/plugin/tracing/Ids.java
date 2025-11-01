package ch.epfl.printwizard.plugin.tracing;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents unique identifiers for spans, frames, and events.
 */
public class Ids {
    private static final AtomicLong SPAN = new AtomicLong(0L);
    private static final AtomicLong FRAME = new AtomicLong(0L);
    private static final AtomicLong EVE = new AtomicLong(0L);

    private Ids() {}

    /**
     * Generates a unique identifier for a span.
     * @return the unique identifier for the span
     */
    public static String nextSpanId() {
        return "spn:" + SPAN.incrementAndGet();
    }

    /**
     * Generates a unique identifier for a frame.
     * @return the unique identifier for the frame
     */
    public static String nextFrameId() {
        return "frm:" + FRAME.incrementAndGet();
    }

    /**
     * Generates a unique identifier for an event.
     * @return the unique identifier for the event
     */
    public static String nextEventId() {
        return "eve:" + EVE.incrementAndGet();
    }
}
