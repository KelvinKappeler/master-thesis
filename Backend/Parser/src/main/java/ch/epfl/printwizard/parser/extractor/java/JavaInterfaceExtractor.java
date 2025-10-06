package ch.epfl.printwizard.parser.extractor.java;

import ch.epfl.printwizard.shared.model.program.InterfaceInfo;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;

import java.util.stream.Stream;

/**
 * Represents an extractor that extracts interface information from a Java CompilationUnit.
 */
public final class JavaInterfaceExtractor extends AbstractJavaTypeExtractor<InterfaceInfo> {
    
    public JavaInterfaceExtractor(String sourceId) {
        super(sourceId);
    }

    @Override
    protected Stream<? extends TypeDeclaration<?>> findTargetDeclarations(CompilationUnit cu) {
        return cu.findAll(ClassOrInterfaceDeclaration.class)
                .stream()
                .filter(ClassOrInterfaceDeclaration::isInterface);
    }

    @Override
    protected InterfaceInfo makeInfo(String id, String name, String packageName, String sourceId) {
        return new InterfaceInfo(id, name, packageName, sourceId);
    }
}
