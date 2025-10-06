export class Preconditions {

    /**
     * Asserts that the provided value is neither null nor undefined. If it is, throws an Error with the provided message.
     * @param value The value to check
     * @param message {string} The error message if the value is null or undefined
     */
    static requireNonNull(value, message = "Value must not be null or undefined") {
        if (value === null || value === undefined) {
            throw new Error(message);
        }
    }

    /**
     * Asserts that a condition is true. If not, throws an Error with the provided message.
     * @param condition {boolean} The condition to check
     * @param message {string} The error message if the condition is false
     */
    static require(condition, message = "Condition failed") {
        if (!condition) {
            throw new Error(message);
        }
    }
}