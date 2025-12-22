import {PWElement} from "./PWElement.js";

/**
 * This class is used to manage a breadcrumb list, to show the current filter
 */
export class Breadcrumb extends PWElement {
    constructor(traceModel) {
        super(document.createElement("ul"));
        this.element.classList.add("breadcrumb");
        this.traceModel = traceModel;
        this.state = { eventId: null, location: null };
    }

    setContext(ctx) {
        this.state.eventId = ctx.eventId ?? null;
        this.state.location = ctx.location ?? null;
        this.render();
    }

    clear() {
        this.setContext({ eventId: null, location: null });
    }

    render() {
        const segments = this.#computeSegments(this.state.eventId, this.state.location);
        this.#renderSegments(segments);
    }

    #renderSegments(segments) {
        this.element.innerHTML = "";
        for (const seg of segments) {
            const li = document.createElement("li");
            li.textContent = seg.label;
            this.element.append(li);
        }
    }

    #computeSegments(eventId, location) {
        if (!location) return [{ label: "" }];

        const segments = [];

        const ev = this.traceModel.getEvent(eventId);
        const span = ev ? this.traceModel.getSpan(ev.spanId) : null;
        const method = span ? this.traceModel.getMethod(span.methodId) : null;
        const clazz = method ? this.traceModel.getClass(method.classId) : null;

        if (clazz && method) segments.push({ label: `${clazz.name}.${method.name}:${location.line}` });
        else if (method) segments.push({ label: `${method.name}()` });

        return segments;
    }
}
