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
import {TraceSpanType} from "./TraceSpanType.js";

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
        const documentFragment = document.createDocumentFragment();

        if (event instanceof CallTraceEvent) {
            documentFragment.append(this.#getHeaderBlockDocumentFragment(event));
        }
        else if (event instanceof LocalTraceEvent) {
            documentFragment.append(`${event.varName} ← `);
            documentFragment.append(this.#getShowValue(event.value, true));
        }
        else if (event instanceof ArithmeticTraceEvent) {
            documentFragment.append(this.#getShowValue(event.left), " ");
            documentFragment.append(this.#getOperator(event.operator), " ");
            documentFragment.append(this.#getShowValue(event.right), " → ");
            documentFragment.append(this.#getShowValue(event.value, true));
        }
        else if (event instanceof ConditionTraceEvent) {
            documentFragment.append("IF");
        }
        else if (event instanceof ReturnTraceEvent) {
            this.#appendHighlighted(documentFragment, "return ");
            documentFragment.append(this.#getShowValue(event.value));
        }
        else if (event instanceof ComparisonTraceEvent) {
            documentFragment.append(this.#getShowValue(event.left), " ");
            documentFragment.append(this.#getOperator(event.operator), " ");
            documentFragment.append(this.#getShowValue(event.right), " → ");
            documentFragment.append(this.#getShowValue(event.result, true));
        }
        else if (event instanceof NewTraceEvent) {
            this.#appendHighlighted(documentFragment, "new ");
            documentFragment.append(event.className);
        }
        else if (event instanceof ArrayStoreTraceEvent) {
            documentFragment.append(`${event.arrayVarName}[${event.index}] ← `);
            documentFragment.append(this.#getShowValue(event.value, true));
        }
        else if (event instanceof FieldWriteTraceEvent) {
            documentFragment.append('object.${event.fieldName} = ');
            documentFragment.append(this.#getShowValue(event.value, true));
        }
        else if (event instanceof LoopTraceEvent) {
            documentFragment.append("LOOP");
        }
        else if (event instanceof LoopIterationTraceEvent) {
            documentFragment.append("LOOP ITERATION");
        }
        else {
            throw new Error("Event type not implemented : " + event.constructor.name);
        }

        return documentFragment;
    }

    #manageInnerEvents(event, parentBlock, location, outerParentBlock) {
        if (event instanceof ConditionTraceEvent) {
            const lineNumber = event.location?.line ?? "-";

            const conditionIds = Array.isArray(event.conditionEventIds) ? event.conditionEventIds : [];
            const conditionEvents = conditionIds.map(id => this.traceModel.getEvent(id)).filter(Boolean);

            const conditionBlock = new TraceBlock(this.container, parentBlock, lineNumber, this.#createConditionHeader(event), true, true);

            if (conditionEvents.length > 0) {
                this.#renderEvents(conditionEvents, conditionBlock, event.location ?? null);
            } else {
                conditionBlock.addLine(lineNumber, TraceSpan.wrapLineColors("∅"));
            }

            const thenIds = Array.isArray(event.thenEventIds) ? event.thenEventIds : [];
            const thenEvents = thenIds.map(id => this.traceModel.getEvent(id)).filter(Boolean);

            if (thenEvents.length > 0) {
                this.#renderEvents(thenEvents, parentBlock, event.location ?? null);
            }

            const elseIds = Array.isArray(event.elseEventIds) ? event.elseEventIds : [];
            const elseEvents = elseIds.map(id => this.traceModel.getEvent(id)).filter(Boolean);
            if (elseEvents.length === 0) return;

            if (elseEvents[0] instanceof ConditionTraceEvent) {
                this.#renderEvents(elseEvents, outerParentBlock, location);
            } else {
                this.#renderElseBlock(elseEvents, outerParentBlock);
            }

            return;
        }

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
            const lineNumber = ev.location?.line ?? "-";

            if (parentBlock === null || !ev.location?.equals(currentLocation)) {
                if (ev instanceof CallTraceEvent && (ev.external || ev.bodyEventIds.length === 0)) {
                    // don't create a block for external calls or calls without body
                }
                else {
                    let isDefaultCollapsed = true;
                    if (ev instanceof CallTraceEvent && ev.name === "main") isDefaultCollapsed = false;

                    const headerFrag = this.#getHeaderBlockDocumentFragment(ev);
                    currentBlock = new TraceBlock(this.container, parentBlock, lineNumber, headerFrag, true, isDefaultCollapsed);
                    currentLocation = ev.location;
                }
            }

            this.#manageInnerEvents(ev, currentBlock, currentLocation, parentBlock);

            if (ev instanceof ConditionTraceEvent) {
                continue;
            }

            if (ev instanceof CallTraceEvent && ev.bodyEventIds.length !== 0) {
                continue;
            }

            currentBlock.addLine(lineNumber, this.#getEventLine(ev));
        }
    }

    #getHeaderBlockDocumentFragment(event) {
        const documentFragment = document.createDocumentFragment();

        if (event instanceof CallTraceEvent) {
            if (event.external) {
                documentFragment.append("[EXT] ");
            }

            documentFragment.append(TraceSpan.createSpan(TraceSpanType.FunctionName, event.name));
            this.#appendHighlighted(documentFragment, "(");
            event.args.forEach((arg, i) => {
                if (i > 0) documentFragment.append(TraceSpan.wrapLineColors(", "));
                documentFragment.append(arg.name, " ");
                documentFragment.append(this.#getShowValue(arg.value));
            });
            this.#appendHighlighted(documentFragment, ")");

            if (event.value !== null) {
                documentFragment.append(" → ");
                documentFragment.append(this.#getShowValue(event.value, true));
            }

        }
        else if (event instanceof ConditionTraceEvent) {
            const structure = this.traceModel.getStructureFromEvent(event.eventId);
            const contentLine = structure?.getLineContent() ?? "?";
            documentFragment.append(TraceSpan.wrapLineColors(contentLine));
            documentFragment.append(" ↦ ");

            const result = event.value.value;
            const traceSpanType = result === true ? TraceSpanType.True : TraceSpanType.False;
            documentFragment.append(TraceSpan.createSpan(traceSpanType, result ? "true" : "false"));
        }
        else {
            const structure = this.traceModel.getStructureFromEvent(event.eventId);
            const contentLine = structure?.getLineContent() ?? "?";
            documentFragment.append(TraceSpan.wrapLineColors(contentLine));
        }

        return documentFragment;
    }

    #getShowValue(value, isReturnValue = false) {
        const documentFragment = document.createDocumentFragment();

        if (value === null || value.kind === "NULL") {
            const traceSpanType = isReturnValue ? TraceSpanType.ReturnValuePrimitive : TraceSpanType.ArgsValuePrimitive;
            documentFragment.append(TraceSpan.createSpan(traceSpanType, "null"));
        }
        else if (value.value !== null) {
            const traceSpanType = isReturnValue ? TraceSpanType.ReturnValuePrimitive : TraceSpanType.ArgsValuePrimitive;
            const isString = value.type.includes("String") || value.type.includes("string");
            const lineContent = isString ? "\"" + value.value + "\"" : value.value;
            documentFragment.append(TraceSpan.createSpan(TraceSpanType.Type, value.type));
            documentFragment.append(":");
            documentFragment.append(TraceSpan.createSpan(traceSpanType, lineContent));
        }
        else {
            const traceSpanType = isReturnValue ? TraceSpanType.ReturnValue : TraceSpanType.ArgsValue;
            const lineContent = value.type + ":" + value.valueObjectId.replace(":", "");
            documentFragment.append(TraceSpan.createSpan(traceSpanType, lineContent));
        }

        return documentFragment;
    }

    #renderElseBlock(elseEvents, outerParentBlock) {
        if (!elseEvents || elseEvents.length === 0) return;

        const first = elseEvents[0];
        const lineNumber = first.location?.line ?? "-";

        const headerFrag = document.createDocumentFragment();
        headerFrag.append(TraceSpan.wrapLineColors("else"));

        const elseBlock = new TraceBlock(this.container, outerParentBlock, lineNumber, headerFrag, true, true);

        this.#renderEvents(elseEvents, elseBlock, first.location);
    }

    #createConditionHeader(conditionEvent) {
        const frag = document.createDocumentFragment();

        const structure = this.traceModel.getStructureFromEvent(conditionEvent.eventId, true);
        const contentLine = structure?.getLineContent() ?? "condition";

        frag.append(TraceSpan.wrapLineColors("condition: "));
        frag.append(TraceSpan.wrapLineColors(contentLine));
        frag.append(TraceSpan.wrapLineColors(" ↦ "));

        const result = conditionEvent.value?.value === true;
        frag.append(TraceSpan.createSpan(result ? TraceSpanType.True : TraceSpanType.False, result ? "true" : "false"));

        return frag;
    }

    #appendHighlighted(documentFragment, str) {
        documentFragment.append(TraceSpan.wrapLineColors(str));
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
