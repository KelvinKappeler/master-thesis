/**
 * Represents the state of the objects with their different properties and versions.
 */
export class State {
    constructor(objects) {
        this.objects = objects;
    }
}

/**
 * Represents the state of a specific field within an object.
 */
export class FieldState {
    constructor(type, value, objectId) {
        this.type = type;
        this.value = value;
        this.objectId = objectId;
    }
}

/**
 * Represents a timeline of a versioned object.
 */
export class ObjectTimeline {
    constructor(objectId, type, timeline) {
        this.objectId = objectId;
        this.type = type;
        this.timeline = timeline;
    }
}

/**
 * Represents a snapshot of the state for a versioned object.
 */
export class StateSnapshot {
    constructor(version, eventId, fields) {
        this.version = version;
        this.eventId = eventId;
        this.fields = fields;
    }
}
