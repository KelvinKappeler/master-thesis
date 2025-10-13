import {TraceContainer} from "./TraceContainer.js";
import {CallTraceEvent, LocalTraceEvent} from "../model/EventDefs.js";

/**
 * Represents the view for the trace part of the application.
 * @param traceViewModel {TraceViewModel} The view model for the trace.
 * @param traceContainer {TraceContainer} The container for the trace.
 */
export class TraceView {
    constructor(traceViewModel, traceContainer) {
        this.traceViewModel = traceViewModel;
        this.container = traceContainer;
    }

    /**
     * Renders the trace view.
     */
    render() {
        const eventStructureMap = this.traceViewModel.getEventsProgramStructureMap();

        for (const [event, structure] of eventStructureMap) {
            if (structure === null || structure === undefined) continue;

            const ct = this.#addLine(event.location.line, structure.getLineContent(), null);
            this.#addLine(event.location.line, this.#getEventLine(event), ct);
        }
    }

    #addLine(lineNumber, content, parent = null, isContentDefaultHidden = false) {
        const ln = document.createElement('div');
        ln.textContent = String(lineNumber);

        const ct = document.createElement('div');
        const depth = parent ? (Number(parent.dataset.depth || 0) + 1) : 0;
        ct.textContent = ' '.repeat(depth * 2) + String(content);
        ct.dataset.depth = depth.toString();

        if (parent !== null) {
            parent.append(ct);
        }
        else {
            this.container.traceContentArea.append(ct);
        }
        this.container.lineNumbersArea.append(ln);

        return ct;
    }

    #getEventLine(event) {
        if (event instanceof CallTraceEvent) {
            return "CALL " + event.calleeMethodId;
        }
        else if (event instanceof LocalTraceEvent) {
            return event.varName + " := " + event.value;
        }
        else {
            throw new Error("Event type not implemented : " + event.constructor.name);
        }
    }
}
