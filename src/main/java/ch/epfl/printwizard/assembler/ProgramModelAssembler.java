package ch.epfl.printwizard.assembler;

import ch.epfl.printwizard.model.ClassInfo;
import ch.epfl.printwizard.model.MethodInfo;
import ch.epfl.printwizard.model.ProgramFile;
import ch.epfl.printwizard.model.Source;
import ch.epfl.printwizard.scanner.ProgramScanResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;

/**
 * Represents an assembler that constructs a ProgramFile from a ProgramScanResult.
 */
public final class ProgramModelAssembler {

    /**
     * Assembles a ProgramFile from the given ProgramScanResult.
     * @param result the ProgramScanResult to assemble from
     * @return the assembled ProgramFile
     */
    public static ProgramFile assemble(ProgramScanResult result) {
        var sources = dedupeById(result.getSources(), Source::sourceId);
        var classes = dedupeById(result.getClasses(), ClassInfo::classId);
        var methods = dedupeById(result.getMethods(), MethodInfo::methodId);

        sources.sort(Comparator.comparing(Source::sourceId));
        classes.sort(Comparator.comparing(ClassInfo::classId));
        methods.sort(Comparator.comparing(MethodInfo::methodId));

        return new ProgramFile(sources, classes, methods);
    }

    private static <T> ArrayList<T> dedupeById(List<T> list, Function<T, String>idExtractor) {
        var seen = new HashSet<String>();
        var out = new ArrayList<T>(list.size());
        for (T t : list) {
            var id = idExtractor.apply(t);
            if (seen.add(id)) out.add(t);
        }

        return out;
    }
}
