import {Preconditions} from "../utils/Preconditions.js";

/**
 * Represents the container for the visual parts of the trace.
 */
export class TraceContainer {
    constructor(traceContentArea, lineNumbersArea, trianglesArea) {
        Preconditions.requireNonNull(traceContentArea);
        Preconditions.requireNonNull(lineNumbersArea);
        Preconditions.requireNonNull(trianglesArea);

        this.traceContentArea = traceContentArea;
        this.lineNumbersArea = lineNumbersArea;
        this.trianglesArea = trianglesArea;
    }

    /**
     * Clears the trace container.
     */
    clear() {
        this.traceContentArea.replaceChildren();
        this.lineNumbersArea.replaceChildren();
        this.trianglesArea.replaceChildren();
    }
}
