import {TraceContainer} from "./TraceContainer.js";
import {
    ArithmeticTraceEvent,
    ArrayStoreTraceEvent,
    CallTraceEvent,
    ComparisonTraceEvent,
    ConditionTraceEvent,
    FieldWriteTraceEvent,
    LocalTraceEvent,
    LoopIterationTraceEvent,
    LoopTraceEvent,
    NewTraceEvent,
    ReturnTraceEvent
} from "../model/EventDefs.js";
import {TraceBlock} from "./TraceBlock.js";
import {TraceSpan} from "./TraceSpan.js";

/**
 * Represents the view for the trace part of the application.
 * @param traceModel {TraceModel} The model for the trace.
 * @param traceContainer {TraceContainer} The container for the trace.
 */
export class TraceView {
    constructor(traceModel, traceContainer) {
        this.traceModel = traceModel;
        this.container = traceContainer;

        //this.breadcrumb = new Breadcrumb(traceViewModel);
        //this.breadcrumb.attachTo(document.querySelector('.breadcrumb'));
    }

    /**
     * Renders the trace view.
     */
    render() {
        this.container.clear();
        //this.breadcrumb.render();
        const mainEvents = this.#getEvents();
        this.#renderEvents(mainEvents, null, null);
    }

    #getEvents() {
        const eventIds = this.traceModel?.mainData?.index?.bySpan.get("spn:1");
        const event = this.traceModel.getEvent(eventIds[0]);

        return [event];
    }

    #getEventLine(event) {
        if (event instanceof CallTraceEvent) {
            // This is handled in #renderEvents by creating a new block
            throw new Error("CallTraceEvent should not be rendered as a line directly.");
        }
        else if (event instanceof LocalTraceEvent) {
            return `${event.varName} ← ${this.#getShowValue(event.value)}`;
        }
        else if (event instanceof ArithmeticTraceEvent) {
            return `${this.#getShowValue(event.left)} ${this.#getOperator(event.operator)} ${this.#getShowValue(event.right)} = ${this.#getShowValue(event.value)}`;
        }
        else if (event instanceof ConditionTraceEvent) {
            return event.result ? "true" : "false";
        }
        else if (event instanceof ReturnTraceEvent) {
            return `return ${this.#getShowValue(event.value)}`;
        }
        else if (event instanceof ComparisonTraceEvent) {
            return `${this.#getShowValue(event.left)} ${this.#getOperator(event.operator)} ${this.#getShowValue(event.right)} = ${this.#getShowValue(event.result)}`;
        }
        else if (event instanceof NewTraceEvent) {
            return `new ${event.className}()`;
        }
        else if (event instanceof ArrayStoreTraceEvent) {
            return `array[${event.index}] = ${event.value}`;
        }
        else if (event instanceof FieldWriteTraceEvent) {
            return `object.${event.fieldName} = ${event.value}`;
        }
        else if (event instanceof LoopTraceEvent) {
            return `LOOP`;
        }
        else if (event instanceof LoopIterationTraceEvent) {
            return `LOOP ITERATION`;
        }
        else {
            throw new Error("Event type not implemented : " + event.constructor.name);
        }
    }

    #manageInnerEvents(event, parentBlock, location) {
        const childIds = [];

        if (event instanceof CallTraceEvent) {
            if (Array.isArray(event.bodyEventIds)) childIds.push(...event.bodyEventIds);
        } else if (event instanceof LocalTraceEvent) {
            if (event.bodyEventId) childIds.push(event.bodyEventId);
        } else if (event instanceof ReturnTraceEvent) {
            if (event.bodyEventId) childIds.push(event.bodyEventId);
        } else if (event instanceof ArrayStoreTraceEvent) {
            if (event.bodyEventId) childIds.push(event.bodyEventId);
        } else if (event instanceof FieldWriteTraceEvent) {
            if (event.bodyEventId) childIds.push(event.bodyEventId);
        } else if (event instanceof ConditionTraceEvent) {
            if (Array.isArray(event.conditionEventIds)) childIds.push(...event.conditionEventIds);
            if (Array.isArray(event.thenEventIds)) childIds.push(...event.thenEventIds);
            if (Array.isArray(event.elseEventIds)) childIds.push(...event.elseEventIds);
        } else if (event instanceof LoopTraceEvent) {
            if (Array.isArray(event.initEventIds)) childIds.push(...event.initEventIds);
            if (Array.isArray(event.iterationsEventIds)) childIds.push(...event.iterationsEventIds);
        } else if (event instanceof LoopIterationTraceEvent) {
            if (Array.isArray(event.conditionEventIds)) childIds.push(...event.conditionEventIds);
            if (Array.isArray(event.bodyEventIds)) childIds.push(...event.bodyEventIds);
            if (Array.isArray(event.updateEventIds)) childIds.push(...event.updateEventIds);
        } else if (event instanceof ArithmeticTraceEvent) {
            if (event.leftEventId) childIds.push(event.leftEventId);
            if (event.rightEventId) childIds.push(event.rightEventId);
        } else if (event instanceof ComparisonTraceEvent) {
            if (event.leftEventId) childIds.push(event.leftEventId);
            if (event.rightEventId) childIds.push(event.rightEventId);
        } else if (event instanceof NewTraceEvent) {
            // no children
        }

        if (childIds.length === 0) return;

        const children = childIds.map(id => this.traceModel.getEvent(id)).filter(Boolean);

        this.#renderEvents(children, parentBlock, location);
    }

    #renderEvents(events, parentBlock, location) {
        if (!Array.isArray(events) || events.length === 0) return;

        let currentBlock = parentBlock;
        let currentLocation = location;

        for (const ev of events) {
            const line = ev.location?.line ?? "-";
            const lineContent = this.#getLineContent(ev);

            if (parentBlock === null || !ev.location?.equals(currentLocation)) {
                const headerFrag = TraceSpan.wrapLineColors(lineContent);
                currentBlock = new TraceBlock(this.container, parentBlock, line, headerFrag, true, true);
                currentLocation = ev.location;
            }

            this.#manageInnerEvents(ev, currentBlock, currentLocation);

            if (ev instanceof CallTraceEvent) {
                continue;
            }

            currentBlock.addLine(line, this.#getEventLine(ev));
        }
    }

    #getLineContent(event) {
        if (event instanceof CallTraceEvent) {
            return event.name + "(" + event.args.map(arg => arg.name + ":" + this.#getShowValue(arg.value)).join(", ") + ")";
        }
        else {
            const structure = this.traceModel.getStructureFromEvent(event.eventId);

            return structure?.getLineContent() ?? "?";
        }
    }

    #getShowValue(value) {
        if (value.kind === "NULL") return "null";
        else if (value.value !== null) {
            return value.value;
        }
        else {
            return value.valueObjectId;
        }
    }

    #getOperator(operator) {
        switch (operator) {
            case "PLUS": return "+";
            case "SUB": return "-";
            case "MUL": return "*";
            case "DIV": return "/";
            case "MOD": return "%";

            case "EQ": return "==";
            case "NE": return "!=";
            case "LT": return "<";
            case "LE": return "<=";
            case "GT": return ">";
            case "GE": return ">=";

            case "AND": return "&&";
            case "OR": return "||";

            default: return operator;
        }
    }
}
