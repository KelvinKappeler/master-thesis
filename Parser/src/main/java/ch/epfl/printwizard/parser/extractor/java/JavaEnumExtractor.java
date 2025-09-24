package ch.epfl.printwizard.parser.extractor.java;

import ch.epfl.printwizard.shared.model.program.EnumInfo;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;

import java.util.stream.Stream;

/**
 * Represents an extractor that extracts enum information from a Java CompilationUnit.
 */
public final class JavaEnumExtractor extends AbstractJavaTypeExtractor<EnumInfo> {

    public JavaEnumExtractor(String sourceId) {
        super(sourceId);
    }

    @Override
    protected Stream<? extends TypeDeclaration<?>> findTargetDeclarations(CompilationUnit cu) {
        return cu.findAll(EnumDeclaration.class).stream();
    }

    @Override
    protected EnumInfo makeInfo(String id, String name, String packageName, String sourceId) {
        return new EnumInfo(id, name, packageName, sourceId);
    }
}
