/**
 * Represents the view model for the trace part of the application.
 */
export class TraceViewModel {
    constructor(traceModel) {
        this.traceModel = traceModel;

        // UI State
        this.filterType = "none";
        this.filterId = "";
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
