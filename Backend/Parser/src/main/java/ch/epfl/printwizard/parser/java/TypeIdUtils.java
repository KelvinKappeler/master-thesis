package ch.epfl.printwizard.parser.java;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedTypeParameterDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a utility class for generating unique type identifiers for Java types.
 */
public final class TypeIdUtils {
    
    private TypeIdUtils() {}

    /**
     * Generates a unique identifier for a given type declaration within a compilation unit.
     * @param cu the compilation unit
     * @param td the type declaration
     * @return the unique identifier in the format "prefix:fully.qualified.Name"
     */
    public static String idFor(CompilationUnit cu, TypeDeclaration<?> td) {
        return prefix(td) + ":" + fqName(cu, td);
    }

    /**
     * Determines the prefix for the type declaration based on its kind (class, interface, record, etc.).
     * @param td the type declaration
     * @return the prefix string
     */
    public static String prefix(TypeDeclaration<?> td) {
        if (td instanceof ClassOrInterfaceDeclaration classOrInterfaceDeclaration) {
            return classOrInterfaceDeclaration.isInterface() ? "intf" : "cls";
        }
        if (td instanceof RecordDeclaration) return "rec";
        return "type";
    }

    /**
     * Generates the fully qualified name for a given type declaration within a compilation unit.
     * @param cu the compilation unit
     * @param td the type declaration
     * @return the fully qualified name
     */
    public static String fqName(CompilationUnit cu, TypeDeclaration<?> td) {
        String pkg = cu.getPackageDeclaration().map(pd -> pd.getName().toString()).orElse("");
        String nested = nestedName(td);
        return pkg.isEmpty() ? nested : pkg + "." + nested;
    }

    private static String nestedName(TypeDeclaration<?> td) {
        List<String> parts = new ArrayList<>();
        Node cur = td;
        while (cur instanceof TypeDeclaration) {
            parts.addFirst(((TypeDeclaration<?>) cur).getNameAsString());
            cur = cur.getParentNode().orElse(null);
        }
        return String.join(".", parts);
    }

    public static String canonical(Type t) {
        if (t.isVoidType()) return "void";
        if (t.isPrimitiveType()) return t.asPrimitiveType().toString();
        if (t.isArrayType()) return canonical(t.asArrayType().getComponentType()) + "[]";

        try {
            ResolvedType rt = t.resolve();
            return canonical(rt);
        } catch (UnsolvedSymbolException | UnsupportedOperationException e) {
            return t.toString();
        }
    }

    public static String canonical(ResolvedType rt) {
        if (rt.isPrimitive()) return rt.describe();
        if (rt.isVoid()) return "void";
        if (rt.isArray()) return canonical(rt.asArrayType().getComponentType()) + "[]";
        if (rt.isReferenceType()) return rt.asReferenceType().getQualifiedName();

        if (rt.isTypeVariable()) {
            ResolvedTypeParameterDeclaration tp = rt.asTypeVariable().asTypeParameter();

            if (!tp.getBounds().isEmpty()) {
                ResolvedType firstBound = tp.getBounds().getFirst().getType();

                return canonical(firstBound);
            }
        }

        return "java.lang.Object";
    }
}
