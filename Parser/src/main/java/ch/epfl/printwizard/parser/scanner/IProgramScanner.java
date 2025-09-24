package ch.epfl.printwizard.parser.scanner;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Represents an interface for scanning a directory intended mostly to create program.json
 */
public interface IProgramScanner {
    /**
     * Scans the given root directory and returns a ProgramScanResult containing information about the sources and classes found.
     * @param root the root directory to scan
     * @return a ProgramScanResult containing information about the sources and classes found
     * @throws IOException if an I/O error occurs
     */
    ProgramScanResult scan(Path root) throws IOException;
}
