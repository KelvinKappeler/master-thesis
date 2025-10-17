import {TraceContainer} from "./TraceContainer.js";
import {CallTraceEvent, LocalTraceEvent} from "../model/EventDefs.js";
import {BaseTriangle} from "../elements/Triangle.js";

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

            const firstLineFrag = this.#highlight(structure.getLineContent());
            const ct = this.#addLine(event.location.line, firstLineFrag, null, true);
            this.#addLine(event.location.line, this.#getEventLine(event), ct);
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
                return "<internal method>";
            }

            return callee.name;
        }
        else if (event instanceof LocalTraceEvent) {
            const variable = this.traceViewModel.getLocalVar(event.methodId, event.index);
            return variable.name + " := " + event.value;
        }
        else {
            throw new Error("Event type not implemented : " + event.constructor.name);
        }
    }

    #highlight(line) {
        const keywords = [
            "abstract", "continue", "for", "new", "switch", "default", "do", "if", "private", "this",
            "break", "double", "implements", "protected", "throw", "byte", "else", "import", "public", "throws",
            "case", "enum", "instanceof", "return", "transient", "catch", "extends", "int", "short", "try",
            "char", "final", "interface", "static", "void", "class", "finally", "long", "volatile", "float",
            "native", "super", "while"
        ];
        const frag = document.createDocumentFragment();
        const text = String(line);
        const re = new RegExp(`(\\b(?:${keywords.join("|")})\\b|[()])`, "g");

        let last = 0;
        let m;
        while ((m = re.exec(text)) !== null) {
            if (m.index > last) frag.appendChild(document.createTextNode(text.slice(last, m.index)));

            const span = document.createElement("span");
            if (m[0] === "(" || m[0] === ")") {
                span.className = "parenthesis";
            } else {
                span.className = "keyword";
            }
            span.textContent = m[0];
            frag.appendChild(span);

            last = re.lastIndex;
        }
        if (last < text.length) frag.appendChild(document.createTextNode(text.slice(last)));

        return frag;
    }

    #manageTriangle() {

    }
}
