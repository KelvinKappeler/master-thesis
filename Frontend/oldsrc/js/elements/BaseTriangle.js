import {Preconditions} from "../utils/Preconditions.js";
import {PWElement} from "./PWElement.js";

/**
 * This class is used to create a triangle that can be used to collapse and expand elements (hide/show a list of DOM elements).
 */
export class BaseTriangle extends PWElement {
    static iconFold = "bi-chevron-down";
    static iconExpanded = "bi-chevron-right";

    /**
     * Creates a new triangle element
     * @param elementsToHide {Element[]} The elements to hide/show
     * @param isCollapse {Boolean} Whether the elements are initially expanded or not
     * @param iconFold {string} The icon to show when the elements are collapsed
     * @param iconExpanded {string} The icon to show when the elements are expanded
     */
    constructor(elementsToHide, isCollapse = false, iconFold = BaseTriangle.iconFold, iconExpanded = BaseTriangle.iconExpanded) {
        Preconditions.checkIfBoolean(isCollapse);

        const element = document.createElement('i');
        element.classList.add('bi');
        element.addEventListener('click', (event) => this.toggle(event));
        super(element);

        this.isCollapse = !isCollapse;
        this.iconFold = iconFold;
        this.iconExpanded = iconExpanded;
        this.elementsToHide = elementsToHide;

        this.toggle();
    }

    /**
     * Collapses the elements
     */
    collapse() {
        this.element.classList.remove(this.iconFold);
        this.element.classList.add(this.iconExpanded);
        this.isCollapse = true;
        this.elementsToHide.forEach(element => element.classList.add('hidden'));
    }

    /**
     * Expands the elements
     */
    expand() {
        this.element.classList.remove(this.iconExpanded);
        this.element.classList.add(this.iconFold);
        this.isCollapse = false;
        this.elementsToHide.forEach(element => element.classList.remove('hidden'));
    }

    /**
     * Toggles the elements
     * @param event {Event} The event that triggered the toggle
     */
    toggle(event = undefined) {
        this.isCollapse ? this.expand() : this.collapse();
    }
}
