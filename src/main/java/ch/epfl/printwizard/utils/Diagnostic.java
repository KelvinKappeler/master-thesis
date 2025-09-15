package ch.epfl.printwizard.utils;

/**
 * Represents a diagnostic message produced during the scanning of source files.
 * @param severity The severity level of the diagnostic (INFO, WARNING, ERROR)
 * @param message The diagnostic message
 * @param filePath The path of the file where the diagnostic was produced
 */
public record Diagnostic(
    Severity severity,
    String message,
    String filePath
) {
    public enum Severity {
        INFO,
        WARNING,
        ERROR
    }
}
