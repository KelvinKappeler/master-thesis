package ch.epfl.printwizard.shared.utils;

import java.util.List;

/**
 * Utility class for converting Java types to JVM descriptor format
 */
public final class TypeIdUtils {

    private TypeIdUtils() {}

    /**
     * Builds a JVM method descriptor from parameter types and return type
     * @param paramTypes list of parameter types (e.g., ["String", "int"])
     * @param returnType return type (e.g., "void", "int", "<init>")
     * @param isConstructor true if this is a constructor
     * @return JVM descriptor (e.g., "(Ljava/lang/String;I)V")
     */
    public static String buildMethodDescriptor(List<String> paramTypes, String returnType, boolean isConstructor) {
        StringBuilder sb = new StringBuilder("(");

        for (String paramType : paramTypes) {
            sb.append(toJvmType(paramType));
        }

        sb.append(")");

        if (isConstructor) {
            sb.append("V");
        } else {
            sb.append(toJvmType(returnType));
        }

        return sb.toString();
    }

    /**
     * Converts a Java type to JVM descriptor format
     * @param javaType Java type (e.g., "String", "int", "String[]")
     * @return JVM descriptor (e.g., "Ljava/lang/String;", "I", "[Ljava/lang/String;")
     */
    public static String toJvmType(String javaType) {
        // Handle arrays
        int arrayDimensions = 0;
        String baseType = javaType;
        while (baseType.endsWith("[]")) {
            arrayDimensions++;
            baseType = baseType.substring(0, baseType.length() - 2);
        }

        String arrayPrefix = "[".repeat(arrayDimensions);

        // Handle primitive types
        String jvmBaseType = switch (baseType) {
            case "void" -> "V";
            case "boolean" -> "Z";
            case "byte" -> "B";
            case "char" -> "C";
            case "short" -> "S";
            case "int" -> "I";
            case "long" -> "J";
            case "float" -> "F";
            case "double" -> "D";
            default -> "L" + baseType.replace('.', '/') + ";";
        };

        return arrayPrefix + jvmBaseType;
    }
}

