import {TraceContainer} from "./TraceContainer.js";
import {ArithmeticTraceEvent, CallTraceEvent,ConditionTraceEvent, LocalTraceEvent, ReturnTraceEvent} from "../model/EventDefs.js";
import {BaseTriangle} from "../elements/BaseTriangle.js";
import {TraceBlock} from "./TraceBlock.js";
import {TraceSpan} from "./TraceSpan.js";
import {Breadcrumb} from "../elements/Breadcrumb.js";

/**
 * Represents the view for the trace part of the application.
 * @param traceViewModel {TraceViewModel} The view model for the trace.
 * @param traceContainer {TraceContainer} The container for the trace.
 */
export class TraceView {
    constructor(traceViewModel, traceContainer) {
        this.traceViewModel = traceViewModel;
        this.container = traceContainer;

        this.breadcrumb = new Breadcrumb(traceViewModel);
        this.breadcrumb.attachTo(document.querySelector('.breadcrumb'));

        this.childRunState = new Map();
    }

    /**
     * Renders the trace view.
     */
    render() {
        this.container.clear();
        this.breadcrumb.render();
        this.childRunState.clear();

        const eventStructureMap = this.traceViewModel.getEventsProgramStructureMap();

        let traceLoc = null;
        let block = null;
        for (const [event, structure] of eventStructureMap) {
            if (structure === null || structure === undefined) continue;

            const location = event.location;
            if (!location.equals(traceLoc)) {
                const headerFrag = TraceSpan.wrapLineColors(structure.getLineContent());
                block = new TraceBlock(this.container, null, location.line, headerFrag, true, true);
            }

            traceLoc = location;
            block.addLine(event.location.line, this.#getEventLine(event));
            this.#manageInnerEvents(event, block);
        }
    }

    #getEventLine(event) {
        if (event instanceof CallTraceEvent) {
            const callee = this.traceViewModel.getMethod(event.calleeMethodId);
            if (callee === undefined) {
                return event.name;
            }

            return callee.name;
        }
        else if (event instanceof LocalTraceEvent) {
            const variable = this.traceViewModel.getLocalVar(event.methodId, event.index);
            return variable.name + " ← " + event.value;
        }
        else if (event instanceof ArithmeticTraceEvent) {
            return `${event.left} ${event.operation} ${event.right} = ${event.result}`;
        }
        else if (event instanceof ConditionTraceEvent) {
            return event.result ? "true" : "false";
        }
        else {
            throw new Error("Event type not implemented : " + event.constructor.name);
        }
    }

    #manageInnerEvents(event, parentBlock) {
        if (!(event instanceof ConditionTraceEvent)) return;
        if (event.childrenEventIds.length === 0) return;

        let state = this.childRunState.get(parentBlock);
        if (!state) {
            state = { lastLine: null, currentChildBlock: null };
            this.childRunState.set(parentBlock, state);
        }

        for (const childEventId of event.childrenEventIds) {
            const [childEvent, childStructure] = this.traceViewModel.getEvent(childEventId);
            if (!childEvent || !childStructure) continue;

            const childLine = childEvent.location.line;

            if (state.lastLine !== childLine) {
                const headerFrag = TraceSpan.wrapLineColors(childStructure.getLineContent());
                state.currentChildBlock = new TraceBlock(this.container, parentBlock, childLine, headerFrag, true, true);
            }

            state.lastLine = childLine;
            state.currentChildBlock.addLine(childLine, this.#getEventLine(childEvent));
        }
    }
}
