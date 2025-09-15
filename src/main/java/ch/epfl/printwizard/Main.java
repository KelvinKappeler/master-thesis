package ch.epfl.printwizard;

import ch.epfl.printwizard.assembler.ProgramModelAssembler;
import ch.epfl.printwizard.scanner.ProgramScanResult;
import ch.epfl.printwizard.scanner.java.JavaProgramScanner;
import com.github.javaparser.ParserConfiguration;

import java.io.IOException;
import java.nio.file.Path;

public final class Main {
    public static void main(String[] args) throws IOException {
        Path projectRoot = Path.of(".");

        var scanner = JavaProgramScanner.builder(projectRoot)
                .include("src/main/java/**/*.java")
                .exclude("**/build/**")
                .exclude("**/target/**")
                .languageLevel(ParserConfiguration.LanguageLevel.JAVA_21)
                .build();

        ProgramScanResult result = scanner.scan(projectRoot);

        result.getDiagnostics().forEach(d -> System.out.printf("[%s] %s (%s)%n", d.severity(), d.message(), d.filePath()));

        var model = ProgramModelAssembler.assemble(result);
        System.out.printf("Scanned %d sources, %d classes, %d interfaces, %d record, %d methods.%n",
                model.sources().size(),
                model.classes().size(),
                model.interfaces().size(),
                model.records().size(),
                model.methods().size());
    }
}
