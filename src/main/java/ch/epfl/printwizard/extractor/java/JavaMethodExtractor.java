package ch.epfl.printwizard.extractor.java;

import ch.epfl.printwizard.extractor.IExtractor;
import ch.epfl.printwizard.model.MethodInfo;
import ch.epfl.printwizard.utils.Preconditions;
import com.github.javaparser.ast.body.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an extractor for Java methods.
 */
public final class JavaMethodExtractor implements IExtractor<TypeDeclaration<?>, MethodInfo> {

    private static final String METHOD_ID_PREFIX = "m";
    
    private final String sourceId;
    private final String classId;

    /**
     * Constructs a JavaMethodExtractor with the given source ID.
     *
     * @param sourceId the source ID
     */
    public JavaMethodExtractor(String sourceId, String classId) {
        Preconditions.RequireNonNull(sourceId, "Source ID cannot be null");
        Preconditions.RequireNonNull(classId, "Class ID cannot be null");
        Preconditions.Require(!sourceId.isEmpty(), "Source ID cannot be empty");
        Preconditions.Require(!classId.isEmpty(), "Class ID cannot be empty");
        
        this.sourceId = sourceId;
        this.classId = classId;
    }

    @Override
    public List<MethodInfo> extract(TypeDeclaration<?> declaration) {
        Preconditions.RequireNonNull(declaration, "Declaration cannot be null");
        
        List<MethodInfo> methods = new ArrayList<>();
        List<CallableDeclaration<?>> callables = collectCallables(declaration);

        for (var c : callables) {
            methods.add(extractOne(c));
        }
        return methods;
    }

    private static List<CallableDeclaration<?>> collectCallables(TypeDeclaration<?> typeDecl) {
        List<CallableDeclaration<?>> res = new ArrayList<>();
        if (typeDecl instanceof ClassOrInterfaceDeclaration classOrIntDecl) {
            res.addAll(classOrIntDecl.getMethods());
            res.addAll(classOrIntDecl.getConstructors());
        } else if (typeDecl instanceof RecordDeclaration rd) {
            res.addAll(rd.getMethods());
            res.addAll(rd.getConstructors());
        } else {
            res.addAll(typeDecl.findAll(MethodDeclaration.class));
            res.addAll(typeDecl.findAll(ConstructorDeclaration.class));
        }
        return res;
    }
    
    private MethodInfo extractOne(CallableDeclaration<?> c) {
        
    }
}
