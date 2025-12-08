/**
 * Represents the data contained in the trace file.
 */
export class TraceData {
    constructor(spans, events) {
        this.spans = spans;
        this.events = events;
    }
}

/**
 * Represents a span in the trace data.
 */
export class Span {
    constructor(spanId, parentSpanId, methodId, startEventId, endEventId, startLoc, endLoc, thisRef, args) {
        this.id = spanId;
        this.parentSpanId = parentSpanId;
        this.methodId = methodId;
        this.startEventId = startEventId;
        this.endEventId = endEventId;
        this.startLoc = startLoc;
        this.endLoc = endLoc;
        this.thisRef = thisRef;
        this.args = args;
    }
}

/**
 * Represents a location in the trace data.
 */
export class TraceLocation {
    constructor(sourceId, line) {
        this.sourceId = sourceId;
        this.line = line;
    }

    /**
     * Checks if this location is equal to another location.
     * @param other {TraceLocation} The other location to compare with.
     * @returns {boolean} True if the locations are equal, false otherwise.
     */
    equals(other) {
        if (other === null || other === undefined) return false;

        return this.sourceId === other.sourceId && this.line === other.line;
    }
}

/**
 * Represents an argument in the trace data for a span.
 */
export class Argument {
    constructor(name, value, type) {
        this.name = name;
        this.value = value;
        this.type = type;
    }
}
