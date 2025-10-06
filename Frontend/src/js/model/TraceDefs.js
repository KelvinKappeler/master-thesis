/**
 * Represents the data contained in the trace file.
 */
export class TraceData {
    constructor(spans, frames, events) {
        this.spans = spans;
        this.frames = frames;
        this.events = events;
    }
}

/**
 * Represents a span in the trace data.
 */
export class Span {
    constructor(spanId, parentSpanId, methodId, startEventId, endEventId, startLoc, endLoc) {
        this.id = spanId;
        this.parentSpanId = parentSpanId;
        this.methodId = methodId;
        this.startEventId = startEventId;
        this.endEventId = endEventId;
        this.startLoc = startLoc;
        this.endLoc = endLoc;
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
}

/**
 * Represents a frame in the trace data.
 */
export class Frame {
    constructor(frameId, spanId, methodId, thisRef, args) {
        this.id = frameId;
        this.spanId = spanId;
        this.methodId = methodId;
        this.thisRef = thisRef;
        this.args = args;
    }
}

/**
 * Represents an argument in the trace data for a frame.
 */
export class Argument {
    constructor(name, value) {
        this.name = name;
        this.value = value;
    }
}
