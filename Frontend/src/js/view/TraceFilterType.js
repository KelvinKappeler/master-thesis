/**
 * Represents the type of filter for the trace.
 */
export class TraceFilterType {

    static NONE = new TraceFilterType("none");
    static OBJECT = new TraceFilterType("object");
    static SPAN = new TraceFilterType("span");
    static SOURCE = new TraceFilterType("source");

    constructor(name) {
        this.name = name;
    }

    toString() {
        return `TraceFilter.${this.name}`;
    }
}