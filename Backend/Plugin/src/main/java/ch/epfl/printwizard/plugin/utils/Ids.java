package ch.epfl.printwizard.plugin.utils;

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

    /**
     * Creates a unique identifier for a method.
     * @param owner the internal name of the class containing the method
     * @param name the name of the method
     * @param paramTypes the parameter types of the method
     * @param returnType the return resultType of the method
     * @return the unique identifier for the method
     */
    public static String createNewMethodId(String owner, String name, String[] paramTypes, String returnType) {
        return "m:" + owner + "." + name + "(" + String.join(",", paramTypes) + "):" + returnType;
    }

    /**
     * Creates a unique identifier for a source file.
     * @param sourceName the name of the source file
     * @return the unique identifier for the source file
     */
    public static String createSourceId(String sourceName) {
        return "src:" + sourceName;
    }
}
