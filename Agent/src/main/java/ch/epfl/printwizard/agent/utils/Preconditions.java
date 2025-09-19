package ch.epfl.printwizard.agent.utils;

/**
 * Utility class for checking preconditions.
 */
public final class Preconditions {
    /**
     * Checks that a condition is true and throws an IllegalArgumentException with the given message if it is not.
     * @param condition the condition to check
     * @param message the message to include in the exception if the condition is false
     * @throws IllegalArgumentException if the condition is false
     */
    public static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Checks that an object is not null and throws an IllegalArgumentException with the given message if it is.
     * @param obj the object to check
     * @param message the message to include in the exception if the object is null
     * @throws IllegalArgumentException if the object is null
     */
    public static void requireNonNull(Object obj, String message) {
        if (obj == null) {
            throw new IllegalArgumentException(message);
        }
    }
}
