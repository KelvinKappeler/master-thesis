/**
 * Represents the trace model for managing events. Core of the tracing debugger.
 * @param {MainData} mainData
 */
export class TraceModel {

    constructor(mainData) {
        this.mainData = mainData;

        this.eventsById = new Map();
        this.spansById = new Map();
        this.methodsById = new Map();

        this.#createCacheEventsById();
        this.#createCacheSpansById();
        this.#createCacheMethodsById();
    }

    /**
     * Returns all events for a given span.
     * @param spanId {string} - The ID of the span.
     * @returns {TraceEvent[]} - An array of events for the given span.
     */
    getEventsFromSpan(spanId) {
        let eventIds = this.mainData.index.bySpan.get(spanId);
        if (!Array.isArray(eventIds) || eventIds.length === 0) return [];

        return eventIds.map(id => this.eventsById.get(id)).filter(Boolean);
    }

    /**
     * Returns the AST (StructureNode) for a given event.
     * @param {string} eventId - The ID of the event.
     * @returns {StructureNode} The AST for the given event.
     */
    getAstFromEvent(eventId) {
        const ev = this.eventsById.get(eventId);
        if (!ev) return null;

        const span = this.spansById.get(ev.spanId);
        if (!span) return null;

        const method = this.methodsById.get(span.methodId);
        if (!method) return null;

        return method.structures.visitForLine(ev.location.line);
    }

    #createCacheEventsById() {
        for (const ev of this.mainData.trace.events || []) {
            this.eventsById.set(ev.eventId, ev);
        }
    }

    #createCacheMethodsById() {
        for (const method of this.mainData.program.methods || []) {
            this.methodsById.set(method.id, method);
        }
    }

    #createCacheSpansById() {
        for (const span of this.mainData.trace.spans || []) {
            this.spansById.set(span.id, span);
        }
    }
}
