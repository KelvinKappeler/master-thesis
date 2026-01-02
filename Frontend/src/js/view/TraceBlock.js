import {Preconditions} from "../utils/Preconditions.js";
import {BaseTriangle} from "../elements/BaseTriangle.js";

/**
 * Represents a block in the trace. A block has a header and can contain other blocks or lines (events).
 * @param {TraceContainer} container - The container for the block.
 * @param {TraceBlock|null} parent - The parent block.
 * @param {number} headerLineNumber - The line number of the header.
 * @param {string|Node} headerContent - The content of the header.
 * @param {boolean} canBeCollapsed - Whether the block can be collapsed.
 * @param {boolean} isDefaultCollapsed - Whether the block is collapsed by default.
 * @param {(traceEvent: any|null) => void} onHover - Callback for hover events.
 */
export class TraceBlock {
    constructor(container, parent = null, headerLineNumber, headerContent, canBeCollapsed = true, isDefaultCollapsed = true, onHover = null) {
        Preconditions.requireNonNull(container);

        this.container = container;
        this.parent = parent;
        this.depth = parent ? parent.depth + 1 : 0;
        this.onHover = onHover;

        const parents = parent ? parent._childrenRoots : {
            lineNumber : container.lineNumbersArea,
            triangle : container.trianglesArea,
            content : container.traceContentArea
        }

        this._header = {
            lineNumber : document.createElement('div'),
            triangle : document.createElement('div'),
            content : document.createElement('div')
        }

        // == Header ==
        this._header.content.dataset.depth = String(this.depth);
        this._header.content.style.paddingLeft = `${this.depth * 2}ch`;
        this._header.lineNumber.textContent = String(headerLineNumber);

        if (headerContent instanceof Node) {
            this._header.content.append(headerContent);
        } else {
            this._header.content.textContent = String(headerContent ?? "");
        }

        parents.lineNumber.append(this._header.lineNumber);
        parents.triangle.append(this._header.triangle);
        parents.content.append(this._header.content);

        // == Children ==
        this._childrenRoots = {
            lineNumber: document.createElement("div"),
            triangle: document.createElement("div"),
            content: document.createElement("div"),
        };

        parents.lineNumber.append(this._childrenRoots.lineNumber);
        parents.triangle.append(this._childrenRoots.triangle);
        parents.content.append(this._childrenRoots.content);

        // == Triangle ==
        if (canBeCollapsed) {
            const triangle = new BaseTriangle(
                [
                    this._childrenRoots.lineNumber,
                    this._childrenRoots.triangle,
                    this._childrenRoots.content,
                ],
                isDefaultCollapsed
            );
            triangle.attachTo(this._header.triangle);
            this._triangle = triangle;
        } else {
            this._header.triangle.textContent = "\u00A0";
            this._triangle = null;
        }
    }

    /**
     * Sets the header highlighted state.
     * @param on - Whether the header should be highlighted.
     * @param color - The color to use for highlighting.
     */
    setHeaderHighlighted(on, color = "rgba(147,74,172,0.5)") {
        if (!this._header?.content) return;
        this._header.content.style.backgroundColor = on ? color : "";
    }

    /**
     * Adds a line to the block.
     * @param {number} lineNumber - The line number of the line.
     * @param {string|Node} content - The content of the line.
     * @param {any|null} traceEvent - The trace event associated with the line.
     */
    addLine(lineNumber, content, traceEvent = null) {
        const ln = document.createElement("div");
        ln.textContent = String(lineNumber);

        const triangle = document.createElement("div");
        triangle.textContent = "\u00A0";

        const ct = document.createElement("div");
        const depth = this.depth + 1;
        ct.dataset.depth = String(depth);
        ct.style.paddingLeft = `${depth * 2}ch`;

        if (content instanceof Node) {
            ct.append(content);
        } else {
            ct.textContent = String(content ?? "");
        }

        if (traceEvent) {
            ct.addEventListener("mouseenter", () => this.onHover?.(traceEvent));
        }

        this._childrenRoots.lineNumber.append(ln);
        this._childrenRoots.triangle.append(triangle);
        this._childrenRoots.content.append(ct);

        return ct;
    }

    /**
     * Collapses the block if it can be collapsed.
     */
    collapse() {
        if (this._triangle) {
            this._triangle.collapse();
        }
    }

    /**
     * Expands the block if it can be collapsed.
     */
    expand() {
        if (this._triangle) {
            this._triangle.expand();
        }
    }

    bindHeaderHover(traceEvent) {
        if (!this._header?.content) return;

        this._header.content.addEventListener("mouseenter", () => this.onHover?.(traceEvent));
    }
}
