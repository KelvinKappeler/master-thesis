package ch.epfl.printwizard.extractor.java;

import ch.epfl.printwizard.extractor.IExtractor;
import ch.epfl.printwizard.model.BaseTypeInfo;
import ch.epfl.printwizard.utils.Preconditions;
import ch.epfl.printwizard.utils.java.TypeIdUtils;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.TypeDeclaration;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Represents an abstract extractor that extracts type information from a Java CompilationUnit.
 * Subclasses should specify which type declarations to extract (e.g., classes, interfaces, records).ß
 */
abstract class AbstractJavaTypeExtractor<T extends BaseTypeInfo> implements IExtractor<CompilationUnit, T> {
    protected final String sourceId;

    protected AbstractJavaTypeExtractor(String sourceId) {
        Preconditions.requireNonNull(sourceId, "Source ID cannot be null");
        Preconditions.require(!sourceId.isEmpty(), "Source ID cannot be empty");
        
        this.sourceId = sourceId;
    }

    @Override
    public List<T> extract(CompilationUnit cu) {
        Preconditions.requireNonNull(cu, "CompilationUnit cannot be null");

        List<T> out = new ArrayList<>();
        findTargetDeclarations(cu).forEach(decl -> {
            String id = TypeIdUtils.idFor(cu, decl);
            String name = decl.getNameAsString();
            String packageName = cu.getPackageDeclaration().map(pd -> pd.getName().toString()).orElse("");
            out.add(makeInfo(id, name, packageName, sourceId));
        });
        return out;
    }

    /**
     * Subclasses implement this method to find the specific type declarations they are interested in.
     * @param cu the CompilationUnit to search
     * @return a stream of the target type declarations
     */
    protected abstract Stream<? extends TypeDeclaration<?>> findTargetDeclarations(CompilationUnit cu);
    
    /**
     * Creates a BaseTypeInfo object. Subclasses can override this method to create specific type info objects.
     * @param id Fully qualified ID
     * @param name Name without package
     * @param packageName Package name
     * @param sourceId Source ID
     * @return a BaseTypeInfo object
     */
    protected abstract T makeInfo(String id, String name, String packageName, String sourceId);
}
