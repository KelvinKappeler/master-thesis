package ch.epfl.printwizard.parser.extractor.java;

import ch.epfl.printwizard.parser.extractor.IExtractor;
import ch.epfl.printwizard.shared.IdGenerator;
import ch.epfl.printwizard.shared.model.program.Source;
import ch.epfl.printwizard.shared.utils.Preconditions;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public record JavaSourceExtractor(Path baseDir, JavaParser parser) implements IExtractor<Path, Source> {

    public JavaSourceExtractor {
        Preconditions.requireNonNull(baseDir, "Base directory cannot be null");
        Preconditions.requireNonNull(parser, "JavaParser cannot be null");
    }

    public List<Source> extract(Path filePath) throws IOException {
        Path rel = (baseDir != null && filePath.startsWith(baseDir))
                ? baseDir.relativize(filePath)
                : filePath;

        CompilationUnit cu = parser.parse(filePath).getResult()
                .orElseThrow(() -> new IOException("Could not parse: " + filePath));

        int lines = cu.getRange().map(r -> r.end.line).orElseThrow();

        String relPath = rel.toString().replace('\\', '/');

        String packagePath = relPath;
        if (relPath.contains("src/main/java/")) {
            packagePath = relPath.substring(relPath.indexOf("src/main/java/") + "src/main/java/".length());
        }

        String sourceId = IdGenerator.sourceId(packagePath);
        
        return List.of(new Source(sourceId, rel.toString().replace('\\', '/'), "java", lines));
    }
}
