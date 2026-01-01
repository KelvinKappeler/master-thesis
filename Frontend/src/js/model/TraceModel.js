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
        this.classesById = new Map();

        this.objectsById = new Map();

        this.#createCacheEventsById();
        this.#createCacheSpansById();
        this.#createCacheMethodsById();
        this.#createCacheClassesById();
        this.#createObjectsById();
    }

    /**
     * Returns the AST (StructureNode) for a given event.
     * @param {string} eventId - The ID of the event.
     * @param {boolean} isExpression - Whether to look for an expression node.
     * @returns {StructureNode} The AST for the given event.
     */
    getStructureFromEvent(eventId, isExpression = false) {
        const ev = this.eventsById.get(eventId);
        if (!ev) return null;

        const span = this.spansById.get(ev.spanId);
        if (!span) return null;

        const method = this.methodsById.get(span.methodId);
        if (!method) return null;

        return method.structures.visitForLine(ev.location.line, isExpression);
    }

    /**
     * Returns the method for a given ID.
     * @param id {string} - The ID of the method.
     * @returns {Method} - The method for the given ID.
     */
    getMethod(id) {
        return this.methodsById.get(id);
    }

    /**
     * Returns the event for a given ID.
     * @param id {string} - The ID of the event.
     * @returns {TraceEvent} - The event for the given ID.
     */
    getEvent(id) {
        return this.eventsById.get(id);
    }

    /**
     * Returns the class for a given ID.
     * @param id {string} - The ID of the class.
     * @returns {Class} - The class for the given ID.
     */
    getClass(id) {
        return this.classesById.get(id);
    }

    /**
     * Returns the span for a given ID.
     * @param id {string} - The ID of the span.
     * @returns {Span} - The span for the given ID.
     */
    getSpan(id) {
        return this.spansById.get(id);
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

    #createCacheClassesById() {
        for (const clazz of this.mainData.program.classes || []) {
            this.classesById.set(clazz.id, clazz);
        }
    }

    #createCacheSpansById() {
        for (const span of this.mainData.trace.spans || []) {
            this.spansById.set(span.id, span);
        }
    }

    #createObjectsById() {
        for (const obj of this.mainData.state.objects.values()) {
            this.objectsById.set(obj.objectId, obj);
        }
    }
}
