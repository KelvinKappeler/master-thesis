import {Preconditions} from "../utils/Preconditions.js";
import {Class, Enum, Interface, Method, ProgramTrace, Record, Source, Variable} from "../model/ProgramDefs.js";
import {BlockNode, CodePosition, ExprStmtNode, ReturnNode} from "../model/StructureDefs.js";
import {ExprCode} from "../model/ExprDefs.js";
import {Index} from "../model/IndexDefs.js";
import {Argument, Frame, Span, TraceData, TraceLocation} from "../model/TraceDefs.js";
import {CallTraceEvent, LocalTraceEvent, ReturnTraceEvent} from "../model/EventDefs.js";
import {MainData} from "./MainData.js";

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

        const [programFile, traceFile, indexFile] = await jsonData.getAllData();

        let program = this.#assembleProgramTrace(programFile);
        let index = this.#assembleIndex(indexFile);
        let traceData = this.#assembleTraceData(traceFile);

        return new MainData(program, traceData, index);
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

        let indexByObject = new Map();
        for (const [objectId, spans] of Object.entries(indexJson.byObject)) {
            indexByObject.set(objectId, spans);
        }

        let indexBySpan = new Map();
        for (const [spanId, objects] of Object.entries(indexJson.bySpan)) {
            indexBySpan.set(spanId, objects);
        }

        return new Index(indexByLine, indexByObject, indexBySpan);
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
        let id = structureJson.id;
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

    static #assembleTraceData(traceJson) {
        let spans = this.#assembleSpans(traceJson.spans);
        let frames = this.#assembleFrames(traceJson.frames);
        let events = this.#assembleEvents(traceJson.events);

        return new TraceData(spans, frames, events);
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
                this.#assembleTraceLocation(span.endLoc)
            ));
        }

        return spans;
    }

    static #assembleFrames(framesJson) {
        let frames = [];

        for (const frame of framesJson) {
            let args = [];
            for (const arg of frame.args) {
                args.push(new Argument(arg.name, arg.value));
            }
            frames.push(new Frame(frame.frameId, frame.spanId, frame.methodId, frame.thisRef, args));
        }

        return frames;
    }

    static #assembleEvents(eventsJson) {
        let events = [];

        for (const event of eventsJson) {
            events.push(this.#assembleEvent(event));
        }

        return events;
    }

    static #assembleEvent(eventJson) {
        let eventId = eventJson.eventId;
        let spanId = eventJson.spanId;
        let frameId = eventJson.frameId;
        let location = this.#assembleTraceLocation(eventJson.location);

        switch (eventJson.type) {
            case "CALL":
                return new CallTraceEvent(
                    eventId,
                    spanId,
                    frameId,
                    location,
                    eventJson.callerMethodId,
                    eventJson.calleeMethodId,
                    eventJson.name
                );

            case "LOCAL":
                return new LocalTraceEvent(
                    eventId,
                    spanId,
                    frameId,
                    location,
                    eventJson.owner,
                    eventJson.method,
                    eventJson.index,
                    eventJson.value
                );

            case "RETURN":
                return new ReturnTraceEvent(
                    eventId,
                    spanId,
                    frameId,
                    location,
                    eventJson.returnValue
                );

            default:
                throw new Error(`Unknown event type: ${eventJson.eventType}`);
        }
    }
}
