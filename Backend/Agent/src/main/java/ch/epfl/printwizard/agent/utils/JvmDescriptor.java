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
        Preconditions.requireNonNull(descriptor, "descriptor is null");
        Preconditions.require(!descriptor.isEmpty(), "descriptor is empty");

        String params = "";
        String ret;

        if (descriptor.charAt(0) == '(') {
            int i = 1;
            int end = descriptor.indexOf(')', i);
            if (end < 0) {
                throw new IllegalArgumentException("Unterminated parameter list: " + descriptor);
            }

            StringBuilder p = new StringBuilder();
            boolean first = true;
            while (i < end) {
                if (!first) p.append(", ");
                StringBuilder t = new StringBuilder();
                int next = readType(descriptor, i, t);
                if (next > end) {
                    throw new IllegalArgumentException("Parameter overruns ')': " + descriptor);
                }
                p.append(t);
                i = next;
                first = false;
            }

            i = end + 1;

            StringBuilder r = new StringBuilder();
            i = readType(descriptor, i, r);

            if (i != descriptor.length()) {
                throw new IllegalArgumentException("Trailing characters in descriptor: " + descriptor);
            }

            params = p.toString();
            ret = r.toString();
        } else {
            StringBuilder r = new StringBuilder();
            int i = readType(descriptor, 0, r);

            if ("void".contentEquals(r)) {
                throw new IllegalArgumentException("'V' is only valid as a method return type");
            }
            if (i != descriptor.length()) {
                throw new IllegalArgumentException("Trailing characters in descriptor: " + descriptor);
            }
            ret = r.toString();
        }

        return new String[] { params, ret };
    }

    /**
     * Reads one JVM type starting at index {@code pos}, appends its human form to {@code out},
     * and returns the next index to read from.
     */
    private static int readType(String s, int pos, StringBuilder out) {
        final int n = s.length();
        int i = pos;

        int dims = 0;
        while (i < n && s.charAt(i) == '[') {
            dims++;
            i++;
        }
        if (i >= n) {
            throw new IllegalArgumentException("Unexpected end of descriptor: " + s);
        }

        char c = s.charAt(i++);
        switch (c) {
            case 'B': out.append("byte");    break;
            case 'C': out.append("char");    break;
            case 'D': out.append("double");  break;
            case 'F': out.append("float");   break;
            case 'I': out.append("int");     break;
            case 'J': out.append("long");    break;
            case 'S': out.append("short");   break;
            case 'Z': out.append("boolean"); break;
            case 'V': out.append("void");    break;
            case 'L': {
                int semi = s.indexOf(';', i);
                if (semi < 0) {
                    throw new IllegalArgumentException("Missing ';' in object type: " + s);
                }
                String name = s.substring(i, semi).replace('/', '.');
                out.append(name);
                i = semi + 1;
                break;
            }
            default:
                throw new IllegalArgumentException("Unknown type code '" + c + "' in: " + s);
        }

        out.append("[]".repeat(Math.max(0, dims)));

        return i;
    }
}

