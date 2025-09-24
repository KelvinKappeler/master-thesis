package ch.epfl.printwizard.parser.extractor.java;

import ch.epfl.printwizard.shared.model.program.ClassInfo;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;

import java.util.stream.Stream;

/**
 * Represents an extractor that extracts class information from a Java CompilationUnit.
 */
public final class JavaClassExtractor extends AbstractJavaTypeExtractor<ClassInfo> {
    
    public JavaClassExtractor(String sourceId) {
        super(sourceId);
    }

    @Override
    protected Stream<? extends TypeDeclaration<?>> findTargetDeclarations(CompilationUnit cu) {
        return cu.findAll(ClassOrInterfaceDeclaration.class)
                .stream()
                .filter(d -> !d.isInterface());
    }

    @Override
    protected ClassInfo makeInfo(String id, String name, String packageName, String sourceId) {
        return new ClassInfo(id, name, packageName, sourceId);
    }
}
