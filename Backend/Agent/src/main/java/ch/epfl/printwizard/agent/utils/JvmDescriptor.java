package ch.epfl.printwizard.agent.utils;

/**
 * Represents a utility class for converting JVM descriptors to human-readable forms.
 */
public final class JvmDescriptor {

    private JvmDescriptor() {}

    /**
     * Convert a JVM descriptor to a human-readable form.
     * @param descriptor JVM descriptor (e.g., "I", "[[Ljava/lang/String;", "(I)V")
     * @return array of two strings: parameter types and return type
     */
    public static String[] toHuman(String descriptor) {
        Index idx = new Index();
        String params = "";
        String ret;

        if (descriptor.charAt(0) == '(') {
            idx.pos++;
            StringBuilder p = new StringBuilder();
            boolean first = true;
            while (descriptor.charAt(idx.pos) != ')') {
                String t = readType(descriptor, idx);
                if (!first) p.append(", ");
                p.append(t);
                first = false;
            }
            idx.pos++;
            ret = readType(descriptor, idx);
            if (idx.pos != descriptor.length()) {
                throw new IllegalArgumentException("Trailing characters in descriptor: " + descriptor);
            }
            params = p.toString();
        }
        else {
            String t = readType(descriptor, idx);
            if (t.equals("void")) {
                throw new IllegalArgumentException("'V' is only valid as a method return type");
            }
            if (idx.pos != descriptor.length()) {
                throw new IllegalArgumentException("Trailing characters in descriptor: " + descriptor);
            }
            ret = t;
        }
        return new String[] { params, ret };
    }

    private static String readType(String s, Index idx) {
        int arrayDims = 0;
        while (s.charAt(idx.pos) == '[') {
            arrayDims++;
            idx.pos++;
        }

        char c = s.charAt(idx.pos);
        String base;
        switch (c) {
            case 'B': base = "byte";    idx.pos++; break;
            case 'C': base = "char";    idx.pos++; break;
            case 'D': base = "double";  idx.pos++; break;
            case 'F': base = "float";   idx.pos++; break;
            case 'I': base = "int";     idx.pos++; break;
            case 'J': base = "long";    idx.pos++; break;
            case 'S': base = "short";   idx.pos++; break;
            case 'Z': base = "boolean"; idx.pos++; break;
            case 'V': base = "void";    idx.pos++; break;
            case 'L': {
                int semi = s.indexOf(';', idx.pos);
                if (semi < 0) throw new IllegalArgumentException("Missing ';' in object type: " + s);
                String name = s.substring(idx.pos + 1, semi);
                base = name.replace('/', '.');
                idx.pos = semi + 1;
                break;
            }
            default:
                throw new IllegalArgumentException("Unknown type code '" + c + "' in: " + s);
        }

        if (arrayDims > 0) {
            return base + "[]".repeat(arrayDims);
        }
        return base;
    }

    private static final class Index {
        int pos = 0;
    }
}

