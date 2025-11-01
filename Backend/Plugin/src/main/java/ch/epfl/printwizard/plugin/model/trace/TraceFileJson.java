package ch.epfl.printwizard.plugin.model.trace;

import ch.epfl.printwizard.plugin.utils.Preconditions;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;

/**
 * Represents a JSON serializer for {@link TraceFile}.
 */
public class TraceFileJson {
    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private TraceFileJson() {}

    /**
     * Writes the given trace file to the given path.
     * @param path the path to write the trace file to
     * @param traceFile the trace file to write
     */
    public static void write(String path, TraceFile traceFile) throws IOException {
        Preconditions.requireNonNull(path, "path is null");
        Preconditions.requireNonNull(traceFile, "traceFile is null");

        MAPPER.writeValue(new File(path), traceFile);
    }
}
