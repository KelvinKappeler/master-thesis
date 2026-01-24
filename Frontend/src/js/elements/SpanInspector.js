import {PWElement} from "./PWElement.js";
import {TextFilterBox} from "./TextFilterBox.js";
import {TraceFilterType} from "../view/TraceFilterType.js";
import {TraceSpanType} from "../view/TraceSpanType.js";
import {TraceSpan} from "../view/TraceSpan.js";

/**
 * Represent the span inspector that shows details about a selected span.
 */
export class SpanInspector extends PWElement {
    constructor(traceModel) {
        const element = document.createElement("div");
        element.classList.add("inspectorContent");
        element.id = "spanInspector";
        super(element);

        this.traceModel = traceModel;

        this._selectedSpanId = null;
        this._filterText = "";

        this._rowBySpanId = new Map();

        this.#renderShell();
        this.checkIfEmpty();
    }

    checkIfEmpty() {
        if (this.element.childElementCount === 0) {
            this.element.append(SpanInspector.createEmptyState());
            return true;
        }
        document.getElementById("spanInspectorEmpty")?.remove();
        return false;
    }

    static createEmptyState() {
        const div = document.createElement("div");
        div.id = "spanInspectorEmpty";
        div.innerHTML = "<h2>No span selected</h2>";
        return div;
    }

    clear() {
        this.element.innerHTML = "";
        this._rowBySpanId.clear();
        this._selectedSpanId = null;
        this._filterText = "";
        this.#renderShell();
        this.checkIfEmpty();
    }

    /**
     * Select a span in the span inspector and display its details.
     * @param spanId {string}
     */
    select(spanId) {
        if (spanId == null) return;

        const selectedKey = String(spanId);
        this._selectedSpanId = selectedKey;

        for (const [id, row] of this._rowBySpanId.entries()) {
            if (!(row instanceof HTMLElement)) continue;
            row.classList.toggle("selected", id === selectedKey);
        }

        this.#renderDetails(selectedKey);
        this.#ensureRowVisible(selectedKey);
    }

    /**
     * Select a span from an event ID.
     * @param eventId {string} The event ID to select the span from.
     */
    selectFromEvent(eventId) {
        const ev = this.traceModel.getEvent(eventId);
        if (!ev?.spanId) return;
        this.select(ev.spanId);
    }

    #renderShell() {
        this.element.innerHTML = "";

        const root = document.createElement("div");
        root.classList.add("spanInspectorRoot");

        const header = document.createElement("div");
        header.classList.add("spanInspectorHeader");

        const title = document.createElement("div");
        title.classList.add("spanInspectorTitle");
        title.textContent = "Span Inspector";

        header.appendChild(title);

        const columns = document.createElement("div");
        columns.classList.add("spanInspectorColumns");

        const left = document.createElement("div");
        left.classList.add("spanInspectorLeft");

        const filterRow = document.createElement("div");
        filterRow.classList.add("spanInspectorFilterRow");

        const filterBox = new TextFilterBox({
            placeholder: "Search a span (ex: main, Class.method, spn:42, line:12)",
            className: "spanInspectorFilterInput",
            debounceMs: 100
        });

        filterBox.onChange((q) => {
            this._filterText = (q ?? "").trim().toLowerCase();
            this.#applyListFilter();
        });

        filterRow.appendChild(filterBox.element);

        const list = document.createElement("div");
        list.classList.add("spanInspectorList");
        list.id = "spanInspectorList";

        left.appendChild(filterRow);
        left.appendChild(list);

        const right = document.createElement("div");
        right.classList.add("spanInspectorRight");

        const details = document.createElement("div");
        details.classList.add("spanInspectorDetails");
        details.id = "spanInspectorDetails";
        details.innerHTML = "<div class='spanInspectorHint'>Select a span in the list to display its details</div>";

        right.appendChild(details);

        columns.appendChild(left);
        columns.appendChild(right);

        root.appendChild(header);
        root.appendChild(columns);

        this.element.appendChild(root);

        this.#buildSpanList();
    }

    #buildSpanList() {
        const list = this.element.querySelector("#spanInspectorList");
        if (!list) return;

        list.innerHTML = "";
        this._rowBySpanId.clear();

        const spans = this.traceModel.getSpans();

        if (spans.length === 0) {
            const empty = document.createElement("div");
            empty.classList.add("spanInspectorEmptyList");
            empty.textContent = "No spans";
            list.appendChild(empty);
            return;
        }

        for (const sp of spans) {
            const sid = String(sp.id);

            const row = document.createElement("div");
            row.classList.add("spanInspectorRow");
            row.dataset.spanId = sid;

            const labelText = this.#formatSpanLabel(sp);
            const metaText = this.#formatSpanMeta(sp);

            row.dataset.searchText = `${labelText} ${metaText} ${sid}`.toLowerCase();

            const label = document.createElement("div");
            label.classList.add("spanInspectorRowLabel");
            label.textContent = labelText;

            const meta = document.createElement("div");
            meta.classList.add("spanInspectorRowMeta");
            meta.textContent = metaText;

            row.appendChild(label);
            row.appendChild(meta);

            row.addEventListener("click", () => {
                this.select(sid);
            });

            this._rowBySpanId.set(sid, row);
            list.appendChild(row);
        }

        this.#applyListFilter();
    }

    #applyListFilter() {
        const q = (this._filterText ?? "").trim().toLowerCase();

        for (const [, row] of this._rowBySpanId.entries()) {
            const hay = (row.dataset.searchText ?? "").toLowerCase();
            const ok = !q || hay.includes(q);
            row.style.display = ok ? "" : "none";
        }
    }

    #ensureRowVisible(spanId) {
        const row = this._rowBySpanId.get(spanId);
        if (!row) return;

        const list = this.element.querySelector("#spanInspectorList");
        if (!list) return;

        row.scrollIntoView({block: "nearest"});
    }

    #renderDetails(spanId) {
        const details = this.element.querySelector("#spanInspectorDetails");
        if (!details) return;

        const span = this.traceModel.getSpan(spanId);
        if (!span) {
            details.innerHTML = `<div class="spanInspectorError">Span introuvable: ${spanId}</div>`;
            return;
        }

        const method = this.traceModel.getMethod(span.methodId);
        const clazz = method ? this.traceModel.getClass(method.classId) : null;

        details.innerHTML = "";

        const head = document.createElement("div");
        head.classList.add("spanInspectorDetailsHead");

        const h = document.createElement("div");
        h.classList.add("spanInspectorDetailsTitle");

        const actions = document.createElement("div");
        actions.classList.add("spanInspectorActions");

        const btnRevealStart = this.#makeButton("Reveal first event", "Reveal the first event of the span in the trace",
            () => { window.dispatchEvent(new CustomEvent("pw:reveal-event", {detail: {eventId: span.startEventId}}));},
            () => { window.dispatchEvent(new CustomEvent("pw:preview-event", {detail: {eventId: span.startEventId, on: true}}));},
            () => { window.dispatchEvent(new CustomEvent("pw:preview-event", {detail: {eventId: span.startEventId, on: false}}));}
        );

        const btnRevealEnd = this.#makeButton("Reveal last event", "Reveal the last event of the span in the trace",
            () => { window.dispatchEvent(new CustomEvent("pw:reveal-event", {detail: {eventId: span.endEventId}}));},
            () => { window.dispatchEvent(new CustomEvent("pw:preview-event", {detail: {eventId: span.endEventId, on: true}}));},
            () => { window.dispatchEvent(new CustomEvent("pw:preview-event", {detail: {eventId: span.endEventId, on: false}}));}
        );

        const btnFocus = this.#makeButton("Focus span", "Focus the trace view on this span", () => {
            window.dispatchEvent(new CustomEvent("pw:trace-filter", {
                detail: { filterId: span.id }
            }));
        });

        const btnClear = this.#makeButton("Clear focus", "Cancel the focus", () => {
            window.dispatchEvent(new CustomEvent("pw:trace-filter", {
                detail: { filterId: "spn:1" }
            }));
        });

        actions.appendChild(btnRevealStart);
        actions.appendChild(btnRevealEnd);
        actions.appendChild(btnFocus);
        actions.appendChild(btnClear);

        head.appendChild(h);
        head.appendChild(actions);

        const body = document.createElement("div");
        body.classList.add("spanInspectorDetailsBody");

        const parentSpan = span.parentSpanId ? this.traceModel.getSpan(span.parentSpanId) : null;
        const parentMethod = parentSpan ? this.traceModel.getMethod(parentSpan.methodId) : null;
        const parentClazz = parentMethod ? this.traceModel.getClass(parentMethod.classId) : null;
        const parentLabel = `${parentClazz ? parentClazz.name + "." : ""}${parentMethod ? parentMethod.name + "#" + parentSpan.id : "—"}`;

        body.appendChild(this.#kv("Parent span", parentLabel));
        const methodArgs = method.parameters.map(p => {
            return `${p.name}`;
        }).join(", ");
        body.appendChild(this.#kv("Method", method ? `${method.name}(${methodArgs})` : "—"));
        body.appendChild(this.#kv("Class", clazz ? `${clazz.packageName}.${clazz.name}` : "—"));

        body.appendChild(this.#kv("Start location", this.#formatLoc(span.startLoc)));
        body.appendChild(this.#kv("End location", this.#formatLoc(span.endLoc)));

        body.appendChild(this.#kv("Object this", span.thisRef ? this.#showObject(span.thisRef) : "—"));

        const argsDiv = document.createElement("div");
        argsDiv.classList.add("spanInspectorArgs");
        const argsTitle = document.createElement("div");
        argsTitle.classList.add("spanInspectorSectionTitle");
        argsTitle.textContent = "Args";
        argsDiv.appendChild(argsTitle);

        const args = Array.isArray(span.args) ? span.args : [];
        if (args.length === 0) {
            const no = document.createElement("div");
            no.classList.add("spanInspectorMuted");
            no.textContent = "—";
            argsDiv.appendChild(no);
        } else {
            for (const a of args) {
                const line = document.createElement("div");
                line.classList.add("spanInspectorArgLine");
                line.append(`${a.type} ${a.name} = `);
                line.append(this.#getShowValue(a.value, span.startEventId));
                argsDiv.appendChild(line);
            }
        }

        details.appendChild(head);
        details.appendChild(body);
        details.appendChild(argsDiv);
    }

    #makeButton(text, title, onClick, onHover = null, onLeave = null) {
        const btn = document.createElement("button");
        btn.type = "button";
        btn.classList.add("spanInspectorButton");
        btn.textContent = text;
        btn.title = title;
        btn.addEventListener("click", () => {
            onClick?.();
        });
        if (onHover) {
            btn.addEventListener("mouseenter", () => {
                onHover?.();
            });
        }
        if (onLeave) {
            btn.addEventListener("mouseleave", () => {
                onLeave?.();
            });
        }

        return btn;
    }

    #kv(k, v) {
        const row = document.createElement("div");
        row.classList.add("spanInspectorKV");

        const key = document.createElement("div");
        key.classList.add("spanInspectorKey");
        key.textContent = k;

        const val = document.createElement("div");
        val.classList.add("spanInspectorVal");
        val.append(v);

        row.appendChild(key);
        row.appendChild(val);

        return row;
    }

    #formatLoc(loc) {
        if (!loc) return "—";
        const src = this.traceModel?.mainData?.program?.sources?.find(s => s.id === loc.sourceId);
        const srcLabel = src?.path ? src.path.split("/").pop() : loc.sourceId;
        return `${srcLabel}:${loc.line}`;
    }

    #formatSpanLabel(span) {
        const method = this.traceModel.getMethod(span.methodId);
        const clazz = method ? this.traceModel.getClass(method.classId) : null;

        const loc = span.startLoc ? this.#formatLoc(span.startLoc) : "—";
        const sid = span.id;

        if (clazz && method) return `${clazz.name}.${method.name} — ${loc} — ${sid}`;
        if (method) return `${method.name} — ${loc} — ${sid}`;
        return `??? — ${loc} — ${sid}`;
    }

    #formatSpanMeta(span) {
        const num = s => Number(s.replace(/^eve:/, ""));
        const count = num(span.endEventId) - num(span.startEventId);

        return `${count} event(s)`;
    }

    #showObject(objId, contextEventId) {
        const documentFragment = document.createDocumentFragment();

        const traceSpanType = TraceSpanType.ArgsValue;

        const span = TraceSpan.createSpan(traceSpanType, objId);

        span.style.cursor = "pointer";
        span.title = `Inspect ${objId}`;
        span.addEventListener("click", (e) => {
            e.preventDefault?.();
            e.stopPropagation?.();
            window.dispatchEvent(new CustomEvent("pw:inspect-object", {
                detail: {objectId: objId, eventId: contextEventId}
            }))
        });

        documentFragment.append(span);

        return documentFragment;
    }

    #getShowValue(value, contextEventId = null) {
        const documentFragment = document.createDocumentFragment();

        if (value === null || value.kind === "NULL") {
            documentFragment.append("null");
        }
        else if (value.value !== null) {
            const isString = value.type.includes("String") || value.type.includes("string");
            documentFragment.append(isString ? "\"" + value.value + "\"" : value.value);
        }
        else {
            const traceSpanType = TraceSpanType.ArgsValue;
            const objectId = value.valueObjectId;

            const lineContent = objectId ? objectId.replace(":", "") : "?";
            const span = TraceSpan.createSpan(traceSpanType, lineContent);

            if (objectId) {
                span.style.cursor = "pointer";
                span.title = `Inspect ${objectId}`;
                span.addEventListener("click", (e) => {
                    e.preventDefault?.();
                    e.stopPropagation?.();
                    window.dispatchEvent(new CustomEvent("pw:inspect-object", {
                        detail: {objectId: objectId, eventId: contextEventId, objectVersion: value.objectVersion ?? null}
                    }))
                });
            }

            documentFragment.append(span);
        }

        return documentFragment;
    }
}
