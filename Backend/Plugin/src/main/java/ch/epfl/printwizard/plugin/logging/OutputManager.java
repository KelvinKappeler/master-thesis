package ch.epfl.printwizard.plugin.logging;

import ch.epfl.printwizard.plugin.model.index.IndexFile;
import ch.epfl.printwizard.plugin.model.manifest.FileLocations;
import ch.epfl.printwizard.plugin.model.manifest.ManifestFile;
import ch.epfl.printwizard.plugin.model.state.StateFile;
import ch.epfl.printwizard.plugin.model.trace.TraceFile;
import ch.epfl.printwizard.plugin.utils.JsonManager;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Manages the output of the plugin.
 */
public class OutputManager {

    private static final TraceFile.Builder TRACE_FILE_BUILDER = new TraceFile.Builder();
    private static final IndexFile.Builder INDEX_FILE_BUILDER = new IndexFile.Builder();
    private static final StateFile.Builder STATE_FILE_BUILDER = new StateFile.Builder();

    private OutputManager() {}

    /**
     * Gets the builder for the trace file.
     * @return the builder for the trace file
     */
    public static TraceFile.Builder getTraceFileBuilder() {
        return TRACE_FILE_BUILDER;
    }

    /**
     * Gets the builder for the index file.
     * @return the builder for the index file
     */
    public static IndexFile.Builder getIndexFileBuilder() {
        return INDEX_FILE_BUILDER;
    }

    /**
     * Gets the builder for the state file.
     * @return the builder for the state file
     */
    public static StateFile.Builder getStateFileBuilder() {
        return STATE_FILE_BUILDER;
    }

    private static void flush() throws IOException {
        ManifestFile manifestFile = new ManifestFile(
            "1.0.0", LocalDateTime.now().toString(),
            new FileLocations(
                "program.json",
                "trace.json",
                "index.json",
                "state.json"
            )
        );

        TraceFile traceFile = TRACE_FILE_BUILDER.build();
        IndexFile indexFile = INDEX_FILE_BUILDER.build();
        StateFile stateFile = STATE_FILE_BUILDER.build();

        JsonManager.write("Results/New/trace.json", traceFile);
        JsonManager.write("Results/New/index.json", indexFile);
        JsonManager.write("Results/New/manifest.json", manifestFile);
        JsonManager.write("Results/New/state.json", stateFile);
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
