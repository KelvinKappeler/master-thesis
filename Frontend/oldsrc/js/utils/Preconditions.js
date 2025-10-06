/**
 * This class contains static methods that can be used to check preconditions.
 */
export class Preconditions {
    /**
     * Checks if the given object is of the given type.
     * @param object The object to check.
     * @param type The type to check against.
     * @throws Error if the object is not of the given type.
     */
    static checkType(object, type) {
        if (!(object instanceof type)) {
            throw new Error(`Expected ${type.name} but got ${object.constructor.name}`);
        }
    }

    /**
     * Checks if the given field is a boolean.
     * @param boolean The field to check.
     */
    static checkIfBoolean(boolean) {
        if (typeof boolean !== 'boolean') {
            throw new Error(`Expected boolean but got ${typeof boolean}`);
        }
    }

    /**
     * Checks if the given field is a string.
     * @param string The field to check.
     */
    static checkIfString(string) {
        if (typeof string !== 'string') {
            throw new Error(`Expected string but got ${typeof string}`);
        }
    }

    /**
     * Checks if the given object is an array of the given type.
     * @param array The array to check.
     * @param type The type to check against.
     */
    static checkArrayOfTypes(array, type) {
        if (!Array.isArray(array)) {
            throw new Error(`Expected an array but got ${typeof array}`);
        }
        for (const element of array) {
            if (!(element instanceof type)) {
                throw new Error(`Expected an array of ${type.name} but got ${element.constructor.name}`);
            }
        }
    }
}
