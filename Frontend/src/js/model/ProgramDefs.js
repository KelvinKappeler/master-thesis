/**
 * Represents the source code structure of a program.
 */
export class ProgramTrace {
    constructor(sources, classes, interfaces, records, enums, methods) {
        this.sources = sources;
        this.classes = classes;
        this.interfaces = interfaces;
        this.records = records;
        this.enums = enums;
        this.methods = methods;
    }
}

/**
 * Represents a source file in the trace data.
 */
export class Source {
    constructor(sourceId, path, language, lines, sourceContent) {
        this.id = sourceId;
        this.path = path;
        this.language = language;
        this.lines = lines;
        this.sourceContent = sourceContent;
    }
}

/**
 * Represents a class in the trace data.
 */
export class Class {
    constructor(classId, name, packageName, sourceId) {
        this.id = classId;
        this.name = name;
        this.packageName = packageName;
        this.sourceId = sourceId;
    }
}

/**
 * Represents an interface in the trace data.
 */
export class Interface {
    constructor(id, name, packageName, sourceId) {
        this.id = id;
        this.name = name;
        this.packageName = packageName;
        this.sourceId = sourceId;
    }
}

/**
 * Represents a record in the trace data.
 */
export class Record {
    constructor(id, name, packageName, sourceId) {
        this.id = id;
        this.name = name;
        this.packageName = packageName;
        this.sourceId = sourceId;
    }
}

/**
 * Represents an enum in the trace data.
 */
export class Enum {
    constructor(id, name, packageName, sourceId) {
        this.id = id;
        this.name = name;
        this.packageName = packageName;
        this.sourceId = sourceId;
    }
}

/**
 * Represents a method in the trace data.
 */
export class Method {
    constructor(methodId, classId, name, returnType, startLine, endLine, parameters, structures, localVars) {
        this.id = methodId;
        this.classId = classId;
        this.name = name;
        this.returnType = returnType;
        this.startLine = startLine;
        this.endLine = endLine;
        this.parameters = parameters;
        this.structures = structures;
        this.localVars = localVars;
    }
}

/**
 * Represents a variable in the trace data. Used for method parameters and local variables.
 */
export class Variable {
    constructor(index, name, typeId) {
        this.index = index;
        this.name = name;
        this.typeId = typeId;
    }
}
