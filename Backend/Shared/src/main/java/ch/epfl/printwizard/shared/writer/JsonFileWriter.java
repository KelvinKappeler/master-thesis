package ch.epfl.printwizard.shared.writer;

import ch.epfl.printwizard.shared.utils.Preconditions;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Represents a generic JSON file writer.
 * This class is responsible for serializing objects into JSON format and writing them to a specified file
 */
public final class JsonFileWriter {

    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private JsonFileWriter() {}

    /**
     * Writes the given object to the specified output path in JSON format.
     * @param obj the object to write
     * @param outPath the output path where the JSON file will be written
     * @param <T> the type of the object to write
     * @throws IOException if an I/O error occurs
     */
    public static <T> void write(T obj, Path outPath) throws IOException {
        Preconditions.requireNonNull(obj, "obj is null");
        Preconditions.requireNonNull(outPath, "outPath is null");

        if (outPath.getParent() != null) Files.createDirectories(outPath.getParent());
        MAPPER.writeValue(outPath.toFile(), obj);
    }

}
