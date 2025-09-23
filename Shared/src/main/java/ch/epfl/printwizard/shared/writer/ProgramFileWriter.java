package ch.epfl.printwizard.shared.writer;

import ch.epfl.printwizard.shared.model.program.ProgramFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Represents a writer for the program.json file.
 * This class is responsible for serializing the ProgramFile structure into JSON format.
 */
public final class ProgramFileWriter {

    private static final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    
    private ProgramFileWriter() {}

    /**
     * Writes the given ProgramFile to the specified output path in JSON format.
     * @param program the ProgramFile to write
     * @param outPath the output path where the JSON file will be written
     * @throws IOException if an I/O error occurs
     */
    public static void write(ProgramFile program, Path outPath) throws IOException {
        if (outPath.getParent() != null) Files.createDirectories(outPath.getParent());

        objectMapper.writeValue(outPath.toFile(), program);
    }
}
