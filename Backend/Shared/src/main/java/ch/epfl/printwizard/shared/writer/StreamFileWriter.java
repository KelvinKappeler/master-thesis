package ch.epfl.printwizard.shared.writer;

import ch.epfl.printwizard.shared.model.stream.StreamFile;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Represents a writer for the stream.json file.
 * This class is responsible for serializing the StreamFile structure into JSON format.
 */
public final class StreamFileWriter {

    private StreamFileWriter() {}

    /**
     * Writes the given StreamFile to the specified output path in JSON format.
     * @param stream the StreamFile to write
     * @param outPath the output path where the JSON file will be written
     * @throws IOException if an I/O error occurs
     */
    public static void write(StreamFile stream, Path outPath) throws IOException {
        JsonFileWriter.write(stream, outPath);
    }
}
