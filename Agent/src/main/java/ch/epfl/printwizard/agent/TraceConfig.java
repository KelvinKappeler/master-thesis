package ch.epfl.printwizard.agent;

import ch.epfl.printwizard.agent.utils.Preconditions;

import java.util.*;

/**
 * Represents the configuration for tracing, including which classes to include/exclude,
 */
public final class TraceConfig {
    
    private final List<String> includePrefixes;
    private final List<String> excludePrefixes;
    private final boolean isTraceNew;
    private final boolean isTraceFields;
    private final boolean isTraceArrays;
    private final String outPath;

    public TraceConfig(
            List<String> includePrefixes,
            List<String> excludePrefixes,
            boolean isTraceNew,
            boolean isTraceFields,
            boolean isTraceArrays,
            String outPath
    ) {
        Preconditions.requireNonNull(includePrefixes, "includePrefixes cannot be null");
        Preconditions.requireNonNull(excludePrefixes, "excludePrefixes cannot be null");
        Preconditions.requireNonNull(outPath, "outPath cannot be null");
        
        this.includePrefixes = includePrefixes;
        this.excludePrefixes = excludePrefixes;
        this.isTraceNew = isTraceNew;
        this.isTraceFields = isTraceFields;
        this.isTraceArrays = isTraceArrays;
        this.outPath = outPath;
    }

    /**
     * Gets the list of class name prefixes to include in tracing.
     * @return the list of include prefixes
     */
    public List<String> getIncludePrefixes() {
        return includePrefixes;
    }
    
    /**
     * Gets the list of class name prefixes to exclude from tracing.
     * @return the list of exclude prefixes
     */
    public List<String> getExcludePrefixes() {
        return excludePrefixes;
    }

    /**
     * Gets whether to trace object allocations (new).
     * @return true if object allocations should be traced, false otherwise
     */
    public boolean isTraceNew() {
        return isTraceNew;
    }

    /**
     * Gets whether to trace field accesses.
     * @return true if field accesses should be traced, false otherwise
     */
    public boolean isTraceFields() {
        return isTraceFields;
    }

    /**
     * Gets whether to trace array accesses.
     * @return true if array accesses should be traced, false otherwise
     */
    public boolean isTraceArrays() {
        return isTraceArrays;
    }

    /**
     * Gets the output path for the trace logs.
     * @return the output path
     */
    public String getOutPath() {
        return outPath;
    }
    
    /**
     * Determines if a class with the given internal name should be instrumented based on the include/exclude prefixes.
     * @param internalClassName the internal name of the class (e.g., "ch/epfl/printwizard/MyClass")
     * @return true if the class should be instrumented, false otherwise
     */
    public boolean shouldInstrument(String internalClassName) {
        Preconditions.requireNonNull(internalClassName, "internalClassName cannot be null");
        
        for (String e : excludePrefixes) if (internalClassName.startsWith(e)) return false;
        for (String i : includePrefixes) if (internalClassName.startsWith(i)) return true;
        
        return false;
    }

    @Override
    public String toString() {
        return "TraceConfig { \n    - include=" + includePrefixes + ", \n    - exclude=" + excludePrefixes +
            ", \n    - new=" + isTraceNew + ", \n    - fields=" + isTraceFields + ", \n    - arrays=" + isTraceArrays +
            ", \n    - out=" + outPath + " }";
    }
}
