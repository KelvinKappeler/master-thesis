import {PWElement} from "./PWElement.js";
import {TextFilterBox} from "./TextFilterBox.js";
import {TraceFilterType} from "../view/TraceFilterType.js";

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

        const resolvedSpanId = String(span.id);

        const method = this.traceModel.getMethod(span.methodId);
        const clazz = method ? this.traceModel.getClass(method.classId) : null;

        const eventsInSpan = this.traceModel?.mainData?.index?.bySpan?.get(span.id)
            ?? this.traceModel?.mainData?.index?.bySpan?.get(resolvedSpanId)
            ?? [];
        const eventCount = Array.isArray(eventsInSpan) ? eventsInSpan.length : 0;

        details.innerHTML = "";

        const head = document.createElement("div");
        head.classList.add("spanInspectorDetailsHead");

        const h = document.createElement("div");
        h.classList.add("spanInspectorDetailsTitle");
        h.textContent = this.#formatSpanLabel(resolvedSpanId);

        const actions = document.createElement("div");
        actions.classList.add("spanInspectorActions");

        const btnRevealStart = this.#makeButton("Reveal start", "Révéler startEvent dans la trace", () => {
            if (span.startEventId != null) {
                window.dispatchEvent(new CustomEvent("pw:reveal-event", {detail: {eventId: span.startEventId}}));
            }
        });

        const btnRevealEnd = this.#makeButton("Reveal end", "Révéler endEvent dans la trace", () => {
            if (span.endEventId != null) {
                window.dispatchEvent(new CustomEvent("pw:reveal-event", {detail: {eventId: span.endEventId}}));
            }
        });

        const btnFocus = this.#makeButton("Focus span", "N'afficher que ce span (nécessite branchement filtre)", () => {
            window.dispatchEvent(new CustomEvent("pw:trace-filter", {
                detail: { filterType: TraceFilterType.SPAN, filterId: resolvedSpanId }
            }));
        });

        const btnClear = this.#makeButton("Clear focus", "Annuler le focus", () => {
            window.dispatchEvent(new CustomEvent("pw:trace-filter", {
                detail: { filterType: TraceFilterType.NONE, filterId: null }
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

        body.appendChild(this.#kv("Span ID", span.id));
        body.appendChild(this.#kv("Parent span", span.parentSpanId ?? "—"));
        body.appendChild(this.#kv("Method ID", span.methodId ?? "—"));
        body.appendChild(this.#kv("Method", method ? `${method.name}()` : "—"));
        body.appendChild(this.#kv("Class", clazz ? `${clazz.packageName}.${clazz.name}` : "—"));

        body.appendChild(this.#kv("Start event", span.startEventId ?? "—"));
        body.appendChild(this.#kv("End event", span.endEventId ?? "—"));

        body.appendChild(this.#kv("Start loc", this.#formatLoc(span.startLoc)));
        body.appendChild(this.#kv("End loc", this.#formatLoc(span.endLoc)));

        body.appendChild(this.#kv("Events in span", String(eventCount)));

        body.appendChild(this.#kv("thisRef", span.thisRef ?? "—"));

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
                line.textContent = `${a.type} ${a.name} = ${a.value}`;
                argsDiv.appendChild(line);
            }
        }

        details.appendChild(head);
        details.appendChild(body);
        details.appendChild(argsDiv);
    }

    #makeButton(text, title, onClick) {
        const btn = document.createElement("button");
        btn.type = "button";
        btn.classList.add("spanInspectorButton");
        btn.textContent = text;
        btn.title = title;
        btn.addEventListener("click", (e) => {
            e.preventDefault();
            e.stopPropagation();
            onClick?.();
        });
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
        val.textContent = String(v);

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
        const sid = String(span.id);

        if (clazz && method) return `${clazz.name}.${method.name} — ${sid} — ${loc}`;
        if (method) return `${method.name} — ${sid} — ${loc}`;
        return `??? — ${sid} — ${loc}`;
    }

    #formatSpanMeta(span) {
        const eventIds = this.traceModel?.mainData?.index?.bySpan?.get(span.id) ?? this.traceModel?.mainData?.index?.bySpan?.get(String(span.id)) ?? [];
        const count = Array.isArray(eventIds) ? eventIds.length : 0;

        const start = span.startEventId ?? "—";
        const end = span.endEventId ?? "—";

        return `${count} event(s) • ${start} → ${end}`;
    }
}
