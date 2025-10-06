package ch.epfl.printwizard.parser.scanner;

import ch.epfl.printwizard.shared.model.program.*;
import ch.epfl.printwizard.shared.utils.Diagnostic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the result of scanning a program (for program.json), including its sources, classes, methods, and diagnostics.
 */
public final class ProgramScanResult {
    private final List<Source> sources = new ArrayList<>();
    private final List<ClassInfo> classes = new ArrayList<>();
    private final List<InterfaceInfo> interfaces = new ArrayList<>();
    private final List<RecordInfo> records = new ArrayList<>();
    private final List<EnumInfo> enums = new ArrayList<>();
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
     * Returns an unmodifiable list of interfaces in the scanned program.
     * @return List of interfaces
     */
    public List<InterfaceInfo> getInterfaces() {
        return Collections.unmodifiableList(interfaces);
    }

    /**
     * Returns an unmodifiable list of records in the scanned program.
     * @return List of records
     */
    public List<RecordInfo> getRecords() {
        return Collections.unmodifiableList(records);
    }
    
    /**
     * Returns an unmodifiable list of enums in the scanned program.
     * @return List of enums
     */
    public List<EnumInfo> getEnums() {
        return Collections.unmodifiableList(enums);
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
     * Adds a list of interfaces to the scanned program.
     * @param interfaceInfos List of interfaces to add
     */
    public void addInterfaces(List<InterfaceInfo> interfaceInfos) {
        interfaces.addAll(interfaceInfos);
    }

    /**
     * Adds a list of records to the scanned program.
     * @param recordInfos List of records to add
     */
    public void addRecords(List<RecordInfo> recordInfos) {
        records.addAll(recordInfos);
    }
    
    /**
     * Adds a list of enums to the scanned program.
     * @param enumInfos List of enums to add
     */
    public void addEnums(List<EnumInfo> enumInfos) {
        enums.addAll(enumInfos);
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
