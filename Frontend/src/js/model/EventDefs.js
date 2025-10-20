/**
 * Represents the base class for all trace events.
 */
export class TraceEvent {
    constructor(eventId, spanId, frameId, location) {
        this.eventId = eventId;
        this.spanId = spanId;
        this.frameId = frameId;
        this.location = location;
    }
}

/**
 * Represents an event where a method is called.
 */
export class CallTraceEvent extends TraceEvent {
    constructor(eventId, spanId, frameId, location, callerMethodId, calleeMethodId, name) {
        super(eventId, spanId, frameId, location);

        this.name = name;
        this.callerMethodId = callerMethodId;
        this.calleeMethodId = calleeMethodId;
    }
}

/**
 * Represents an event where a local variable is assigned a value.
 */
export class LocalTraceEvent extends TraceEvent {
    constructor(eventId, spanId, frameId, location, owner, methodId, index, value) {
        super(eventId, spanId, frameId, location);

        this.owner = owner;
        this.methodId = methodId;
        this.index = index;
        this.value = value;
    }
}

/**
 * Represents an event where a method returns a value.
 */
export class ReturnTraceEvent extends TraceEvent {
    constructor(eventId, spanId, frameId, location, returnValue) {
        super(eventId, spanId, frameId, location);

        this.returnValue = returnValue;
    }
}

/**
 * Represents an event where an arithmetic operation is performed.
 */
export class ArithmeticTraceEvent extends TraceEvent {
    constructor(eventId, spanId, frameId, location, operation, resultType, left, right, result) {
        super(eventId, spanId, frameId, location);

        this.operation = operation;
        this.resultType = resultType;
        this.left = left;
        this.right = right;
        this.result = result;
    }
}
