package ch.epfl.printwizard.parser;

import ch.epfl.printwizard.parser.assembler.ProgramModelAssembler;
import ch.epfl.printwizard.parser.scanner.ProgramScanResult;
import ch.epfl.printwizard.parser.scanner.java.JavaProgramScanner;
import ch.epfl.printwizard.shared.writer.ProgramFileWriter;
import com.github.javaparser.ParserConfiguration;

import java.io.IOException;
import java.nio.file.Path;

public final class Main {
    public static void main(String[] args) throws IOException {
        Path projectRoot = Path.of(".");

        var scanner = JavaProgramScanner.builder(projectRoot)
            .include("Examples/src/main/java/ch/epfl/printwizard/examples/cheese/Main.java")
            .languageLevel(ParserConfiguration.LanguageLevel.JAVA_21)
            .build();

        ProgramScanResult result = scanner.scan(projectRoot);

        result.getDiagnostics().forEach(d -> System.out.printf("[%s] %s (%s)%n", d.severity(), d.message(), d.filePath()));

        var model = ProgramModelAssembler.assemble(result);
        System.out.printf("Scanned %d sources, %d classes, %d interfaces, %d records, %d enums, %d methods.%n",
                model.sources().size(),
                model.classes().size(),
                model.interfaces().size(),
                model.records().size(),
                model.enums().size(),
                model.methods().size());

        ProgramFileWriter.write(model, Path.of("Results/New/program.json"));
    }
}
