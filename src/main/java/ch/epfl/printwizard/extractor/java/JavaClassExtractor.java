package ch.epfl.printwizard.extractor.java;

import ch.epfl.printwizard.model.ClassInfo;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;

import java.util.stream.Stream;

/**
 * Represents an extractor that extracts class information from a Java CompilationUnit.
 */
public final class JavaClassExtractor extends AbstractJavaTypeExtractor<ClassInfo> {

    private static final String CLASS_ID_PREFIX = "cls";
    
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
    protected String getIdPrefix() {
        return CLASS_ID_PREFIX;
    }

    @Override
    protected ClassInfo makeInfo(String id, String name, String packageName, String sourceId) {
        return new ClassInfo(id, name, packageName, sourceId);
    }
}
