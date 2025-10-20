/**
 * Enum for trace span types.
 */
export class TraceSpanType {
    static Parenthesis = new TraceSpanType("parenthesis");
    static ReturnValue = new TraceSpanType("returnValue");
    static ReturnValuePrimitive = new TraceSpanType("returnValuePrimitive");
    static LoopHeaderAssignment = new TraceSpanType("loopHeaderAssignment");
    static LoopHeaderCondition = new TraceSpanType("loopHeaderCondition");
    static FunctionName = new TraceSpanType("functionName");
    static ArgsValue = new TraceSpanType("argsValue");
    static ArgsValuePrimitive = new TraceSpanType("argsValuePrimitive");
    static Keywords = new TraceSpanType("keywords");
    static String = new TraceSpanType("string");
    static None = new TraceSpanType("none");

    constructor(name) {
        this.name = name;
    }
}
