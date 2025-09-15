package ch.epfl.printwizard.extractor.java;

import ch.epfl.printwizard.model.RecordInfo;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;

import java.util.stream.Stream;

/**
 * Represents an extractor that extracts record information from a Java CompilationUnit.
 */
public final class JavaRecordExtractor extends AbstractJavaTypeExtractor<RecordInfo> {

    private static final String RECORD_ID_PREFIX = "rec";
    
    public JavaRecordExtractor(String sourceId) { super(sourceId); }

    @Override
    protected Stream<? extends TypeDeclaration<?>> findTargetDeclarations(CompilationUnit cu) {
        return cu.findAll(RecordDeclaration.class).stream();
    }

    @Override
    protected String getIdPrefix() {
        return RECORD_ID_PREFIX;
    }

    @Override
    protected RecordInfo makeInfo(String id, String name, String packageName, String sourceId) {
        return new RecordInfo(id, name, packageName, sourceId);
    }
}
