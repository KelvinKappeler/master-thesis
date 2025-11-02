package ch.epfl.printwizard.plugin.logging;

import ch.epfl.printwizard.plugin.model.trace.TraceFile;
import ch.epfl.printwizard.plugin.model.trace.TraceFileJson;

import java.io.IOException;

/**
 * Manages the output of the plugin.
 */
public class OutputManager {

    private static final TraceFile.Builder TRACE_FILE_BUILDER = new TraceFile.Builder();

    private OutputManager() {}

    /**
     * Gets the builder for the trace file.
     * @return the builder for the trace file
     */
    public static TraceFile.Builder getTraceFileBuilder() {
        return TRACE_FILE_BUILDER;
    }

    private static void flush() throws IOException {
        TraceFile traceFile = TRACE_FILE_BUILDER.build();
        TraceFileJson.write("Results/New/trace.json", traceFile);
    }

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                flush();
            } catch (IOException e) {
                System.err.println("Failed to write JSON files: " + e.getMessage());
            }
        }, "printwizard-json-shutdown"));
    }

}
