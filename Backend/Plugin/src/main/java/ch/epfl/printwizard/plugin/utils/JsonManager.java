package ch.epfl.printwizard.plugin.utils;

import ch.epfl.printwizard.plugin.model.trace.TraceFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;

/**
 * Represents a utility class for managing JSON files.
 */
public class JsonManager {
    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private JsonManager() {}

    /**
     * Writes the given object to the specified path in JSON format.
     * @param path the path to write the JSON file
     * @param obj the object to be written
     * @param <T> the type of the object
     * @throws IOException if an I/O error occurs
     */
    public static <T> void write(String path, T obj) throws IOException {
        Preconditions.requireNonNull(path, "path is null");
        Preconditions.requireNonNull(obj, "traceFile is null");

        MAPPER.writeValue(new File(path), obj);
    }
}
