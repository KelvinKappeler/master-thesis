import {TraceFilterType} from "./TraceFilterType.js";

/**
 * Represents the view model for the trace part of the application.
 */
export class TraceViewModel {
    constructor(traceModel) {
        this.traceModel = traceModel;

        this.filterType = TraceFilterType.SPAN;
        this.filterId = "spn:1";
    }

    /**
     * Returns the events based on the filter type and ID.
     * @returns {TraceEvent[]} An array of trace events.
     */
    getEvents() {
        switch (this.filterType.name) {
            case "span":
                return this.traceModel.getEventsFromSpan(this.filterId);
            default:
                throw new Error("Filter type not implemented");
        }
    }

    /**
     * Returns the event with the given ID and its corresponding structure node.
     * @param id {string} - The ID of the event.
     * @returns {[TraceEvent, StructureNode]} The event with the given ID and its corresponding structure node.
     */
    getEvent(id) {
        const event = this.traceModel.getEvent(id);
        return [event, this.traceModel.getAstFromEvent(event.eventId)]
    }

    /**
     * Returns the method with the given ID.
     * @param id {string} The ID of the method.
     * @returns {Method} The method with the given ID.
     */
    getMethod(id) {
        return this.traceModel.getMethod(id);
    }

    /**
     * Returns the class with the given ID.
     * @param id {string} The ID of the class.
     * @returns {Class} The class with the given ID.
     */
    getClass(id) {
        return this.traceModel.getClass(id);
    }

    /**
     * Gets the current method based on the filter type and ID.
     * @returns {Method} The current method.
     */
    getCurrentMethod() {
        switch (this.filterType.name) {
            case "span":
                const span = this.traceModel.getSpan(this.filterId);
                return this.traceModel.getMethod(span.methodId);
            default:
                throw new Error("Filter type not implemented");
        }
    }

    /**
     * Returns the local variable with the given method ID and index.
     * @param methodId {string} The ID of the method.
     * @param index {number} The index of the local variable.
     * @returns {Variable} The variable with the given method ID and index.
     */
    getLocalVar(methodId, index) {
        return this.traceModel.getLocalVar(methodId, index);
    }

    /**
     * Gets a map of events to their corresponding structure nodes.
     * @returns {Map<TraceEvent, StructureNode>} A map of events to structure nodes.
     */
    getEventsProgramStructureMap() {
        const map = new Map();

        for (const event of this.getEvents()) {
            map.set(event, this.traceModel.getAstFromEvent(event.eventId));
        }

        return map;
    }

    /**
     * Sets the filter type and ID.
     * @param type {TraceFilterType} The type of filter to apply.
     * @param id {string} The ID of the filter to apply.
     */
    setFilter(type, id) {
        this.filterType = type;
        this.filterId = id;
    }
}
