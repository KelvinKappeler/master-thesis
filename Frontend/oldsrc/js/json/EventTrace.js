export class Trace {
    constructor(trace) {
        this.trace = trace.trace.map(entry => {
            if (entry.type === 'GroupEvent') {
                return new Event(entry);
            } else {
                return new Step(entry);
            }
        });
    }
}

export class Event {
    constructor(event) {
        this.eventId = event.eventId;
        this.eventType = event.eventType;
        this.type = event.type;
        this.pos = event.pos;
        this.kind = event.kind ? new Kind(event.kind) : undefined;
    }
}

class Kind {
    constructor(kind) {
        this.type = kind.type;
        this.functionName = kind.functionName ? kind.functionName.trimEnd() : "";
    }
}

export class Step {
    constructor(step) {
        this.stepId = step.stepId;
        this.nodeKey = step.nodeKey;
        this.argsValues = step.argsValues ? step.argsValues.map(arg => ArgValue.newArg(arg)) : [];
        this.type = step.type;
        this.kind = step.kind;
        this.result = step.result ? new Result(step.result) : undefined;
        this.assigns = step.assigns ? step.assigns.map(assign => new Assigns(assign)) : undefined;
    }
}

class ArgValue {
    constructor(arg) {
        this.dataType = arg.dataType;
    }

    static newArg(argument) {
        if (argument.dataType === "instanceRef") {
            return new ObjectArgValue(argument);
        } else {
            return new PrimitiveArgValue(argument);
        }
    }
}

class PrimitiveArgValue extends ArgValue {
    constructor(arg) {
        super(arg);
        this.value = arg.value;
    }
}

class ObjectArgValue extends ArgValue {
    constructor(arg) {
        super(arg);
        this.pointer = arg.pointer;
        this.className = new ClassName(arg.className);
        this.version = arg.version;
    }
}

class Result {
    constructor(result) {
        this.pointer = result.pointer;
        this.className = result.className ? new ClassName(result.className) : undefined;
        this.version = result.version;
        this.dataType = result.dataType;
        this.value = result.value;
    }
}

class Assigns {
    constructor(assign) {
        this.value = assign.value ? new Result(assign.value) : undefined;
        this.identifier = assign.identifier ? new Identifier(assign.identifier) : undefined;
        this.dataType = assign.dataType;
    }
}

class ClassName {
    constructor(className) {
        this.className = className.className;
        this.packageName = className.packageName;
    }
}

class Identifier {
    constructor(identifier) {
        this.name = identifier.name;
        this.parent = identifier.parent;
        this.dataType = identifier.dataType;
    }
}
