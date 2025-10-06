import {PrimitiveValue} from "../Def.js";
import {Window} from "../Window.js";
import {PWElement} from "../elements/PWElement.js";
import {BaseTriangle} from "../elements/BaseTriangle.js";

/**
 * This class is used to manage the object inspector
 */
export class ObjectInspector extends PWElement {

    static instance = undefined;

    constructor() {
        const element = document.createElement('div');
        element.classList.add('inspectorContent');
        element.id = 'objectInspector';
        super(element);
        this.checkIfEmpty();

        if (ObjectInspector.instance === undefined) {
            ObjectInspector.instance = this;
        } else {
            throw new Error("Only one instance of ObjectInspector is allowed");
        }
    }

    /**
     * Adds a new object to the object inspector
     * @param objectValue {ObjectValue} The object to add
     * @param isWindow {Boolean} Whether the object should be added as a window
     */
    add(objectValue, isWindow = false) {
        const mainDiv = document.createElement('div');
        mainDiv.classList.add('objectInspectorPanel');

        const fieldsDiv = document.createElement('div');
        fieldsDiv.classList.add('fieldsDiv');

        const titleDiv = document.createElement('div');
        const triangle = new BaseTriangle([fieldsDiv]);
        triangle.element.classList.add('triangleObjectInspector');
        triangle.attachTo(titleDiv);
        titleDiv.appendChild(document.createTextNode(
            objectValue.dataType + ": $" + objectValue.shortPointer() + " (v" + objectValue.version + ")"
        ));
        titleDiv.classList.add('objectTitle');
        const openWindow = document.createElement('i');
        openWindow.classList.add('bi');
        openWindow.classList.add('bi-window-plus');
        openWindow.classList.add('closeButton');
        openWindow.addEventListener('click', () => {
            this.add(objectValue, true);
            this.remove(mainDiv);
        });

        const closeButton = document.createElement('i');
        closeButton.classList.add('bi');
        closeButton.classList.add('bi-x');
        closeButton.classList.add('closeButton');
        closeButton.addEventListener('click', () => {
            this.remove(mainDiv);
        });
        if (!isWindow) {
            titleDiv.appendChild(closeButton);
            titleDiv.appendChild(openWindow);
        }
        mainDiv.appendChild(titleDiv);

        for (let i = 0; i < objectValue.fields.length; i++) {
            const field = objectValue.fields[i];
            const fieldDiv = document.createElement('div');

            const statesDiv = this.createFieldStateDiv(objectValue.states, objectValue, field);

            const fieldTriangle = new BaseTriangle([statesDiv], true);
            fieldTriangle.element.classList.add('triangleFields');
            fieldTriangle.attachTo(fieldDiv);
            fieldDiv.classList.add('fieldDiv');
            fieldDiv.appendChild(document.createTextNode(field[1].dataType + " " + field[0] + ": "));
            if (field[1] instanceof PrimitiveValue) {
                fieldDiv.appendChild(document.createTextNode(field[1].value));
            } else {
                fieldDiv.appendChild(field[1].documentFragment());
            }

            fieldDiv.appendChild(statesDiv);
            fieldsDiv.appendChild(fieldDiv);
        }

        if (objectValue.fields.length === 0) {
            const emptyField = document.createElement('div');
            emptyField.innerHTML = 'No fields';
            fieldsDiv.appendChild(emptyField);
        }

        mainDiv.appendChild(fieldsDiv);

        this.element.prepend(mainDiv);
        mainDiv.scrollIntoView();
        this.checkIfEmpty();

        if (isWindow) {
            this.element.prepend(Window.newWindow("Object Inspector", mainDiv,
                () => {
                    this.add(objectValue);
                }));
        }
    }

    /**
     * Clears the object inspector
     */
    clear() {
        this.element.innerHTML = '';
        this.checkIfEmpty();
    }

    /**
     * Removes a child from the object inspector
     * @param child {Element} The child to remove
     */
    remove(child) {
        this.element.removeChild(child);
        this.checkIfEmpty();
    }

    /**
     * Creates an empty state div
     * @returns {HTMLDivElement} The empty state div
     */
    static createEmptyState() {
        const div = document.createElement('div');
        div.id = 'objectInspectorEmpty';
        div.innerHTML = '<h2>No object selected</h2>';
        return div;
    }

    /**
     * Checks if the object inspector is empty. If it is, it appends an empty state div
     * @returns {boolean} Whether the object inspector is empty
     */
    checkIfEmpty() {
        if (this.element.childElementCount === 0) {
            this.element.append(ObjectInspector.createEmptyState());
            return true;
        }

        document.getElementById('objectInspectorEmpty')?.remove();

        return false;
    }

    /**
     * Creates a field state div
     * @param states The states of the object
     * @param objectValue {ObjectValue} The object value
     * @param currentField The current field
     * @returns {HTMLDivElement} The field state div
     */
    createFieldStateDiv(states, objectValue, currentField) {
        let objectStates = states.stateDictionary.get(objectValue.pointer);
        let fieldStates = objectStates.map(objectVal => [objectVal, objectVal.fields.find(f => f[0] === currentField[0])[1]]);
        const currentVersion = states.getLastVersion(objectValue.pointer, objectValue.version).self.version;

        const mainDiv = document.createElement('div');
        let lastValue = undefined;
        for (let f of fieldStates) {
            const div = document.createElement('div');
            const obj = f[0];
            const value = f[1];

            if (currentVersion === obj.version) {
                div.classList.add('currentState');
                div.append(document.createTextNode('▶ '));
            }
            if (value instanceof PrimitiveValue) {
                if (value.value === lastValue && currentVersion !== obj.version) continue;
                div.append(document.createTextNode(value.value));
                lastValue = value.value;
            } else {
                if (lastValue !== undefined && value.fields === lastValue.fields && currentVersion !== obj.version) continue;
                div.append(value.documentFragment());
                lastValue = value;
            }

            div.append(document.createTextNode(" | v" + obj.version + " | "));
            div.append(obj.documentFragment());

            const viewInTrace = document.createElement('button');
            viewInTrace.classList.add('viewInTrace');
            viewInTrace.innerHTML = '<i class="bi bi-search"></i>';

            viewInTrace.addEventListener('click', () => {
                const traceElement = pw.trace.getTraceElementWithObjectValue(obj);
                let tempTraceElement = traceElement.parent;

                while (tempTraceElement !== undefined) {
                    tempTraceElement.element.style.backgroundColor = '';
                    const triangle = tempTraceElement.triangle;
                    if (triangle) {
                        triangle.expand();
                    }
                    tempTraceElement = tempTraceElement.parent;
                }

                traceElement.element.scrollIntoView({ behavior: 'smooth', block: 'center' });
            });

            viewInTrace.addEventListener('mouseenter', () => {
                let traceElement = pw.trace.getTraceElementWithObjectValue(obj);
                traceElement.element.style.backgroundColor = 'rgba(147,74,172,0.5)';

                while (traceElement !== undefined && traceElement.element.parentNode !== undefined) {
                    if (!traceElement.element.parentNode.classList.contains('hidden')) {
                        traceElement.element.style.backgroundColor = 'rgba(147,74,172,0.5)';
                        break;
                    }
                    traceElement = traceElement.parent;
                }

            });

            viewInTrace.addEventListener('mouseleave', () => {
                let traceElement = pw.trace.getTraceElementWithObjectValue(obj);

                while (traceElement !== undefined && traceElement.element.parentNode !== undefined) {
                    traceElement.element.style.backgroundColor = '';
                    traceElement = traceElement.parent;
                }
            });

            div.append(viewInTrace);
            div.append(document.createElement('br'));
            div.classList.add('fieldState');



            mainDiv.append(div);
        }

        return mainDiv;
    }
}
