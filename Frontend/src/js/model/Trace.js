/**
 * Represents a trace of events. Core of the tracing debugger.
 */
export class Trace {

    constructor(program, trace, index) {
        this.program = program;
        this.trace = trace;
        this.index = index;
    }
}
