package ch.epfl.printwizard.assembler;

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
        ArrayList<Source> sources = dedupeById(result.getSources(), Source::sourceId);
        ArrayList<ClassInfo> classes = dedupeById(result.getClasses(), ClassInfo::getId);
        ArrayList<InterfaceInfo> interfaces = dedupeById(result.getInterfaces(), InterfaceInfo::getId);
        ArrayList<RecordInfo> records = dedupeById(result.getRecords(), RecordInfo::getId);
        ArrayList<EnumInfo> enums = dedupeById(result.getEnums(), EnumInfo::getId);
        ArrayList<MethodInfo> methods = dedupeById(result.getMethods(), MethodInfo::methodId);

        sources.sort(Comparator.comparing(Source::sourceId));
        classes.sort(Comparator.comparing(ClassInfo::getId));
        interfaces.sort(Comparator.comparing(InterfaceInfo::getId));
        records.sort(Comparator.comparing(RecordInfo::getId));
        enums.sort(Comparator.comparing(EnumInfo::getId));
        methods.sort(Comparator.comparing(MethodInfo::methodId));

        return new ProgramFile(sources, classes, interfaces, records, enums, methods);
    }

    private static <T> ArrayList<T> dedupeById(List<T> list, Function<T, String>idExtractor) {
        HashSet<String> seen = new HashSet<>();
        ArrayList<T> out = new ArrayList<>(list.size());
        for (T t : list) {
            String id = idExtractor.apply(t);
            if (seen.add(id)) out.add(t);
        }

        return out;
    }
}
