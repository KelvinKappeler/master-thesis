/**
 * Enum for trace span types.
 */
export class TraceSpanType {
    static Parenthesis = new TraceSpanType("parenthesis");
    static ReturnValue = new TraceSpanType("returnValue");
    static ReturnValuePrimitive = new TraceSpanType("returnValuePrimitive");
    static Annotation = new TraceSpanType("annotation");
    static FunctionName = new TraceSpanType("functionName");
    static ArgsValue = new TraceSpanType("argsValue");
    static ArgsValuePrimitive = new TraceSpanType("argsValuePrimitive");
    static Keywords = new TraceSpanType("keywords");
    static String = new TraceSpanType("string");
    static Type = new TraceSpanType("type");
    static True = new TraceSpanType("true");
    static False = new TraceSpanType("false");
    static None = new TraceSpanType("none");

    constructor(name) {
        this.name = name;
    }
}
