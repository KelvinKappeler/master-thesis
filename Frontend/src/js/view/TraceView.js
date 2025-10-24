import {TraceContainer} from "./TraceContainer.js";
import {ArithmeticTraceEvent, CallTraceEvent, LocalTraceEvent, ReturnTraceEvent} from "../model/EventDefs.js";
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
    }

    /**
     * Renders the trace view.
     */
    render() {
        this.container.clear();
        this.breadcrumb.render();

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
        }
    }

    #addLine(lineNumber, content, parent = null, addTriangle = false, isContentDefaultHidden = false) {
        const ln = document.createElement('div');
        ln.textContent = String(lineNumber);

        const ct = document.createElement('div');
        const depth = parent ? (Number(parent.dataset.depth || 0) + 1) : 0;
        ct.dataset.depth = depth.toString();
        ct.style.paddingLeft = `${depth * 2}ch`;

        if (content instanceof Node) {
            ct.append(content);
        } else {
            ct.textContent = String(content);
        }

        if (parent !== null) {
            parent.append(ct);
        }
        else {
            this.container.traceContentArea.append(ct);
        }
        this.container.lineNumbersArea.append(ln);

        if (addTriangle) {
            const placeholder = document.createElement('div');
            const triangle = new BaseTriangle([ct], isContentDefaultHidden);
            triangle.attachTo(placeholder);
            this.container.trianglesArea.append(placeholder);
        } else {
            const placeholder = document.createElement('div');
            placeholder.textContent = " ";
            this.container.trianglesArea.append(placeholder);
        }

        return ct;
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
        else {
            throw new Error("Event type not implemented : " + event.constructor.name);
        }
    }
}
