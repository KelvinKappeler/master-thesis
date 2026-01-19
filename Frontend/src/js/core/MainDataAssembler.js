import {Preconditions} from "../utils/Preconditions.js";
import {Class, Enum, Interface, Method, ProgramTrace, Record, Source, Variable} from "../model/ProgramDefs.js";
import {BlockNode, CodePosition, ExprStmtNode, ForNode, IfNode, ReturnNode, WhileNode} from "../model/StructureDefs.js";
import {ExprCode} from "../model/ExprDefs.js";
import {Index} from "../model/IndexDefs.js";
import {Argument, Span, TraceData, TraceLocation} from "../model/TraceDefs.js";
import {
    ArithmeticTraceEvent, ArrayStoreTraceEvent,
    CallTraceEvent, ComparisonTraceEvent,
    ConditionTraceEvent, EventValue, FieldWriteTraceEvent,
    LocalTraceEvent, LoopIterationTraceEvent, LoopTraceEvent, NewTraceEvent,
    ReturnTraceEvent
} from "../model/EventDefs.js";
import {MainData} from "./MainData.js";
import {FieldState, ObjectTimeline, State, StateSnapshot} from "../model/StateDefs.js";

/**
 * This class is used to assemble a MainData object from JSON data.
 */
export class MainDataAssembler {

    /**
     * Assembles a MainData object from JSON data.
     * @param jsonData {JsonData} The JSON data to assemble the main data from.
     * @returns {Promise<MainData>} A promise that resolves to the assembled TraceData object.
     */
    static async assemble(jsonData) {
        Preconditions.requireNonNull(jsonData, "jsonData is null");

        const [programFile, traceFile, indexFile, stateFile] = await jsonData.getAllData();

        let program = this.#assembleProgramTrace(programFile);
        let index = this.#assembleIndex(indexFile);
        let traceData = this.#assembleTraceData(traceFile);
        let stateData = this.#assembleStateData(stateFile);

        return new MainData(program, traceData, index, stateData);
    }

    static #assembleIndex(indexJson) {
        let indexByLine = new Map();
        for (const [source, lineWithEvents] of Object.entries(indexJson.byLine)) {
            let lineMap = new Map();
            for (const [line, events] of Object.entries(lineWithEvents)) {
                lineMap.set(parseInt(line), events);
            }
            indexByLine.set(source, lineMap);
        }

        let indexByLocal = new Map();
        for (const [localId, events] of Object.entries(indexJson.byLocal)) {
            indexByLocal.set(localId, events);
        }

        let indexByObject = new Map();
        for (const [objectId, spans] of Object.entries(indexJson.byObject)) {
            indexByObject.set(objectId, spans);
        }

        let indexBySpan = new Map();
        for (const [spanId, objects] of Object.entries(indexJson.bySpan)) {
            indexBySpan.set(spanId, objects);
        }

        return new Index(indexByLine, indexByLocal, indexByObject, indexBySpan);
    }

    static #assembleProgramTrace(programJson) {
        let sources = this.#assembleSource(programJson);
        let classes = this.#assembleClasses(programJson);
        let interfaces = this.#assembleInterfaces(programJson);
        let records = this.#assembleRecords(programJson);
        let enums = this.#assembleEnums(programJson);
        let methods = this.#assembleMethods(programJson);

        return new ProgramTrace(sources, classes, interfaces, records, enums, methods);
    }

    static #assembleSource(programJson) {
        let sources = [];

        for (const source of programJson.sources) {
            sources.push(new Source(source.sourceId, source.path, source.language, source.lines, source.sourceContent));
        }

        return sources;
    }

    static #assembleClasses(programJson) {
        let classes = [];

        for (const cls of programJson.classes) {
            classes.push(new Class(cls.id, cls.name, cls.packageName, cls.sourceId));
        }

        return classes;
    }

    static #assembleInterfaces(programJson) {
        let interfaces = [];

        for (const intf of programJson.interfaces) {
            interfaces.push(new Interface(intf.id, intf.name, intf.packageName, intf.sourceId));
        }

        return interfaces;
    }

    static #assembleRecords(programJson) {
        let records = [];

        for (const rec of programJson.records) {
            records.push(new Record(rec.id, rec.name, rec.packageName, rec.sourceId));
        }

        return records;
    }

    static #assembleEnums(programJson) {
        let enums = [];

        for (const enm of programJson.enums) {
            enums.push(new Enum(enm.id, enm.name, enm.packageName, enm.sourceId));
        }

        return enums;
    }

    static #assembleMethods(programJson) {
        let methods = [];

        for (const method of programJson.methods) {
            methods.push(this.#assembleMethod(method));
        }

        return methods;
    }

    static #assembleMethod(methodJson) {
        return new Method(
            methodJson.methodId,
            methodJson.classId,
            methodJson.name,
            methodJson.returnType,
            methodJson.startLine,
            methodJson.endLine,
            this.#assembleVariables(methodJson.parameters),
            this.#assembleMethodStructure(methodJson.structure),
            this.#assembleVariables(methodJson.localVars)
        )
    }

    static #assembleVariables(variablesJson) {
        let parameters = [];

        for (const param of variablesJson) {
            parameters.push(new Variable(param.index, param.name, param.typeId));
        }

        return parameters;
    }

    static #assembleMethodStructure(structureJson) {
        if (structureJson === null) return null;

        let id = structureJson.structureId;
        let startPosition = this.#assemblePosition(structureJson.startPosition);
        let endPosition = this.#assemblePosition(structureJson.endPosition);

        switch (structureJson.kind) {
            case "BLOCK":
                let structures = this.#assembleMethodStructures(structureJson.structures);
                return new BlockNode(id, startPosition, endPosition, structures);

            case "EXPR_STMT":
                let expr = this.#assembleMethodStructureExpression(structureJson.expr);
                return new ExprStmtNode(id, expr, startPosition, endPosition);

            case "RETURN":
                return new ReturnNode(id, structureJson.code, startPosition, endPosition, structureJson.value);

            case "IF":
                return new IfNode(id, structureJson.code, startPosition, endPosition, this.#assembleMethodStructureExpression(structureJson.condition),
                    this.#assembleMethodStructure(structureJson.thenBranch), this.#assembleMethodStructure(structureJson.elseBranch));

            case "FOR":
                return new ForNode(id, structureJson.code, startPosition, endPosition,
                    this.#assembleMethodStructureExpression(structureJson.initialization),
                    this.#assembleMethodStructureExpression(structureJson.condition),
                    this.#assembleMethodStructureExpression(structureJson.update),
                    this.#assembleMethodStructure(structureJson.body)
                );

            case "WHILE":
                return new WhileNode(id, structureJson.code, startPosition, endPosition,
                    this.#assembleMethodStructureExpression(structureJson.condition),
                    this.#assembleMethodStructure(structureJson.body)
                );

            default:
                throw new Error(`Unknown structure kind: ${structureJson.kind}`);
        }
    }

    static #assembleMethodStructures(structuresJson) {
        let structures = [];

        for (const struct of structuresJson) {
            structures.push(this.#assembleMethodStructure(struct));
        }

        return structures;
    }

    static #assemblePosition(positionJson) {
        return new CodePosition(positionJson.line, positionJson.column);
    }

    static #assembleMethodStructureExpression(expressionJson) {
        let code = expressionJson.code;
        let startPosition = this.#assemblePosition(expressionJson.startPosition);
        let endPosition = this.#assemblePosition(expressionJson.endPosition);

        switch (expressionJson.kind) {
            case "ALL":
                return new ExprCode(code, startPosition, endPosition);

            default:
                throw new Error(`Unknown expression kind: ${expressionJson.kind}`);
        }
    }

    static #assembleStateData(stateFile) {
        let objects = new Map();
        for (const [objectId, objectState] of Object.entries(stateFile.objects)) {
            let timeline = [];
            for (const objectTimeline of objectState.timeline) {

                let fields = new Map();
                for (const [fieldName, fieldState] of Object.entries(objectTimeline.fields)) {
                    fields.set(fieldName, new FieldState(fieldState.type, fieldState.value, fieldState.objectId));
                }

                timeline.push(new StateSnapshot(objectTimeline.version, objectTimeline.eventId, fields));
            }

            objects.set(objectId, new ObjectTimeline(objectState.objectId, objectState.type, timeline));
        }

        return new State(objects);
    }

    static #assembleTraceData(traceJson) {
        let spans = this.#assembleSpans(traceJson.spans);
        let events = this.#assembleEvents(traceJson.events);

        return new TraceData(spans, events);
    }

    static #assembleTraceLocation(locationJson) {
        return new TraceLocation(locationJson.sourceId, locationJson.line);
    }

    static #assembleSpans(spansJson) {
        let spans = [];

        for (const span of spansJson) {
            spans.push(new Span(
                span.spanId,
                span.parentSpanId,
                span.methodId,
                span.startEventId,
                span.endEventId,
                this.#assembleTraceLocation(span.startLoc),
                this.#assembleTraceLocation(span.endLoc),
                span.thisRef,
                span.args.map(arg => new Argument(arg.name, arg.value, arg.type))
            ));
        }

        return spans;
    }

    static #assembleEvents(eventsJson) {
        let events = [];

        for (const event of eventsJson) {
            events.push(this.#assembleEvent(event));
        }

        return events;
    }

    static #assembleEventValue(eventValueJson) {
        if (eventValueJson === null || eventValueJson === undefined) {
            return null;
        }

        return new EventValue(
            eventValueJson.value,
            eventValueJson.valueObjectId,
            eventValueJson.type,
            eventValueJson.kind,
            eventValueJson.objectVersion
        );
    }

    static #assembleEvent(eventJson) {
        let eventId = eventJson.eventId;
        let spanId = eventJson.spanId;
        let frameId = eventJson.frameId;
        let location = this.#assembleTraceLocation(eventJson.location);

        switch (eventJson.type) {
            case "CALL":
                return new CallTraceEvent(
                    eventId, spanId, location,
                    eventJson.callerMethodId,
                    eventJson.calleeMethodId,
                    eventJson.name,
                    eventJson.external,
                    eventJson.args,
                    this.#assembleEventValue(eventJson.value),
                    eventJson.bodyEventIds
                );

            case "LOCAL":
                return new LocalTraceEvent(
                    eventId, spanId, location,
                    eventJson.method,
                    eventJson.varName,
                    eventJson.value,
                    eventJson.label,
                    eventJson.bodyEventId
                );

            case "RETURN":
                return new ReturnTraceEvent(
                    eventId, spanId, location,
                    this.#assembleEventValue(eventJson.value),
                    eventJson.bodyEventId
                );

            case "ARITHMETIC":
                return new ArithmeticTraceEvent(
                    eventId, spanId, location,
                    eventJson.operator,
                    this.#assembleEventValue(eventJson.left),
                    eventJson.leftEventId,
                    this.#assembleEventValue(eventJson.right),
                    eventJson.rightEventId,
                    this.#assembleEventValue(eventJson.value)
                )

            case "CONDITION":
                return new ConditionTraceEvent(
                    eventId, spanId, location,
                    eventJson.kind,
                    eventJson.conditionEventIds,
                    eventJson.thenEventIds,
                    eventJson.elseEventIds,
                    this.#assembleEventValue(eventJson.value)
                );

            case "COMPARISON":
                return new ComparisonTraceEvent(
                    eventId, spanId, location,
                    eventJson.operator,
                    this.#assembleEventValue(eventJson.left),
                    eventJson.leftEventId,
                    this.#assembleEventValue(eventJson.right),
                    eventJson.rightEventId,
                    this.#assembleEventValue(eventJson.result)
                )

            case "ARRAYSTORE":
                return new ArrayStoreTraceEvent(
                    eventId, spanId, location,
                    eventJson.arrayVarName,
                    eventJson.arrayObjectId,
                    eventJson.index,
                    this.#assembleEventValue(eventJson.value),
                    eventJson.label,
                    eventJson.bodyEventId
                )

            case "PUTFIELD":
                return new FieldWriteTraceEvent(
                    eventId, spanId, location,
                    eventJson.objectId,
                    eventJson.fieldName,
                    this.#assembleEventValue(eventJson.value),
                    eventJson.fieldType,
                    eventJson.bodyEventId
                )

            case "LOOP":
                return new LoopTraceEvent(
                    eventId, spanId, location,
                    eventJson.loopKind,
                    eventJson.initEventIds,
                    eventJson.iterationsEventIds
                )

            case "LOOP_ITERATION":
                return new LoopIterationTraceEvent(
                    eventId, spanId, location,
                    eventJson.iterationIndex,
                    this.#assembleEventValue(eventJson.value),
                    eventJson.conditionEventIds,
                    eventJson.bodyEventIds,
                    eventJson.updateEventIds
                )

            case "NEW":
                return new NewTraceEvent(
                    eventId, spanId, location,
                    eventJson.objectId,
                    eventJson.typeName
                )

            default:
                throw new Error(`Unknown event type: ${eventJson.eventType}`);
        }
    }
}
