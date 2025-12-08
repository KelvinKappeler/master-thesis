import {TraceContainer} from "./TraceContainer.js";
import {
    ArithmeticTraceEvent,
    CallTraceEvent,
    ConditionTraceEvent,
    LocalTraceEvent,
    ReturnTraceEvent,
    ArrayStoreTraceEvent,
    FieldWriteTraceEvent,
    LoopTraceEvent,
    LoopIterationTraceEvent,
    ComparisonTraceEvent,
    NewTraceEvent
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
        this.#renderEvents(mainEvents, null);
    }

    #getEvents() {
        const eventIds = this.traceModel?.mainData?.index?.bySpan.get("spn:1");
        const event = this.traceModel.getEvent(eventIds[0]);

        return [event];
    }

    #getEventLine(event) {
        if (event instanceof CallTraceEvent) {
            /*const callee = this.traceViewModel.getMethod(event.calleeMethodId);
            if (callee === undefined) {
                return event.name;
            }*/

            return "CALL";
        }
        else if (event instanceof LocalTraceEvent) {
            return event.varName + " ← " + event.value.value;
        }
        else if (event instanceof ArithmeticTraceEvent) {
            return `${event.left} ${event.operation} ${event.right} = ${event.result}`;
        }
        else if (event instanceof ConditionTraceEvent) {
            return event.result ? "true" : "false";
        }
        else if (event instanceof ReturnTraceEvent) {
            return "RETURN " + event.value;
        }
        else if (event instanceof ComparisonTraceEvent) {
            return `${event.left} ${event.operation} ${event.right} = ${event.result}`;
        }
        else {
            throw new Error("Event type not implemented : " + event.constructor.name);
        }
    }

    #manageInnerEvents(event, parentBlock) {
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

        this.#renderEvents(children, parentBlock);
    }

    #renderEvents(events, parentBlock) {
        if (!Array.isArray(events) || events.length === 0) return;

        let currentBlock = null;
        let currentLine = null;
        let currentLineContentKey = null;

        for (const ev of events) {
            const structure = this.traceModel.getStructureFromEvent(ev.eventId);
            if (!structure) {
                this.#manageInnerEvents(ev, parentBlock);
                continue;
            }

            const line = ev.location?.line ?? "-";
            const lineContent = structure?.getLineContent() ?? "?";
            const contentKey = `${line}:${lineContent}`;

            if (currentLine !== line || currentLineContentKey !== contentKey) {
                const headerFrag = TraceSpan.wrapLineColors(lineContent);
                currentBlock = new TraceBlock(this.container, parentBlock, line, headerFrag, true, true);
                currentLine = line;
                currentLineContentKey = contentKey;
            }

            currentBlock.addLine(line, this.#getEventLine(ev));
            this.#manageInnerEvents(ev, currentBlock);
        }
    }
}
