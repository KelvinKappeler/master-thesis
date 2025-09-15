package ch.epfl.printwizard.scanner.java;

import ch.epfl.printwizard.extractor.java.*;
import ch.epfl.printwizard.model.*;
import ch.epfl.printwizard.scanner.IProgramScanner;
import ch.epfl.printwizard.scanner.ProgramScanResult;
import ch.epfl.printwizard.utils.Diagnostic;
import ch.epfl.printwizard.utils.Preconditions;
import ch.epfl.printwizard.utils.java.TypeIdUtils;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.TypeDeclaration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static java.nio.file.FileVisitOption.FOLLOW_LINKS;

/**
 * Represents a scanner for Java programs (intended mostly to create program.json)
 */
public record JavaProgramScanner(Path baseDir, JavaParser parser, Predicate<Path> pathFilter) implements IProgramScanner {

    /**
     * Creates a builder for JavaProgramScanner with the specified base directory.
     *
     * @param baseDir the base directory for the scanner
     * @return a Builder instance to configure and build the JavaProgramScanner
     */
    public static Builder builder(Path baseDir) {
        Preconditions.requireNonNull(baseDir, "Base directory cannot be null");

        return new Builder(baseDir);
    }

    public JavaProgramScanner(Path baseDir, JavaParser parser, Predicate<Path> pathFilter) {
        Preconditions.requireNonNull(baseDir, "Base directory cannot be null");
        Preconditions.requireNonNull(parser, "JavaParser cannot be null");

        this.baseDir = baseDir;
        this.parser = parser;
        this.pathFilter = pathFilter != null ? pathFilter : p -> true;
    }

    @Override
    public ProgramScanResult scan(Path root) throws IOException {
        Preconditions.requireNonNull(root, "Root path cannot be null");

        ProgramScanResult result = new ProgramScanResult();
        JavaSourceExtractor sourceExtractor = new JavaSourceExtractor(baseDir, parser);

        try (var stream = Files.walk(root, Integer.MAX_VALUE, FOLLOW_LINKS)) {
            stream.filter(p -> p.toString().endsWith(".java"))
                .filter(pathFilter).forEach(file -> {
                    try {
                        // (1) Source
                        List<Source> sources = sourceExtractor.extract(file);
                        if (sources.isEmpty()) return;
                        Source source = sources.getFirst();
                        result.addSources(sources);

                        // (2) Parse
                        CompilationUnit cu = parser.parse(file).getResult().orElseThrow(() -> new IOException("Parse failed: " + file));

                        // (3) Classes / Interface / Records for this file
                        JavaClassExtractor classExtractor = new JavaClassExtractor(source.sourceId());
                        List<ClassInfo> classes = classExtractor.extract(cu);
                        result.addClasses(classes);

                        JavaInterfaceExtractor interfaceExtractor = new JavaInterfaceExtractor(source.sourceId());
                        List<InterfaceInfo> interfaces = interfaceExtractor.extract(cu);
                        result.addInterfaces(interfaces);

                        JavaRecordExtractor recordExtractor = new JavaRecordExtractor(source.sourceId());
                        List<RecordInfo> records = recordExtractor.extract(cu);
                        result.addRecords(records);

                        // (4) Methods
                        Map<String, TypeDeclaration<?>> typesById = new HashMap<>();
                        for (TypeDeclaration<?> td : cu.findAll(TypeDeclaration.class)) {
                            String id = TypeIdUtils.idFor(cu, td);
                            typesById.put(id, td);
                        }
                        
                        List<BaseTypeInfo> allTypes = new ArrayList<>();
                        allTypes.addAll(classes);
                        allTypes.addAll(interfaces);
                        allTypes.addAll(records);

                        for (BaseTypeInfo bti : allTypes) {
                            TypeDeclaration<?> td = typesById.get(bti.getId());
                            if (td == null) {
                                continue;
                            }
                            
                            JavaMethodExtractor methodExtractor = new JavaMethodExtractor(source, bti);

                            List<MethodInfo> methods = methodExtractor.extract(td);
                            result.addMethods(methods);
                        }

                    } catch (Exception e) {
                        result.addDiagnostic(new Diagnostic(
                                Diagnostic.Severity.ERROR,
                                e.getMessage(),
                                relativizeSafe(file)
                        ));
                    }
                });
        }

        return result;
    }

    private String relativizeSafe(Path file) {
        Preconditions.requireNonNull(file, "File cannot be null");

        try {
            return baseDir.relativize(file).toString().replace('\\', '/');
        } catch (Exception ex) {
            return file.toString().replace('\\', '/');
        }
    }

    public static final class Builder {

        private final Path baseDir;
        private final List<String> includeList = new ArrayList<>();
        private final List<String> excludeList = new ArrayList<>();
        private ParserConfiguration.LanguageLevel languageLevel = ParserConfiguration.LanguageLevel.JAVA_21;

        private Builder(Path baseDir) {
            Preconditions.requireNonNull(baseDir, "Base directory cannot be null");

            this.baseDir = baseDir;
        }

        /**
         * Adds a path to the include list, e.g. "src/main/java/**{@literal /}*.java"
         *
         * @param path the path pattern to include
         * @return the Builder instance for chaining
         */
        public Builder include(String path) {
            includeList.add(path);
            return this;
        }

        /**
         * Adds a path to the exclude list, e.g. "src/test/java/**{@literal /}*.java"
         *
         * @param path the path pattern to exclude
         * @return the Builder instance for chaining
         */
        public Builder exclude(String path) {
            excludeList.add(path);
            return this;
        }

        /**
         * Sets the language level for the Java parser.
         *
         * @param level the language level to set
         * @return the Builder instance for chaining
         */
        public Builder languageLevel(ParserConfiguration.LanguageLevel level) {
            this.languageLevel = level;
            return this;
        }

        public JavaProgramScanner build() {
            ParserConfiguration config = new ParserConfiguration()
                    .setCharacterEncoding(StandardCharsets.UTF_8)
                    .setLanguageLevel(languageLevel)
                    .setAttributeComments(true);
            JavaParser parser = new JavaParser(config);

            Predicate<Path> filter = buildPathFilter();
            return new JavaProgramScanner(baseDir, parser, filter);
        }

        private Predicate<Path> buildPathFilter() {
            var includes = compileMatchers(includeList);
            var excludes = compileMatchers(excludeList);

            return path -> {
                Path rel = relativize(path);
                boolean included = includes.isEmpty() || matchesAny(rel, includes);
                boolean excluded = matchesAny(rel, excludes);
                return included && !excluded;
            };
        }

        private List<PathMatcher> compileMatchers(List<String> globs) {
            FileSystem fs = baseDir.getFileSystem();
            List<PathMatcher> out = new ArrayList<>();
            for (String g : globs) {
                out.add(fs.getPathMatcher("glob:" + g));
            }
            return out;
        }

        private boolean matchesAny(Path rel, List<PathMatcher> matchers) {
            for (PathMatcher m : matchers) if (m.matches(rel)) return true;
            return false;
        }

        private Path relativize(Path p) {
            try {
                return baseDir.relativize(p);
            } catch (Exception e) {
                return p;
            }
        }
    }
}
