package ch.epfl.printwizard.agent;

import ch.epfl.printwizard.agent.utils.Preconditions;

import java.util.*;

/**
 * Represents the configuration for tracing, including which classes to include/exclude,
 */
public record TraceConfig(
    List<String> includePrefixes,
    List<String> excludePrefixes,
    boolean isTraceNew,
    boolean isTraceFields,
    boolean isTraceArrays,
    boolean isTraceLocals,
    String outPath
) {

    public TraceConfig {
        Preconditions.requireNonNull(includePrefixes, "includePrefixes cannot be null");
        Preconditions.requireNonNull(excludePrefixes, "excludePrefixes cannot be null");
        Preconditions.requireNonNull(outPath, "outPath cannot be null");

    }

    /**
     * Gets the list of class name prefixes to include in tracing.
     *
     * @return the list of include prefixes
     */
    @Override
    public List<String> includePrefixes() {
        return includePrefixes;
    }

    /**
     * Gets the list of class name prefixes to exclude from tracing.
     *
     * @return the list of exclude prefixes
     */
    @Override
    public List<String> excludePrefixes() {
        return excludePrefixes;
    }

    /**
     * Gets whether to trace object allocations (new).
     *
     * @return true if object allocations should be traced, false otherwise
     */
    @Override
    public boolean isTraceNew() {
        return isTraceNew;
    }

    /**
     * Gets whether to trace field accesses.
     *
     * @return true if field accesses should be traced, false otherwise
     */
    @Override
    public boolean isTraceFields() {
        return isTraceFields;
    }

    /**
     * Gets whether to trace array accesses.
     *
     * @return true if array accesses should be traced, false otherwise
     */
    @Override
    public boolean isTraceArrays() {
        return isTraceArrays;
    }

    /**
     * Gets whether to trace local variable accesses.
     *
     * @return true if local variable accesses should be traced, false otherwise
     */
    @Override
    public boolean isTraceLocals() {
        return isTraceLocals;
    }

    /**
     * Gets the output path for the trace logs.
     *
     * @return the output path
     */
    @Override
    public String outPath() {
        return outPath;
    }

    /**
     * Determines if a class with the given internal name should be instrumented based on the include/exclude prefixes.
     *
     * @param internalClassName the internal name of the class (e.g., "ch/epfl/printwizard/MyClass")
     * @return true if the class should be instrumented, false otherwise
     */
    public boolean shouldInstrument(String internalClassName) {
        Preconditions.requireNonNull(internalClassName, "internalClassName cannot be null");

        for (String e : excludePrefixes) if (internalClassName.startsWith(e)) return false;
        for (String i : includePrefixes) if (internalClassName.startsWith(i)) return true;

        return false;
    }
}
