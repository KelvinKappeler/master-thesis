/**
 * Represents the definition of a value in an event.
 */
export class EventValue {
    constructor(value, valueObjectId, type, kind) {
        this.value = value;
        this.valueObjectId = valueObjectId;
        this.type = type;
        this.kind = kind;
    }
}

/**
 * Represents the base class for all trace events.
 */
export class TraceEvent {
    constructor(eventId, spanId, location) {
        this.eventId = eventId;
        this.spanId = spanId;
        this.location = location;
    }
}

/**
 * Represents an event where a method is called.
 */
export class CallTraceEvent extends TraceEvent {
    constructor(eventId, spanId, location, callerMethodId, calleeMethodId, name, external, args, value, bodyEventIds) {
        super(eventId, spanId, location);

        this.name = name;
        this.callerMethodId = callerMethodId;
        this.calleeMethodId = calleeMethodId;
        this.external = external;
        this.args = args;
        this.value = value;
        this.bodyEventIds = bodyEventIds;
    }
}

/**
 * Represents an event where a local variable is assigned a value.
 */
export class LocalTraceEvent extends TraceEvent {
    constructor(eventId, spanId, location, method, varName, value, label, bodyEventId) {
        super(eventId, spanId, location);

        this.method = method;
        this.varName = varName;
        this.value = value;
        this.label = label;
        this.bodyEventId = bodyEventId;
    }
}

/**
 * Represents an event where a method returns a value.
 */
export class ReturnTraceEvent extends TraceEvent {
    constructor(eventId, spanId, location, value, bodyEventId) {
        super(eventId, spanId, location);

        this.value = value;
        this.bodyEventId = bodyEventId;
    }
}

/**
 * Represents an event where an arithmetic operation is performed.
 */
export class ArithmeticTraceEvent extends TraceEvent {
    constructor(eventId, spanId, location, operator, left, leftEventId, right, rightEventId, value) {
        super(eventId, spanId, location);

        this.operator = operator;
        this.left = left;
        this.leftEventId = leftEventId;
        this.right = right;
        this.rightEventId = rightEventId;
        this.value = value;
    }
}

/**
 * Represents an event where a value is stored in an array.
 */
export class ArrayStoreTraceEvent extends TraceEvent {
    constructor (eventId, spanId, location, arrayVarName, arrayObjectId, index, value, label, bodyEventId) {
        super(eventId, spanId, location);

        this.arrayVarName = arrayVarName;
        this.arrayObjectId = arrayObjectId;
        this.index = index;
        this.value = value;
        this.label = label;
        this.bodyEventId = bodyEventId;
    }
}

/**
 * Represents an event where a condition is evaluated.
 */
export class ConditionTraceEvent extends TraceEvent {
    constructor(eventId, spanId, location, kind, conditionEventIds, thenEventIds, elseEventIds, value) {
        super(eventId, spanId, location);

        this.kind = kind;
        this.conditionEventIds = conditionEventIds;
        this.thenEventIds = thenEventIds;
        this.elseEventIds = elseEventIds;
        this.value = value;
    }
}

/**
 * Represents an event where a comparison operation is performed.
 */
export class ComparisonTraceEvent extends TraceEvent {
    constructor(eventId, spanId, location, operator, left, leftEventId, right, rightEventId, result) {
        super(eventId, spanId, location);

        this.operator = operator;
        this.left = left;
        this.leftEventId = leftEventId;
        this.right = right;
        this.rightEventId = rightEventId;
        this.result = result;
    }
}

/**
 * Represents an event where a field of an object is written to.
 */
export class FieldWriteTraceEvent extends TraceEvent {
    constructor(eventId, spanId, location, objectId, fieldName, value, fieldType, bodyEventId) {
        super(eventId, spanId, location);

        this.objectId = objectId;
        this.fieldName = fieldName;
        this.value = value;
        this.fieldType = fieldType;
        this.bodyEventId = bodyEventId;
    }
}

/**
 * Represents an event where a loop is executed.
 */
export class LoopTraceEvent extends TraceEvent {
    constructor(eventId, spanId, location, loopKind, initEventIds, iterationsEventIds) {
        super(eventId, spanId, location);

        this.loopKind = loopKind;
        this.initEventIds = initEventIds;
        this.iterationsEventIds = iterationsEventIds;
    }
}

/**
 * Represents an event for a single iteration of a loop.
 */
export class LoopIterationTraceEvent extends TraceEvent {
    constructor(eventId, spanId, location, iterationIndex, conditionEventIds, bodyEventIds, updateEventIds) {
        super(eventId, spanId, location);

        this.iterationIndex = iterationIndex;
        this.conditionEventIds = conditionEventIds;
        this.bodyEventIds = bodyEventIds;
        this.updateEventIds = updateEventIds;
    }
}

/**
 * Represents an event where a new object is created.
 */
export class NewTraceEvent extends TraceEvent {
    constructor(eventId, spanId, location, objectId, typeName) {
        super(eventId, spanId, location);

        this.objectId = objectId;
        this.typeName = typeName;
    }
}
