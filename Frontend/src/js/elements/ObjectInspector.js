import {PWElement} from "./PWElement.js";
import {BaseTriangle} from "./BaseTriangle.js";

/**
 * Represents the object inspector.
 */
export class ObjectInspector extends PWElement {
    constructor(traceModel) {
        const element = document.createElement("div");
        element.classList.add("inspectorContent");
        element.id = "objectInspector";
        super(element);

        this.traceModel = traceModel;

        this.checkIfEmpty();
    }

    /**
     * Add a new object to the object inspector.
     */
    add(objectId, eventId) {
        const timeline = this.traceModel.objectsById.get(objectId);
        const snapshot = timeline.timeline.find(s => s.eventId === eventId);

        const mainDiv = document.createElement("div");
        mainDiv.classList.add("objectInspectorPanel");

        const fieldsDiv = document.createElement("div");
        fieldsDiv.classList.add("fieldsDiv");

        const titleDiv = document.createElement("div");
        titleDiv.classList.add("objectTitle");

        const triangle = new BaseTriangle([fieldsDiv]);
        triangle.element.classList.add("triangleObjectInspector");
        triangle.attachTo(titleDiv);

        titleDiv.appendChild(
            document.createTextNode(`
                ${timeline.type.split(".").pop()}:
                ${timeline.objectId.replace(":", "")}`
            )
        );

        const closeIcon = document.createElement("i");
        closeIcon.classList.add("bi", "bi-x", "inspectorIconButton");
        closeIcon.title = "Remove from inspector";
        closeIcon.addEventListener("click", (e) => {
            e.preventDefault();
            e.stopPropagation();
            this.remove(mainDiv);
        });
        titleDiv.appendChild(closeIcon);


        mainDiv.appendChild(titleDiv);

        if (snapshot.fields.size === 0) {
            const emptyField = document.createElement("div");
            emptyField.textContent = "No fields";
            fieldsDiv.appendChild(emptyField);
        } else {
            for (const [fieldName, fieldState] of snapshot.fields) {
                const fieldDiv = document.createElement("div");
                fieldDiv.classList.add("fieldDiv");

                const statesDiv = this.#createFieldTimelineDiv(timeline, fieldName, snapshot.version);

                const fieldTriangle = new BaseTriangle([statesDiv], true);
                fieldTriangle.element.classList.add("triangleFields");
                fieldTriangle.attachTo(fieldDiv);

                const label = document.createElement("span");
                label.appendChild(document.createTextNode(`${fieldState.type} ${fieldName}: `));
                fieldDiv.appendChild(label);

                fieldDiv.appendChild(this.#renderFieldValue(fieldState));

                fieldDiv.appendChild(statesDiv);
                fieldsDiv.appendChild(fieldDiv);
            }
        }

        mainDiv.appendChild(fieldsDiv);

        this.element.prepend(mainDiv);
        mainDiv.scrollIntoView();
        this.checkIfEmpty();
    }

    clear() {
        this.element.innerHTML = "";
        this.checkIfEmpty();
    }

    remove(child) {
        this.element.removeChild(child);
        this.checkIfEmpty();
    }

    static createEmptyState() {
        const div = document.createElement("div");
        div.id = "objectInspectorEmpty";
        div.innerHTML = "<h2>No object selected</h2>";
        return div;
    }

    checkIfEmpty() {
        if (this.element.childElementCount === 0) {
            this.element.append(ObjectInspector.createEmptyState());
            return true;
        }
        document.getElementById("objectInspectorEmpty")?.remove();
        return false;
    }

    #renderFieldValue(fieldState) {
        const span = document.createElement("span");

        if (fieldState.value !== null && fieldState.value !== undefined) {
            span.textContent = String(fieldState.value);
            return span;
        }

        if (fieldState.objectId) {
            const a = document.createElement("a");
            a.href = "#";
            a.textContent = fieldState.objectId;
            a.addEventListener("click", (e) => {
                e.preventDefault();
                this.add(fieldState.objectId);
            });
            span.appendChild(a);
            return span;
        }

        span.textContent = "null";
        return span;
    }

    #createFieldTimelineDiv(objectTimeline, fieldName, currentVersion) {
        const mainDiv = document.createElement("div");

        const tl = Array.isArray(objectTimeline.timeline) ? objectTimeline.timeline : [];

        let lastRenderedKey = null;

        for (const snap of tl) {
            const fields = snap.fields ?? {};
            const fieldState = fields.get(fieldName);
            if (!fieldState) continue;

            const row = document.createElement("div");
            row.classList.add("fieldState");

            if (snap.version === currentVersion) {
                row.classList.add("currentState");
                row.append(document.createTextNode("▶ "));
            }

            const renderKey = JSON.stringify({
                type: fieldState.type,
                value: fieldState.value ?? null,
                objectId: fieldState.objectId ?? null
            });

            if (renderKey === lastRenderedKey && snap.version !== currentVersion) {
                continue;
            }
            lastRenderedKey = renderKey;

            row.appendChild(this.#renderFieldValue(fieldState));
            row.append(document.createTextNode(` | v${snap.version} | `));

            const btn = document.createElement("button");
            btn.classList.add("viewInTrace");
            btn.textContent = "🔎";
            btn.title = "Show event";

            const targetEventId = snap.eventId;

            btn.addEventListener("click", () => {
                window.dispatchEvent(new CustomEvent("pw:reveal-event", { detail: { eventId: targetEventId } }));
            });
            btn.addEventListener("mouseenter", () => {
                window.dispatchEvent(new CustomEvent("pw:preview-event", { detail: { eventId: targetEventId, on: true } }));
            });
            btn.addEventListener("mouseleave", () => {
                window.dispatchEvent(new CustomEvent("pw:preview-event", { detail: { eventId: targetEventId, on: false } }));
            });


            row.appendChild(btn);
            row.appendChild(document.createElement("br"));

            mainDiv.appendChild(row);
        }

        return mainDiv;
    }
}