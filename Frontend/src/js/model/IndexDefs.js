/**
 * Represents an index to speed up lookups for events in the trace.
 * @param {Object} byLine - Maps source with line numbers to arrays of event IDs.
 * @param {Object} byObject - Maps object IDs to arrays of event IDs.
 * @param {Object} bySpan - Maps span IDs to arrays of event IDs.
 */
export class Index {
    constructor(byLine, byObject, bySpan) {
        this.byLine = byLine;
        this.byObject = byObject;
        this.bySpan = bySpan;
    }
}
