package ch.epfl.printwizard.plugin.logging;

import ch.epfl.printwizard.plugin.model.trace.*;
import ch.epfl.printwizard.plugin.model.trace.events.*;
import ch.epfl.printwizard.plugin.utils.Ids;

import java.util.*;

/**
 * Represents a list of static methods used by the program to log data during its execution.
 */
@SuppressWarnings("unused")
public class TraceOut {

    private static final ThreadLocal<Deque<SpanCtx>> SPAN_STACK = ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<Deque<ExecCtx>> CTX_STACK = ThreadLocal.withInitial(ArrayDeque::new);

    private TraceOut() {}

    @SuppressWarnings("unused")
    public static void onEnter(Object thisRef, String owner, String method, Arg[] args, String returnType, String sourceId, int line) {
        String frameId = Ids.nextFrameId();
        String spanId = Ids.nextSpanId();
        String[] argsTypes = Arrays.stream(args).map(Arg::type).toArray(String[]::new);
        String methodId = Ids.createNewMethodId(owner, method, argsTypes, returnType);
        String callerMethodId = SPAN_STACK.get().isEmpty() ? "null" : currentFrameCtx().methodId();
        String parentSpanId = SPAN_STACK.get().isEmpty() ? "null" : currentFrameCtx().spanId();
        String startEventId = Ids.nextEventId();
        TraceLoc loc = new TraceLoc(sourceId, line);
        String thisRefObjectId = thisRef != null ? EventValue.getOrCreateObjectId(thisRef) : null;
        
        CallEvent callEvent = new CallEvent(
            startEventId, spanId,
            loc,
            callerMethodId, methodId,
            method, false, args,
            EventValue.of(null), new String[0]
        );
        addEvent(callEvent, true);

        OutputManager.getTraceFileBuilder().addSpan(new TraceSpan(
            spanId, parentSpanId, methodId, thisRefObjectId, List.of(args),
            startEventId, "end",
            loc, new TraceLoc("source", 1)
        ));

        SPAN_STACK.get().push(new SpanCtx(spanId, methodId));
        CTX_STACK.get().push(new MethodCtx(startEventId, spanId, frameId));
    }

    @SuppressWarnings("unused")
    public static void recordCall(String owner, String method, Arg[] args, String returnType, boolean isExternal, String sourceId, int line) {
        SpanCtx ctx = currentFrameCtx();
        TraceLoc loc = new TraceLoc(sourceId, line);
        String[] argTypes = Arrays.stream(args).map(Arg::type).toArray(String[]::new);
        String eventId = Ids.nextEventId();
        String callerMethodId = ctx.methodId();
        String calleeMethodId = isExternal ? "-" : Ids.createNewMethodId(owner, method, argTypes, returnType);
        boolean isVoid = "void".equals(returnType);

        CallEvent ev = new CallEvent(
            eventId, ctx.spanId(),
            loc, callerMethodId, calleeMethodId,
            method, isExternal, args,
            isVoid ? null : EventValue.of(null), new String[0]
        );

        addEvent(ev, true);
    }

    @SuppressWarnings("unused")
    public static <T> T recordNewObject(T obj, String typeName, String sourceId, int line) {
        SpanCtx ctx = currentFrameCtx();
        TraceLoc loc = new TraceLoc(sourceId, line);

        String objectId = EventValue.getOrCreateObjectId(obj);

        addEvent(new NewEvent(Ids.nextEventId(), ctx.spanId(), loc, objectId, typeName), true);

        return obj;
    }

    @SuppressWarnings("unused")
    public static String beginArrayStore(String label, Object arrayRef, String arrayName, int index, String sourceId, int line) {
        SpanCtx frame = currentFrameCtx();

        String eventId = Ids.nextEventId();
        TraceLoc loc = new TraceLoc(sourceId, line);

        String arrayObjectId = EventValue.getOrCreateObjectId(arrayRef);

        ArrayStoreCtx ctx = new ArrayStoreCtx(
            eventId, frame.spanId(), frame.methodId(),
            loc, arrayName, arrayObjectId, index, label
        );

        CTX_STACK.get().push(ctx);

        return eventId;
    }

    @SuppressWarnings("unused")
    public static <T> T endArrayStore(String arrayEventId, T value) {
        Deque<ExecCtx> stack = CTX_STACK.get();
        ArrayStoreCtx found = null;

        while (!stack.isEmpty()) {
            ExecCtx ctx = stack.pop();
            if (ctx instanceof ArrayStoreCtx ac && ac.getArrayEventId().equals(arrayEventId)) {
                found = ac;
                break;
            }
        }

        if (found == null) {
            throw new IllegalStateException("No ArrayStoreCtx for id " + arrayEventId);
        }

        String rootEventId = null;
        List<String> children = found.getChildEventIds();
        if (!children.isEmpty()) {
            rootEventId = children.getLast();
        }

        ArrayStoreEvent ev = new ArrayStoreEvent(
            found.getArrayEventId(), found.getSpanId(),
            found.getLocation(),
            found.getArrayVarName(),
            found.getArrayObjectId(),
            found.getIndex(),
            EventValue.of(value),
            found.getLabel(), rootEventId
        );

        addEvent(ev, true);

        return value;
    }

    @SuppressWarnings("unused")
    public static String beginComparison(String op, String sourceId, int line) {
        SpanCtx frame = currentFrameCtx();

        String eventId = Ids.nextEventId();
        TraceLoc loc = new TraceLoc(sourceId, line);

        ComparisonCtx ctx = new ComparisonCtx(
            eventId, frame.spanId(), frame.methodId(),
            loc, op
        );

        CTX_STACK.get().push(ctx);

        return eventId;
    }

    @SuppressWarnings("unused")
    public static String beginComparisonLeft() {
        ComparisonCtx ctx = findTopComparisonCtx();
        if (ctx == null) {
            throw new IllegalStateException("No active ComparisonCtx found");
        }

        CTX_STACK.get().push(new ComparisonPhaseCtx(ctx, ComparisonPhase.LEFT));
        return ctx.getComparisonEventId();
    }

    @SuppressWarnings("unused")
    public static <T> T endComparisonLeft(String comparisonEventId, T value) {
        Deque<ExecCtx> stack = CTX_STACK.get();
        if (!stack.isEmpty() && stack.peek() instanceof ComparisonPhaseCtx cpc && cpc.getPhase() == ComparisonPhase.LEFT) {
            stack.pop();
        }

        return value;
    }

    @SuppressWarnings("unused")
    public static String beginComparisonRight() {
        ComparisonCtx ctx = findTopComparisonCtx();
        if (ctx == null) {
            throw new IllegalStateException("No active ComparisonCtx found");
        }
        CTX_STACK.get().push(new ComparisonPhaseCtx(ctx, ComparisonPhase.RIGHT));

        return ctx.getComparisonEventId();
    }

    @SuppressWarnings("unused")
    public static <T> T endComparisonRight(String comparisonEventId, T value) {
        Deque<ExecCtx> stack = CTX_STACK.get();
        if (!stack.isEmpty() && stack.peek() instanceof ComparisonPhaseCtx cpc &&
                cpc.getPhase() == ComparisonPhase.RIGHT) {
            stack.pop();
        }

        return value;
    }

    @SuppressWarnings("unused")
    public static boolean endComparison(String comparisonEventId, Object left, Object right) {
        Deque<ExecCtx> stack = CTX_STACK.get();

        while (!stack.isEmpty() && stack.peek() instanceof ComparisonPhaseCtx) {
            stack.pop();
        }

        ComparisonCtx found = null;
        for (ExecCtx ctx : stack) {
            if (ctx instanceof ComparisonCtx cc && cc.getComparisonEventId().equals(comparisonEventId)) {
                found = cc;
                break;
            }
        }

        if (found == null) {
            throw new IllegalStateException("No ComparisonCtx for id " + comparisonEventId);
        }

        stack.remove(found);

        String leftEventId = found.getLeftEventIds().isEmpty() ? null : found.getLeftEventIds().getLast();
        String rightEventId = found.getRightEventIds().isEmpty() ? null : found.getRightEventIds().getLast();

        boolean result = computeComparison(found.getOperator(), left, right);

        ComparisonEvent ev = new ComparisonEvent(
            found.getComparisonEventId(), found.getSpanId(),
            found.getLocation(), found.getOperator(),
            EventValue.of(left), leftEventId,
            EventValue.of(right), rightEventId,
            new EventValue(result, null, "boolean", ValueKind.PRIMITIVE)
        );

        addEvent(ev, true);

        return result;
    }

    private static ComparisonCtx findTopComparisonCtx() {
        Deque<ExecCtx> stack = CTX_STACK.get();
        for (ExecCtx ctx : stack) {
            if (ctx instanceof ComparisonCtx cc) {
                return cc;
            }
        }
        return null;
    }

    private static boolean computeComparison(String op, Object left, Object right) {
        if ("EQ".equals(op) || "NE".equals(op)) {
            boolean eq;

            if (left == null || right == null) {
                eq = (left == right);
            } else if (left instanceof Number lNum && right instanceof Number rNum) {
                double lv = lNum.doubleValue();
                double rv = rNum.doubleValue();
                eq = Double.compare(lv, rv) == 0;
            } else if (left instanceof Character lc && right instanceof Character rc) {
                eq = lc.charValue() == rc.charValue();
            } else if (left instanceof Boolean lb && right instanceof Boolean rb) {
                eq = lb == rb;
            } else {
                eq = left.equals(right);
            }
            
            return "EQ".equals(op) == eq;
        }
        
        if ("AND".equals(op) || "&&".equals(op) || "OR".equals(op) || "||".equals(op)) {
            if (!(left instanceof Boolean lb) || !(right instanceof Boolean rb)) {
                throw new IllegalStateException("Logical " + op + " with non-boolean operands: "
                    + (left == null ? "null" : left.getClass()) + " and "
                    + (right == null ? "null" : right.getClass()));
            }

            if ("AND".equals(op) || "&&".equals(op)) {
                return lb && rb;
            } else {
                return lb || rb;
            }
        }
        
        if (left == null || right == null) {
            throw new IllegalStateException("Relational comparison with null");
        }

        double lv;
        double rv;

        if (left instanceof Character lc) {
            lv = lc;
        } else if (left instanceof Number ln) {
            lv = ln.doubleValue();
        } else {
            throw new IllegalStateException("Unsupported left type for " + op + ": " + left.getClass());
        }

        if (right instanceof Character rc) {
            rv = rc;
        } else if (right instanceof Number rn) {
            rv = rn.doubleValue();
        } else {
            throw new IllegalStateException("Unsupported right type for " + op + ": " + right.getClass());
        }

        return switch (op) {
            case "LT" -> lv < rv;
            case "LE" -> lv <= rv;
            case "GT" -> lv > rv;
            case "GE" -> lv >= rv;
            default -> throw new IllegalStateException("Unknown comparison op: " + op);
        };
    }

    @SuppressWarnings("unused")
    public static String beginArithmetic(String op, String sourceId, int line) {
        SpanCtx frame = currentFrameCtx();

        String eventId = Ids.nextEventId();
        TraceLoc loc = new TraceLoc(sourceId, line);

        ArithmeticCtx ctx = new ArithmeticCtx(eventId, frame.spanId(), loc, op);

        CTX_STACK.get().push(ctx);

        return eventId;
    }

    @SuppressWarnings("unused")
    public static String beginArithmeticLeft() {
        ArithmeticCtx ctx = findTopArithmeticCtx();
        if (ctx == null) {
            throw new IllegalStateException("No active ArithmeticCtx found");
        }

        CTX_STACK.get().push(new ArithmeticPhaseCtx(ctx, ArithmeticPhase.LEFT));
        return ctx.getArithmeticEventId();
    }

    @SuppressWarnings("unused")
    public static <T> T endArithmeticLeft(String arithmeticEventId, T value) {
        Deque<ExecCtx> stack = CTX_STACK.get();
        if (!stack.isEmpty() && stack.peek() instanceof ArithmeticPhaseCtx apc && apc.getPhase() == ArithmeticPhase.LEFT) {
            stack.pop();
        }

        return value;
    }

    @SuppressWarnings("unused")
    public static String beginArithmeticRight() {
        ArithmeticCtx ctx = findTopArithmeticCtx();
        if (ctx == null) {
            throw new IllegalStateException("No active ArithmeticCtx found");
        }

        CTX_STACK.get().push(new ArithmeticPhaseCtx(ctx, ArithmeticPhase.RIGHT));

        return ctx.getArithmeticEventId();
    }

    @SuppressWarnings("unused")
    public static <T> T endArithmeticRight(String arithmeticEventId, T value) {
        Deque<ExecCtx> stack = CTX_STACK.get();
        if (!stack.isEmpty() && stack.peek() instanceof ArithmeticPhaseCtx apc && apc.getPhase() == ArithmeticPhase.RIGHT) {
            stack.pop();
        }

        return value;
    }

    @SuppressWarnings("unused")
    public static <T> T endArithmetic(String arithmeticEventId, Object left, Object right, T result) {
        Deque<ExecCtx> stack = CTX_STACK.get();

        while (!stack.isEmpty() && stack.peek() instanceof ArithmeticPhaseCtx) {
            stack.pop();
        }

        ArithmeticCtx found = null;
        for (ExecCtx ctx : stack) {
            if (ctx instanceof ArithmeticCtx ac && ac.getArithmeticEventId().equals(arithmeticEventId)) {
                found = ac;
                break;
            }
        }

        if (found == null) {
            throw new IllegalStateException("No ArithmeticCtx for id " + arithmeticEventId);
        }

        stack.remove(found);

        String leftEventId = found.getLeftEventIds().isEmpty() ? null : found.getLeftEventIds().getLast();
        String rightEventId = found.getRightEventIds().isEmpty() ? null : found.getRightEventIds().getLast();

        ArithmeticEvent ev = new ArithmeticEvent(
            found.getArithmeticEventId(), found.getSpanId(),
            found.getLocation(), found.getOperation(),
            EventValue.of(left), leftEventId,
            EventValue.of(right), rightEventId,
            EventValue.of(result)
        );

        addEvent(ev, true);

        return result;
    }

    private static ArithmeticCtx findTopArithmeticCtx() {
        Deque<ExecCtx> stack = CTX_STACK.get();
        for (ExecCtx ctx : stack) {
            if (ctx instanceof ArithmeticCtx ac) {
                return ac;
            }
        }
        return null;
    }

    @SuppressWarnings("unused")
    public static String beginLocal(String label, String varName, String sourceId, int line) {
        SpanCtx frame = currentFrameCtx();

        String eventId = Ids.nextEventId();
        TraceLoc loc = new TraceLoc(sourceId, line);
        LocalCtx ctx = new LocalCtx(eventId, frame.spanId(), frame.methodId(), loc, varName, label);

        CTX_STACK.get().push(ctx);

        return eventId;
    }

    @SuppressWarnings("unused")
    public static <T> T endLocal(String localEventId, T value) {
        Deque<ExecCtx> stack = CTX_STACK.get();
        LocalCtx found = null;

        while (!stack.isEmpty()) {
            ExecCtx ctx = stack.pop();
            if (ctx instanceof LocalCtx lc && lc.getLocalEventId().equals(localEventId)) {
                found = lc;
                break;
            }
        }

        if (found == null) {
            throw new IllegalStateException("No LocalCtx for id " + localEventId);
        }

        String rootEventId = null;
        if (!found.getChildEventIds().isEmpty()) {
            rootEventId = found.getChildEventIds().getLast();
        }

        LocalEvent ev = new LocalEvent(
            found.getLocalEventId(), found.getSpanId(),
            found.getLocation(), found.getMethodId(), found.getVarName(),
            EventValue.of(value),
            found.getLabel(), rootEventId
        );

        addEvent(ev, true);

        return value;
    }

    @SuppressWarnings("unused")
    public static String beginFieldWrite(Object target, String fieldName, String sourceId, int line) {
        SpanCtx frame = currentFrameCtx();
        TraceLoc loc = new TraceLoc(sourceId, line);

        String objectId = EventValue.getOrCreateObjectId(target);
        String fieldType = resolveFieldType(target, fieldName, null);
        String eventId = Ids.nextEventId();

        FieldWriteCtx ctx = new FieldWriteCtx(
            eventId, frame.spanId(),
            loc, objectId,
            fieldName, fieldType
        );

        CTX_STACK.get().push(ctx);

        return eventId;
    }

    @SuppressWarnings("unused")
    public static <T> T endFieldWrite(String fieldWriteEventId, T value) {
        Deque<ExecCtx> stack = CTX_STACK.get();
        FieldWriteCtx found = null;

        while (!stack.isEmpty()) {
            ExecCtx ctx = stack.pop();
            if (ctx instanceof FieldWriteCtx fw && fw.getFieldWriteEventId().equals(fieldWriteEventId)) {
                found = fw;
                break;
            }
        }

        if (found == null) {
            throw new IllegalStateException("No FieldWriteCtx for id " + fieldWriteEventId);
        }

        String bodyEventId = null;
        if (!found.getChildEventIds().isEmpty()) {
            bodyEventId = found.getChildEventIds().getLast();
        }

        FieldWriteEvent ev = new FieldWriteEvent(
            found.getFieldWriteEventId(), found.getSpanId(),
            found.getLocation(), found.getObjectId(),
            found.getFieldName(), EventValue.of(value), found.getFieldType(),
            bodyEventId
        );

        addEvent(ev, true);

        return value;
    }

    @SuppressWarnings("unused")
    public static String beginReturn(String sourceId, int line) {
        var frameStack = SPAN_STACK.get();
        if (frameStack.isEmpty()) {
            return null;
        }

        SpanCtx spanCtx = frameStack.peek();
        String evId = Ids.nextEventId();
        TraceLoc loc = new TraceLoc(sourceId, line);

        ReturnCtx ctx = new ReturnCtx(evId, spanCtx.spanId(), loc);

        CTX_STACK.get().push(ctx);

        return evId;
    }

    @SuppressWarnings("unused")
    public static <T> T endReturn(String returnEventId, T ret, boolean isVoid) {
        var frameStack = SPAN_STACK.get();
        if (frameStack.isEmpty()) {
            return ret;
        }

        SpanCtx spanCtx = frameStack.pop();

        Deque<ExecCtx> stack = CTX_STACK.get();
        ReturnCtx found = null;

        while (!stack.isEmpty()) {
            ExecCtx ctx = stack.pop();
            if (ctx instanceof ReturnCtx rc && rc.getReturnEventId().equals(returnEventId)) {
                found = rc;
                break;
            }
        }

        if (found == null) {
            throw new IllegalStateException("No ReturnCtx for id " + returnEventId);
        }

        String bodyEventId = null;
        if (!found.getChildEventIds().isEmpty()) {
            bodyEventId = found.getChildEventIds().getLast();
        }

        TraceLoc loc = found.getLocation();
        EventValue value = isVoid ? null : EventValue.of(ret);

        addEvent(new ReturnEvent(found.getReturnEventId(), spanCtx.spanId(), loc, value, bodyEventId), !isVoid);

        MethodCtx methodCtx = popMethodCtxForSpan(spanCtx.spanId());

        if (methodCtx != null) {
            patchCall(methodCtx.getCallEventId(), value, methodCtx.getBodyEventIds());
        }

        patchSpanEnd(spanCtx.spanId(), found.getReturnEventId(), loc);

        return ret;
    }

    @SuppressWarnings("unused")
    public static String beginCondition(String sourceId, int line, String conditionKind) {
        SpanCtx ctx = currentFrameCtx();
        String eventId = Ids.nextEventId();
        TraceLoc loc = new TraceLoc(sourceId, line);

        ConditionKind kind = ConditionKind.valueOf(conditionKind);
        addEvent(new ConditionEvent(eventId, ctx.spanId(), loc, kind, new String[0], new String[0], new String[0], null), true);

        ConditionCtx conditionCtx = new ConditionCtx(eventId);
        CTX_STACK.get().push(conditionCtx);
        CTX_STACK.get().push(new ConditionPhaseCtx(conditionCtx, ConditionPhase.CONDITION_EXPR));

        return eventId;
    }

    public static <T> T endCondition(String conditionEventId, T value) {
        Deque<ExecCtx> stack = CTX_STACK.get();

        if (!stack.isEmpty() && stack.peek() instanceof ConditionPhaseCtx) {
            stack.pop();
        }

        if (!stack.isEmpty() && stack.peek() instanceof ConditionCtx c && Objects.equals(c.getConditionEventId(), conditionEventId)) {
            ConditionCtx condCtx = (ConditionCtx) stack.pop();
            condCtx.setValue(EventValue.of(value));

            patchCondition(conditionEventId, condCtx);
        }

        return value;
    }

    @SuppressWarnings("unused")
    public static void beginThenBlock(String conditionEventId) {
        ConditionCtx condCtx = findConditionCtx(conditionEventId);

        if (condCtx == null) {
            throw new IllegalStateException("No condition context for id " + conditionEventId);
        }

        CTX_STACK.get().push(new ConditionPhaseCtx(condCtx, ConditionPhase.THEN_BLOCK));
    }

    @SuppressWarnings("unused")
    public static void endThenBlock(String conditionEventId) {
        popPhase(ConditionPhase.THEN_BLOCK);
    }

    @SuppressWarnings("unused")
    public static void beginElseBlock(String conditionEventId) {
        ConditionCtx condCtx = findConditionCtx(conditionEventId);

        if (condCtx == null) {
            throw new IllegalStateException("No condition context for id " + conditionEventId);
        }

        CTX_STACK.get().push(new ConditionPhaseCtx(condCtx, ConditionPhase.ELSE_BLOCK));
    }

    @SuppressWarnings("unused")
    public static void endElseBlock(String conditionEventId) {
        popPhase(ConditionPhase.ELSE_BLOCK);
    }

    private static ConditionCtx findConditionCtx(String conditionEventId) {
        Deque<ExecCtx> stack = CTX_STACK.get();

        for (ExecCtx ctx : stack) {
            if (ctx instanceof ConditionCtx c && Objects.equals(c.getConditionEventId(), conditionEventId)) {
                return c;
            }
        }

        return null;
    }

    private static void popPhase(ConditionPhase phase) {
        Deque<ExecCtx> stack = CTX_STACK.get();

        if (!stack.isEmpty() && stack.peek() instanceof ConditionPhaseCtx cp && cp.getPhase() == phase) {
            stack.pop();
        }
    }

    @SuppressWarnings("unused")
    public static String beginLoop(String sourceId, int line, String kindName) {
        SpanCtx frame = currentFrameCtx();
        String loopEventId = Ids.nextEventId();
        TraceLoc loc = new TraceLoc(sourceId, line);
        LoopKind loopKind = LoopKind.valueOf(kindName);

        CTX_STACK.get().push(new LoopCtx(loopEventId, frame.spanId(), loc, loopKind));

        return loopEventId;
    }

    @SuppressWarnings("unused")
    public static void endLoop(String loopEventId) {
        Deque<ExecCtx> stack = CTX_STACK.get();

        while (!stack.isEmpty()) {
            ExecCtx ctx = stack.pop();
            if (ctx instanceof LoopCtx loopCtx && Objects.equals(loopCtx.getLoopEventId(), loopEventId)) {

                String[] initEventIds = loopCtx.getInitEventIds().toArray(String[]::new);
                String[] iterationEventIds = loopCtx.getIterationEventIds().toArray(String[]::new);

                LoopEvent loopEvent = new LoopEvent(
                    loopCtx.getLoopEventId(), loopCtx.getSpanId(),
                    loopCtx.getLocation(), loopCtx.getKind(),
                    initEventIds, iterationEventIds
                );

                addEvent(loopEvent, true);

                break;
            }
        }
    }

    @SuppressWarnings("unused")
    public static void beginLoopCondition(String loopEventId) {
        LoopCtx loopCtx = findLoopCtx(loopEventId);

        if (loopCtx == null) {
            throw new IllegalStateException("No loop context for id " + loopEventId);
        }

        loopCtx.getPendingConditionEvents().clear();

        CTX_STACK.get().push(new LoopPhaseCtx(loopCtx, null, LoopPhase.CONDITION));
    }

    @SuppressWarnings("unused")
    public static void endLoopCondition(String loopEventId) {
        Deque<ExecCtx> stack = CTX_STACK.get();

        if (!stack.isEmpty() && stack.peek() instanceof LoopPhaseCtx lp &&
                lp.getPhase() == LoopPhase.CONDITION) {
            stack.pop();
        }
    }

    @SuppressWarnings("unused")
    public static void beginLoopInit(String loopEventId) {
        LoopCtx loopCtx = findLoopCtx(loopEventId);

        if (loopCtx == null) {
            throw new IllegalStateException("No loop context for id " + loopEventId);
        }

        CTX_STACK.get().push(new LoopPhaseCtx(loopCtx, null, LoopPhase.INIT));
    }

    @SuppressWarnings("unused")
    public static void endLoopInit(String loopEventId) {
        Deque<ExecCtx> stack = CTX_STACK.get();

        if (!stack.isEmpty() && stack.peek() instanceof LoopPhaseCtx lp &&
                lp.getPhase() == LoopPhase.INIT) {
            stack.pop();
        }
    }

    @SuppressWarnings("unused")
    public static String beginLoopIteration(String loopEventId) {
        LoopCtx loopCtx = findLoopCtx(loopEventId);

        if (loopCtx == null) {
            throw new IllegalStateException("Loop iteration without active loop context");
        }

        int idx = loopCtx.getNextIterationIndex();
        loopCtx.setNextIterationIndex(idx + 1);

        String iterEventId = Ids.nextEventId();

        LoopIterationCtx iterCtx = new LoopIterationCtx(loopCtx, idx, iterEventId);
        iterCtx.getConditionEventIds().addAll(loopCtx.getPendingConditionEvents());

        CTX_STACK.get().push(iterCtx);
        CTX_STACK.get().push(new LoopPhaseCtx(loopCtx, iterCtx, LoopPhase.BODY));

        return iterEventId;
    }

    @SuppressWarnings("unused")
    public static void endLoopIteration(String iterationEventId) {
        Deque<ExecCtx> stack = CTX_STACK.get();

        while (!stack.isEmpty() && stack.peek() instanceof LoopPhaseCtx) {
            stack.pop();
        }

        if (stack.isEmpty() || !(stack.peek() instanceof LoopIterationCtx iterCtx) || !Objects.equals(iterCtx.getIterationEventId(), iterationEventId)) {
            throw new IllegalStateException("Invalid loop iteration end");
        }

        iterCtx = (LoopIterationCtx) stack.pop();
        LoopCtx loopCtx = iterCtx.getLoopCtx();

        String[] condIds = iterCtx.getConditionEventIds().toArray(String[]::new);
        String[] bodyIds = iterCtx.getBodyEventIds().toArray(String[]::new);
        String[] updateIds = iterCtx.getUpdateEventIds().toArray(String[]::new);

        LoopIterationEvent iterEvent = new LoopIterationEvent(
            iterCtx.getIterationEventId(), loopCtx.getSpanId(),
            loopCtx.getLocation(), iterCtx.getIterationIndex(),
            condIds, bodyIds, updateIds
        );

        addEvent(iterEvent, true);
        loopCtx.getIterationEventIds().add(iterCtx.getIterationEventId());
    }

    @SuppressWarnings("unused")
    public static void beginLoopUpdate(String loopEventId) {
        LoopCtx loopCtx = findLoopCtx(loopEventId);

        if (loopCtx == null) {
            throw new IllegalStateException("No loop context for id " + loopEventId);
        }

        LoopIterationCtx iterCtx = getCurrentIterationCtx(loopCtx);

        if (iterCtx == null) {
            throw new IllegalStateException("No loop iteration context for update");
        }

        CTX_STACK.get().push(new LoopPhaseCtx(loopCtx, iterCtx, LoopPhase.UPDATE));
    }

    @SuppressWarnings("unused")
    public static void endLoopUpdate(String loopEventId) {
        Deque<ExecCtx> stack = CTX_STACK.get();

        if (!stack.isEmpty() && stack.peek() instanceof LoopPhaseCtx lp && lp.getPhase() == LoopPhase.UPDATE) {
            stack.pop();
        }
    }

    private static LoopCtx findLoopCtx(String loopEventId) {
        Deque<ExecCtx> stack = CTX_STACK.get();
        for (ExecCtx ctx : stack) {
            if (ctx instanceof LoopCtx lc &&
                    Objects.equals(lc.getLoopEventId(), loopEventId)) {
                return lc;
            }
        }
        return null;
    }

    private static LoopIterationCtx getCurrentIterationCtx(LoopCtx loopCtx) {
        Deque<ExecCtx> stack = CTX_STACK.get();
        for (ExecCtx ctx : stack) {
            if (ctx instanceof LoopIterationCtx li &&
                    li.getLoopCtx() == loopCtx) {
                return li;
            }
        }
        return null;
    }

    private static void addEvent(TraceEvent event, boolean shouldAddInFile)
    {
        if (shouldAddInFile) {
            OutputManager.getTraceFileBuilder().addEvent(event);
        }

        Deque<ExecCtx> stack = CTX_STACK.get();

        for (ExecCtx ctx : stack) {
            if (ctx.handleEvent(event)) {
                break;
            }
        }

        if (shouldAddInFile) {
            OutputManager.getIndexFileBuilder().addEvent(event);
            OutputManager.getStateFileBuilder().onEvent(event);
        }
    }

    private static SpanCtx currentFrameCtx() {
        return SPAN_STACK.get().peek();
    }

    private static void patchSpanEnd(String spanId, String endEventId, TraceLoc endLoc) {
        TraceFile.Builder traceFileBuilder = OutputManager.getTraceFileBuilder();
        for (int i = traceFileBuilder.getSpans().size() - 1; i >= 0; i--) {
            TraceSpan s = traceFileBuilder.getSpans().get(i);
            if (s.spanId().equals(spanId) && s.endEventId().equals("end")) {
                traceFileBuilder.getSpans().set(i, new TraceSpan(
                    s.spanId(), s.parentSpanId(), s.methodId(),
                    s.thisRef(), s.args(),
                    s.startEventId(), endEventId, s.startLoc(), endLoc
                ));
                return;
            }
        }
    }

    private static void patchCondition(String conditionEventId, ConditionCtx condCtx) {
        TraceFile.Builder traceFileBuilder = OutputManager.getTraceFileBuilder();
        List<TraceEvent> events = traceFileBuilder.getEvents();

        String[] condIds = condCtx.getConditionEventIds().toArray(String[]::new);
        String[] thenIds = condCtx.getThenEventIds().toArray(String[]::new);
        String[] elseIds = condCtx.getElseEventIds().toArray(String[]::new);
        EventValue value = condCtx.getValue();

        for (int i = events.size() - 1; i >= 0; i--) {
            TraceEvent e = events.get(i);
            if (e instanceof ConditionEvent ce && ce.eventId().equals(conditionEventId)) {
                ConditionEvent patched = new ConditionEvent(
                    ce.eventId(), ce.spanId(), ce.location(), ce.kind(),
                    condIds, thenIds, elseIds,
                    value
                );

                events.set(i, patched);

                return;
            }
        }
    }

    private static void patchCall(String callEventId, EventValue returnValue, List<String> eventIds) {
        TraceFile.Builder traceFileBuilder = OutputManager.getTraceFileBuilder();
        List<TraceEvent> events = traceFileBuilder.getEvents();

        String[] eventIdArray = eventIds.toArray(String[]::new);

        for (int i = events.size() - 1; i >= 0; i--) {
            TraceEvent e = events.get(i);
            if (e instanceof CallEvent ce && ce.eventId().equals(callEventId)) {
                CallEvent patched = new CallEvent(
                    ce.eventId(), ce.spanId(),
                    ce.location(), ce.callerMethodId(), ce.calleeMethodId(),
                    ce.name(), ce.external(), ce.args(),
                    returnValue, eventIdArray
                );
                
                events.set(i, patched);
                
                return;
            }
        }
    }
    
    private static MethodCtx popMethodCtxForSpan(String spanId) {
        Deque<ExecCtx> stack = CTX_STACK.get();
        MethodCtx found = null;

        for (ExecCtx ctx : stack) {
            if (ctx instanceof MethodCtx mc && mc.getSpanId().equals(spanId)) {
                found = mc;
                
                break;
            }
        }

        if (found != null) {
            stack.remove(found);
        }

        return found;
    }

    private static String resolveFieldType(Object target, String fieldName, Object value) {
        if (target != null) {
            Class<?> c = target.getClass();
            try {
                var f = c.getDeclaredField(fieldName);
                return f.getType().getTypeName();
            } catch (NoSuchFieldException e) {
                throw new IllegalStateException("Could not find field " + fieldName + " in class " + c.getTypeName(), e);
            }
        }

        if (value != null) {
            return value.getClass().getTypeName();
        }

        return "java.lang.Object";
    }
}
