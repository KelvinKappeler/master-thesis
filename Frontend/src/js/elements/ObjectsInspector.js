import {PWElement} from "./PWElement.js";
import {TextFilterBox} from "./TextFilterBox.js";

export class ObjectsInspector extends PWElement {
    constructor(traceModel, objectInspector) {
        const element = document.createElement("div");
        element.classList.add("inspectorContent");
        element.id = "objectsInspector";
        super(element);

        this.traceModel = traceModel;

        this._selectedObjectId = null;
        this._filterText = "";
        this._rowByObjectId = new Map();

        this.objectInspector = objectInspector;

        this.#renderShell();
        this.checkIfEmpty();
    }

    checkIfEmpty() {
        if (this.element.childElementCount === 0) {
            this.element.append(ObjectsInspector.createEmptyState());
            return true;
        }
        document.getElementById("objectsInspectorEmpty")?.remove();
        return false;
    }

    static createEmptyState() {
        const div = document.createElement("div");
        div.id = "objectsInspectorEmpty";
        div.innerHTML = "<h2>No object selected</h2>";
        return div;
    }

    clear() {
        this.element.innerHTML = "";
        this._rowByObjectId.clear();
        this._selectedObjectId = null;
        this._filterText = "";
        this.objectInspector.clear();
        this.#renderShell();
        this.checkIfEmpty();
    }

    select(objectId) {
        if (objectId == null) return;

        const key = String(objectId);
        this._selectedObjectId = key;

        for (const [id, row] of this._rowByObjectId.entries()) {
            if (!(row instanceof HTMLElement)) continue;
            row.classList.toggle("selected", id === key);
        }

        this.#renderDetails(key);
        this.#ensureRowVisible(key);
    }

    #renderShell() {
        this.element.innerHTML = "";

        const root = document.createElement("div");
        root.classList.add("objectsInspectorRoot");

        const header = document.createElement("div");
        header.classList.add("objectsInspectorHeader");

        const title = document.createElement("div");
        title.classList.add("objectsInspectorTitle");
        title.textContent = "Object Inspector";

        header.appendChild(title);

        const columns = document.createElement("div");
        columns.classList.add("objectsInspectorColumns");

        const left = document.createElement("div");
        left.classList.add("objectsInspectorLeft");

        const filterRow = document.createElement("div");
        filterRow.classList.add("objectsInspectorFilterRow");

        const filterBox = new TextFilterBox({
            placeholder: "Search an object (ex: MyType, obj:42, id:obj:7)",
            className: "objectsInspectorFilterInput",
            debounceMs: 100
        });

        filterBox.onChange((q) => {
            this._filterText = (q ?? "").trim().toLowerCase();
            this.#applyListFilter();
        });

        filterRow.appendChild(filterBox.element);

        const list = document.createElement("div");
        list.classList.add("objectsInspectorList");
        list.id = "objectsInspectorList";

        left.appendChild(filterRow);
        left.appendChild(list);

        const right = document.createElement("div");
        right.classList.add("objectsInspectorRight");

        const details = document.createElement("div");
        details.classList.add("objectsInspectorDetails");
        details.id = "objectsInspectorDetails";
        details.innerHTML = "<div class='objectsInspectorHint'>Select an object in the list to display its details</div>";

        right.appendChild(details);

        columns.appendChild(left);
        columns.appendChild(right);

        root.appendChild(header);
        root.appendChild(columns);

        this.element.appendChild(root);

        this.#buildObjectList();
    }

    #buildObjectList() {
        const list = this.element.querySelector("#objectsInspectorList");
        if (!list) return;

        list.innerHTML = "";
        this._rowByObjectId.clear();

        const entries = Array.from(this.traceModel?.objectsById?.entries?.() ?? []);
        if (entries.length === 0) {
            const empty = document.createElement("div");
            empty.classList.add("objectsInspectorEmptyList");
            empty.textContent = "No objects";
            list.appendChild(empty);
            return;
        }

        entries.sort((a, b) => {
            const ta = String(a[1]?.type ?? "");
            const tb = String(b[1]?.type ?? "");
            const cmpT = ta.localeCompare(tb);
            if (cmpT !== 0) return cmpT;
            return String(a[0]).localeCompare(String(b[0]));
        });

        for (const [objectId, timeline] of entries) {
            const oid = String(objectId);
            const type = String(timeline?.type ?? "???").split(".").pop();
            const versions = Array.isArray(timeline?.timeline) ? timeline.timeline.length : 0;

            const row = document.createElement("div");
            row.classList.add("objectsInspectorRow");
            row.dataset.objectId = oid;

            const labelText = `${type} — ${oid.replace(":", "")}`;
            const metaText = `${versions} version(s)`;

            row.dataset.searchText = `${labelText} ${metaText} ${oid} ${type}`.toLowerCase();

            const label = document.createElement("div");
            label.classList.add("objectsInspectorRowLabel");
            label.textContent = labelText;

            const meta = document.createElement("div");
            meta.classList.add("objectsInspectorRowMeta");
            meta.textContent = metaText;

            row.appendChild(label);
            row.appendChild(meta);

            row.addEventListener("click", () => this.select(oid));

            this._rowByObjectId.set(oid, row);
            list.appendChild(row);
        }

        this.#applyListFilter();
    }

    #applyListFilter() {
        const q = (this._filterText ?? "").trim().toLowerCase();
        for (const [, row] of this._rowByObjectId.entries()) {
            const hay = (row.dataset.searchText ?? "").toLowerCase();
            const ok = !q || hay.includes(q);
            row.style.display = ok ? "" : "none";
        }
    }

    #ensureRowVisible(objectId) {
        const row = this._rowByObjectId.get(objectId);
        if (!row) return;
        row.scrollIntoView({block: "nearest"});
    }

    #renderDetails(objectId) {
        const details = this.element.querySelector("#objectsInspectorDetails");
        if (!details) return;

        const timeline = this.traceModel?.objectsById?.get?.(objectId);
        if (!timeline) {
            details.innerHTML = `<div class="objectsInspectorError">Object not found: ${objectId}</div>`;
            return;
        }

        details.innerHTML = "";

        const head = document.createElement("div");
        head.classList.add("objectsInspectorDetailsHead");

        const title = document.createElement("div");
        title.classList.add("objectsInspectorDetailsTitle");
        title.textContent = `${String(timeline.type ?? "???").split(".").pop()} — ${String(objectId).replace(":", "")}`;

        const actions = document.createElement("div");
        actions.classList.add("objectsInspectorActions");

        const btnOpenLatest = this.#makeButton(
            "Open latest",
            "Open latest version in the inspector",
            () => {
                const tl = Array.isArray(timeline.timeline) ? timeline.timeline : [];
                const last = tl[tl.length - 1];
                const eventId = last?.eventId ?? null;
                const version = last?.version ?? null;

                window.dispatchEvent(new CustomEvent("pw:inspect-object", {
                    detail: {objectId: objectId, eventId: eventId, objectVersion:version}
                }))
            }
        );

        const btnClear = this.#makeButton(
            "Clear",
            "Clear the inspector",
            () => this.objectInspector.clear()
        );

        actions.appendChild(btnOpenLatest);
        actions.appendChild(btnClear);

        head.appendChild(title);
        head.appendChild(actions);

        const body = document.createElement("div");
        body.classList.add("objectsInspectorDetailsBody");

        const versionsDiv = document.createElement("div");
        versionsDiv.classList.add("objectsInspectorVersions");

        const h = document.createElement("div");
        h.classList.add("objectsInspectorSectionTitle");
        h.textContent = "Versions";
        versionsDiv.appendChild(h);

        const tl = Array.isArray(timeline.timeline) ? timeline.timeline : [];
        if (tl.length === 0) {
            const no = document.createElement("div");
            no.classList.add("objectsInspectorMuted");
            no.textContent = "—";
            versionsDiv.appendChild(no);
        } else {
            for (const snap of tl) {
                const row = document.createElement("div");
                row.classList.add("objectsInspectorVersionRow");

                const v = snap?.version ?? "?";
                const eventId = snap?.eventId ?? null;

                const left = document.createElement("div");
                left.classList.add("objectsInspectorVersionLabel");
                left.textContent = `v${v}`;

                const right = document.createElement("div");
                right.classList.add("objectsInspectorVersionMeta");
                right.textContent = eventId ? `${eventId}` : "—";

                const btn = document.createElement("button");
                btn.type = "button";
                btn.classList.add("objectsInspectorButton");
                btn.textContent = "Open";
                btn.title = "Open this version in the inspector";
                btn.addEventListener("click", (e) => {
                    e.preventDefault?.();
                    e.stopPropagation?.();
                    window.dispatchEvent(new CustomEvent("pw:inspect-object", {
                        detail: {objectId: objectId, eventId: eventId, objectVersion:v}
                    }))
                });

                const btnReveal = document.createElement("button");
                btnReveal.type = "button";
                btnReveal.classList.add("objectsInspectorButton");
                btnReveal.textContent = "Reveal";
                btnReveal.title = "Reveal the snapshot event in the trace";
                btnReveal.addEventListener("click", () => {
                    window.dispatchEvent(new CustomEvent("pw:reveal-event", { detail: { eventId: eventId } }));
                });
                btnReveal.addEventListener("mouseenter", () => {
                    window.dispatchEvent(new CustomEvent("pw:preview-event", { detail: { eventId: eventId, on: true } }));
                });
                btnReveal.addEventListener("mouseleave", () => {
                    window.dispatchEvent(new CustomEvent("pw:preview-event", { detail: { eventId: eventId, on: false } }));
                });

                row.appendChild(left);
                row.appendChild(right);
                row.appendChild(btn);
                row.appendChild(btnReveal);

                versionsDiv.appendChild(row);
            }
        }

        details.appendChild(head);
        details.appendChild(body);
        details.appendChild(versionsDiv);
    }

    #makeButton(text, title, onClick) {
        const btn = document.createElement("button");
        btn.type = "button";
        btn.classList.add("objectsInspectorButton");
        btn.textContent = text;
        btn.title = title;
        btn.addEventListener("click", () => onClick?.());
        return btn;
    }
}
