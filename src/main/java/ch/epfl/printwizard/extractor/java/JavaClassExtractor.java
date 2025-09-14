package ch.epfl.printwizard.extractor.java;

import ch.epfl.printwizard.extractor.IExtractor;
import ch.epfl.printwizard.model.ClassInfo;
import ch.epfl.printwizard.utils.Preconditions;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an extractor that extracts class information from a Java CompilationUnit.
 */
public final class JavaClassExtractor implements IExtractor<CompilationUnit, ClassInfo> {

    private static final String CLASS_ID_PREFIX = "cls";
    private final String sourceId;

    public JavaClassExtractor(String sourceId) {
        Preconditions.RequireNonNull(sourceId, "Source ID cannot be null");
        Preconditions.Require(!sourceId.isEmpty(), "Source ID cannot be empty");

        this.sourceId = sourceId;
    }

    @Override
    public List<ClassInfo> extract(CompilationUnit compilationUnit) {
        Preconditions.RequireNonNull(compilationUnit, "CompilationUnit cannot be null");

        List<ClassInfo> classInfos = new ArrayList<>();

        String packageName = compilationUnit.getPackageDeclaration().map(pd -> pd.getName().toString()).orElse("");

        for (ClassOrInterfaceDeclaration decl : compilationUnit.findAll(ClassOrInterfaceDeclaration.class)) {
            String className = decl.getNameAsString();
            String classId = CLASS_ID_PREFIX + ":" + (packageName.isEmpty() ? className : packageName + "." + className);

            classInfos.add(new ClassInfo(classId, className, packageName, sourceId));
        }

        return classInfos;
    }
}
