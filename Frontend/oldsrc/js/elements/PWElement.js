import {Preconditions} from "../utils/Preconditions.js";

/**
 * Base class for all elements. An element is an object that can be attached to a DOM element.
 */
export class PWElement {

    /**
     * Creates a new element.
     * @param element {Element} The DOM element to create.
     */
    constructor(element) {
        Preconditions.checkType(element, Element);

        this.element = element;
    }

    /**
     * Attaches the triangle to a parent element.
     * @param parent {Element} The parent element to attach the triangle to.
     * @param isAppend {Boolean} Whether to append the triangle to the parent element.
     */
    attachTo(parent, isAppend = true) {
        Preconditions.checkType(parent, Element);
        Preconditions.checkIfBoolean(isAppend);

        if (!isAppend) {
            parent.prepend(this.element);
        } else {
            parent.append(this.element);
        }
    }
}
