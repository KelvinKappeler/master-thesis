/**
 * Represents an index to speed up lookups for events in the trace.
 * @param {Object} byLine - Maps source with line numbers to arrays of event IDs.
 * @param {Object} byLocal - Maps method IDs to arrays of event IDs for local variables.
 * @param {Object} byObject - Maps object IDs to arrays of event IDs.
 * @param {Object} bySpan - Maps span IDs to arrays of event IDs.
 */
export class Index {
    constructor(byLine, byLocal, byObject, bySpan) {
        this.byLine = byLine;
        this.byLocal = byLocal;
        this.byObject = byObject;
        this.bySpan = bySpan;
    }
}
