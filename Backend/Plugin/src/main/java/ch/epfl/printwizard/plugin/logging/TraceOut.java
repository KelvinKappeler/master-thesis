package ch.epfl.printwizard.plugin.logging;

import ch.epfl.printwizard.plugin.model.trace.TraceLoc;
import ch.epfl.printwizard.plugin.model.trace.events.LocalEvent;

import java.util.Set;

/**
 * Represents a list of static methods used by the program to log data during its execution.
 */
public class TraceOut {

    private static final Set<StackWalker.Option> WALKER_OPTS = Set.of(
            StackWalker.Option.RETAIN_CLASS_REFERENCE,
            StackWalker.Option.SHOW_HIDDEN_FRAMES
    );
    private static final StackWalker WALKER = StackWalker.getInstance(WALKER_OPTS);

    private TraceOut() {}

    public static <T> T recordLocalEvent(String label, T value) {
        System.out.println("[recordLocalEvent] T " + label + " = " + value);
        OutputManager.getTraceFileBuilder().addEvent(makeLocalEvent(label, value));
        return value;
    }

    private static LocalEvent makeLocalEvent(String label, Object value) {
        return new LocalEvent(Ids.nextEventId(), "spanId", "frameId", currentLoc(), "owner", "method", label, -1, value);
    }

    private static TraceLoc currentLoc() {
        StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
        return walker.walk(stream -> stream
            .skip(1)
            .findFirst()
            .map(f -> new TraceLoc(f.getFileName(), f.getLineNumber()))
            .orElseGet(() -> new TraceLoc("unknown", -1))
        );
    }
}
