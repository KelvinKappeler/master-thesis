package ch.epfl.printwizard.parser.extractor;

import java.io.IOException;
import java.util.List;

/**
 * Represents a generic extractor that takes an input of resultType I and produces a list of outputs of resultType O.
 * @param <I> Input resultType (e.g., a file path, a string, etc.)
 * @param <O> Output resultType (e.g., a data object, a record, etc.)
 */
public interface IExtractor<I, O> {
    /**
     * Extracts a list of outputs from the given input.
     * @param input The input from which to extract data.
     * @return A list of extracted outputs.
     * @throws IOException If an I/O error occurs during extraction.
     */
    List<O> extract(I input) throws IOException;
}
