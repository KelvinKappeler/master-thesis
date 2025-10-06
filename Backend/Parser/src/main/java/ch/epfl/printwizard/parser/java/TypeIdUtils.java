package ch.epfl.printwizard.parser.java;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;

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
}
