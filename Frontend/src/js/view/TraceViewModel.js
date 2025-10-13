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
