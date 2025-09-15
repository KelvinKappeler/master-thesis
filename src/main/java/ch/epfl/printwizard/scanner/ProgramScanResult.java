package ch.epfl.printwizard.scanner;

import ch.epfl.printwizard.model.ClassInfo;
import ch.epfl.printwizard.model.MethodInfo;
import ch.epfl.printwizard.model.Source;
import ch.epfl.printwizard.utils.Diagnostic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the result of scanning a program (for program.json), including its sources, classes, methods, and diagnostics.
 */
public final class ProgramScanResult {
    private final List<Source> sources = new ArrayList<>();
    private final List<ClassInfo> classes = new ArrayList<>();
    private final List<MethodInfo> methods = new ArrayList<>();
    private final List<Diagnostic> diagnostics = new ArrayList<>();

    /**
     * Returns an unmodifiable list of sources in the scanned program.
     * @return List of sources
     */
    public List<Source> getSources() {
        return Collections.unmodifiableList(sources);
    }

    /**
     * Returns an unmodifiable list of classes in the scanned program.
     * @return List of classes
     */
    public List<ClassInfo> getClasses() {
        return Collections.unmodifiableList(classes);
    }

    /**
     * Returns an unmodifiable list of methods in the scanned program.
     * @return List of methods
     */
    public List<MethodInfo> getMethods() {
        return Collections.unmodifiableList(methods);
    }

    /**
     * Returns an unmodifiable list of diagnostics generated during the scanning process.
     * @return List of diagnostics
     */
    public List<Diagnostic> getDiagnostics() {
        return Collections.unmodifiableList(diagnostics);
    }

    /**
     * Adds a list of sources to the scanned program.
     * @param s List of sources to add
     */
    public void addSources(List<Source> s) {
        sources.addAll(s);
    }

    /**
     * Adds a list of classes to the scanned program.
     * @param classInfos List of classes to add
     */
    public void addClasses(List<ClassInfo> classInfos) {
        classes.addAll(classInfos);
    }

    /**
     * Adds a list of methods to the scanned program.
     * @param methodInfos List of methods to add
     */
    public void addMethods(List<MethodInfo> methodInfos) {
        methods.addAll(methodInfos);
    }

    /**
     * Adds a diagnostic to the scanned program.
     * @param diagnostic Diagnostic to add
     */
    public void addDiagnostic(Diagnostic diagnostic) {
        diagnostics.add(diagnostic);
    }
}
