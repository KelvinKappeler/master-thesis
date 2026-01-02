import {PWElement} from "./PWElement.js";
import {BaseTriangle} from "./BaseTriangle.js";
import {TextFilterBox} from "./TextFilterBox.js";

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
        this._statesDivByFieldState = new WeakMap();

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
                ${timeline.objectId.replace(":", "")} 
                (v${snapshot.version})`
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
                this._statesDivByFieldState.set(fieldState, statesDiv);

                const fieldTriangle = new BaseTriangle([statesDiv], true);
                fieldTriangle.element.classList.add("triangleFields");

                const headerDiv = document.createElement("div");
                headerDiv.classList.add("fieldHeader");

                fieldTriangle.attachTo(headerDiv);

                const label = document.createElement("span");
                label.appendChild(document.createTextNode(`${fieldState.type} ${fieldName}: `));
                headerDiv.appendChild(label);

                headerDiv.appendChild(this.#renderFieldValue(fieldState));

                const filterBox = new TextFilterBox({
                    placeholder: "Filter (10, v1, v>=3, >=100)",
                    className: "fieldFilterInput",
                    debounceMs: 80
                });

                filterBox.onChange((q) => {
                    this.#applyFieldTimelineFilter(fieldState, q);
                });

                filterBox.attachTo(statesDiv, false);

                fieldDiv.appendChild(headerDiv);
                fieldDiv.appendChild(statesDiv);
                fieldsDiv.appendChild(fieldDiv);

                this.#applyFieldTimelineFilter(fieldState, "");
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

        for (const snap of tl) {
            const fields = snap.fields ?? {};
            const fieldState = fields.get(fieldName);
            if (!fieldState) continue;

            const row = document.createElement("div");
            row.classList.add("fieldState");

            row.dataset.version = String(snap.version);
            row.dataset.valueText = this.#getComparableFieldValueText(fieldState);

            if (snap.version === currentVersion) {
                row.classList.add("currentState");
                row.append(document.createTextNode("▶ "));
            }

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

    #getComparableFieldValueText(fieldState) {
        if (fieldState?.value !== null && fieldState?.value !== undefined) return String(fieldState.value);
        if (fieldState?.objectId) return String(fieldState.objectId);
        return "null";
    }

    #applyFieldTimelineFilter(fieldState, rawQuery) {
        const q = (rawQuery ?? "").trim().toLowerCase();
        const predicate = this.#buildFieldTimelinePredicate(q);

        const statesDiv = this._statesDivByFieldState.get(fieldState);
        if (!statesDiv) return;

        const rows = Array.from(statesDiv.children);
        for (const row of rows) {
            if (!(row instanceof HTMLElement)) continue;
            if (!row.classList.contains("fieldState")) continue;

            const ok = predicate(row);
            row.style.display = ok ? "" : "none";
        }
    }

    #buildFieldTimelinePredicate(q) {
        if (!q) return () => true;

        const cmp = (op, a, b) => {
            switch (op) {
                case "<": return a < b;
                case "<=": return a <= b;
                case ">": return a > b;
                case ">=": return a >= b;
                case "=": return a === b;
                case "==": return a === b;
                case "!=": return a !== b;
                default: return false;
            }
        };

        const vMatch = q.match(/^v\s*(<=|>=|!=|==|=|<|>)?\s*(\d+)$/i);
        if (vMatch) {
            const op = vMatch[1] ?? "=";
            const wanted = Number(vMatch[2]);

            return (row) => {
                const ver = Number(row.dataset.version);
                if (Number.isNaN(ver)) return false;
                return cmp(op, ver, wanted);
            };
        }

        const numMatch = q.match(/^(<=|>=|!=|==|=|<|>)\s*(-?\d+(?:\.\d+)?)$/);
        if (numMatch) {
            const op = numMatch[1];
            const wanted = Number(numMatch[2]);

            return (row) => {
                const valueText = (row.dataset.valueText ?? "").trim();
                const valueNum = Number(valueText);
                if (Number.isNaN(valueNum)) return false;
                return cmp(op, valueNum, wanted);
            };
        }

        return (row) => {
            const valueText = (row.dataset.valueText ?? "").toLowerCase();
            const versionText = `v${row.dataset.version ?? ""}`.toLowerCase();
            return valueText.includes(q) || versionText.includes(q);
        };
    }

}