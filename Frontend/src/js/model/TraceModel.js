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
        this.localVarsByIdByMethodId = new Map();

        this.#createCacheEventsById();
        this.#createCacheSpansById();
        this.#createCacheMethodsById();
        this.#createCacheClassesById();
        this.#createCacheLocalVarsByIdByMethodId();
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

    /**
     * Returns the method for a given ID.
     * @param id {string} - The ID of the method.
     * @returns {Method} - The method for the given ID.
     */
    getMethod(id) {
        return this.methodsById.get(id);
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

    /**
     * Returns the local variable for a given method and index.
     * @param methodId {string} - The ID of the method.
     * @param index {number} - The index of the local variable.
     * @returns {Variable} - The local variable for the given method and index.
     */
    getLocalVar(methodId, index) {
        return this.localVarsByIdByMethodId.get(methodId).get(index);
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

    #createCacheLocalVarsByIdByMethodId() {
        for (const method of this.mainData.program.methods || []) {
            const localVarsById = new Map();
            for (const localVar of method.localVars) {
                localVarsById.set(localVar.index, localVar);
            }
            this.localVarsByIdByMethodId.set(method.id, localVarsById);
        }
    }
}
