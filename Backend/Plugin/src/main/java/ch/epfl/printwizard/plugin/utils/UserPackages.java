package ch.epfl.printwizard.plugin.utils;

import com.sun.tools.javac.tree.JCTree;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents the set of user packages.
 * Used to determine if a package is a user-defined package.
 */
public final class UserPackages {

    private static final Set<String> PACKAGES = ConcurrentHashMap.newKeySet();

    private UserPackages() {}

    /**
     * Registers a compilation unit.
     * @param cu the compilation unit to register
     */
    public static void registerCompilationUnit(JCTree.JCCompilationUnit cu) {
        if (cu.packge != null) {
            String pkg = cu.packge.toString();
            if (!pkg.isEmpty()) {
                PACKAGES.add(pkg);
            }
        }
    }

    /**
     * Determines if a package is a user-defined package.
     * @param pkg the package to check
     * @return true, if the package is a user-defined package, false otherwise
     */
    public static boolean isUserPackage(String pkg) {
        if (pkg == null || pkg.isEmpty()) {
            return false;
        }

        for (String base : PACKAGES) {
            if (pkg.equals(base) || pkg.startsWith(base + ".")) {
                return true;
            }
        }

        return false;
    }

}
