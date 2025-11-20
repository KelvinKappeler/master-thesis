package ch.epfl.printwizard.plugin.logging;

import ch.epfl.printwizard.plugin.model.trace.*;
import ch.epfl.printwizard.plugin.model.trace.events.*;
import ch.epfl.printwizard.plugin.utils.Ids;

import java.lang.reflect.Array;
import java.util.*;

/**
 * Represents a list of static methods used by the program to log data during its execution.
 */
@SuppressWarnings("unused")
public class TraceOut {

    private static final ThreadLocal<Deque<FrameCtx>> FRAME_STACK = ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<Deque<ExecCtx>> CTX_STACK = ThreadLocal.withInitial(ArrayDeque::new);

    private TraceOut() {}

    @SuppressWarnings("unused")
    public static void onEnter(String owner, String method, Arg[] args, String returnType, String sourceId, int line) {
        String frameId = Ids.nextFrameId();
        String spanId = Ids.nextSpanId();
        String[] argsTypes = Arrays.stream(args).map(Arg::type).toArray(String[]::new);
        String methodId = Ids.createNewMethodId(owner, method, argsTypes, returnType);
        String parent = FRAME_STACK.get().isEmpty() ? "null" : currentFrameCtx().spanId();
        String startEventId = Ids.nextEventId();
        TraceLoc loc = new TraceLoc(sourceId, line);

        addEvent(new CallEvent(startEventId, spanId, frameId, loc, parent, methodId, method, false, args));

        OutputManager.getTraceFileBuilder().addSpan(new TraceSpan(
            spanId, parent, methodId, startEventId, "end",
            loc, new TraceLoc("source", 1))
        );
        OutputManager.getTraceFileBuilder().addFrame(new TraceFrame(frameId, spanId, methodId, null, List.of(args)));

        FRAME_STACK.get().push(new FrameCtx(spanId, frameId, methodId));
    }

    @SuppressWarnings("unused")
    public static void onReturn(Object ret, String sourceId, int line) {
        var stack = FRAME_STACK.get();
        if (stack.isEmpty()) return;
        var frameCtx = stack.pop();

        String evId = Ids.nextEventId();
        TraceLoc loc = new TraceLoc(sourceId, line);
        addEvent(new ReturnEvent(evId, frameCtx.spanId(), frameCtx.frameId(), loc, ret));

        patchSpanEnd(frameCtx.spanId(), evId, loc);
    }

    @SuppressWarnings("unused")
    public static void recordCall(String owner, String method, Arg[] args, String returnType, boolean isExternal, String sourceId, int line) {
        FrameCtx ctx = currentFrameCtx();
        TraceLoc loc = new TraceLoc(sourceId, line);
        String[] argTypes = Arrays.stream(args).map(Arg::type).toArray(String[]::new);
        String eventId = Ids.nextEventId();
        String callerMethodId = ctx.methodId();
        String calleeMethodId = isExternal ? "-" : Ids.createNewMethodId(owner, method, argTypes, returnType);

        addEvent(new CallEvent(eventId, ctx.spanId(), ctx.frameId(), loc, callerMethodId, calleeMethodId, method, isExternal, args));
    }

    @SuppressWarnings("unused")
    public static <T> T recordLocalEvent(String label, String varName, T value, String sourceId, int line) {
        FrameCtx ctx = currentFrameCtx();
        TraceLoc loc = new TraceLoc(sourceId, line);

        addEvent(new LocalEvent(
            Ids.nextEventId(), ctx.spanId(), ctx.frameId(), loc, "owner", ctx.methodId(), varName, value, label
        ));

        return value;
    }

    @SuppressWarnings("unused")
    public static <T> T recordArithmetic(String op, Object left, Object right, T result, String sourceId, int line) {
        FrameCtx ctx = currentFrameCtx();
        TraceLoc loc = new TraceLoc(sourceId, line);

        addEvent(new ArithmeticEvent(
            Ids.nextEventId(), ctx.spanId(), ctx.frameId(), loc, op, left, right, result
        ));

        return result;
    }

    @SuppressWarnings("unused")
    public static <T> T recordArrayStore(String label, String arrayName, int index, T value, String sourceId, int line) {
        FrameCtx ctx = currentFrameCtx();
        TraceLoc loc = new TraceLoc(sourceId, line);

        addEvent(new ArrayStoreEvent(Ids.nextEventId(), ctx.spanId(), ctx.frameId(), loc, arrayName, index, value, label));

        return value;
    }

    @SuppressWarnings("unused")
    public static boolean recordComparison(String op, Object left, Object right, boolean result, String sourceId, int line) {
        FrameCtx ctx = currentFrameCtx();
        TraceLoc loc = new TraceLoc(sourceId, line);

        addEvent(new ComparisonEvent(
            Ids.nextEventId(), ctx.spanId(), ctx.frameId(), loc, op, left, right, result
        ));

        return result;
    }

    @SuppressWarnings("unused")
    public static Object recordArrayInit(String label, String arrayName, Object arrayRef, String sourceId, int line) {
        FrameCtx ctx = currentFrameCtx();
        TraceLoc loc = new TraceLoc(sourceId, line);

        int length = Array.getLength(arrayRef);
        for (int i = 0; i < length; i++) {
            Object value = Array.get(arrayRef, i);
            addEvent(new ArrayStoreEvent(
                Ids.nextEventId(), ctx.spanId(), ctx.frameId(), loc, arrayName, i, value, label
            ));
        }

        return arrayRef;
    }

    @SuppressWarnings("unused")
    public static String beginCondition(String sourceId, int line) {
        FrameCtx ctx = currentFrameCtx();
        String eventId = Ids.nextEventId();
        TraceLoc loc = new TraceLoc(sourceId, line);

        addEvent(new ConditionEvent(eventId, ctx.spanId(), ctx.frameId(), loc, new String[0], new String[0], new String[0]));

        ConditionCtx conditionCtx = new ConditionCtx(eventId);
        CTX_STACK.get().push(conditionCtx);
        CTX_STACK.get().push(new ConditionPhaseCtx(conditionCtx, ConditionPhase.CONDITION_EXPR));

        return eventId;
    }

    @SuppressWarnings("unused")
    public static void endCondition(String conditionEventId) {
        Deque<ExecCtx> stack = CTX_STACK.get();

        if (!stack.isEmpty() && stack.peek() instanceof ConditionPhaseCtx) {
            stack.pop();
        }

        if (!stack.isEmpty() && stack.peek() instanceof ConditionCtx c && Objects.equals(c.getConditionEventId(), conditionEventId)) {
            ConditionCtx condCtx = (ConditionCtx) stack.pop();

            patchCondition(conditionEventId, condCtx.getConditionEventIds(), condCtx.getThenEventIds(), condCtx.getElseEventIds());
        }
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
        FrameCtx frame = currentFrameCtx();
        String loopEventId = Ids.nextEventId();
        TraceLoc loc = new TraceLoc(sourceId, line);
        LoopKind loopKind = LoopKind.valueOf(kindName);

        CTX_STACK.get().push(new LoopCtx(loopEventId, frame.spanId(), frame.frameId(), loc, loopKind));

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
                    loopCtx.getLoopEventId(), loopCtx.getSpanId(), loopCtx.getFrameId(),
                    loopCtx.getLocation(), loopCtx.getKind(),
                    initEventIds, iterationEventIds
                );

                addEvent(loopEvent);

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
            iterCtx.getIterationEventId(), loopCtx.getSpanId(), loopCtx.getFrameId(),
            loopCtx.getLocation(), iterCtx.getIterationIndex(),
            condIds, bodyIds, updateIds
        );

        addEvent(iterEvent);
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

    private static void addEvent(TraceEvent event)
    {
        OutputManager.getTraceFileBuilder().addEvent(event);

        Deque<ExecCtx> stack = CTX_STACK.get();

        for (ExecCtx ctx : stack) {
            if (ctx.handleEvent(event)) {
                break;
            }
        }

        boolean indexAsRoot = !(event instanceof LoopEvent) && !(event instanceof LoopIterationEvent) && !(event instanceof ConditionEvent);

        if (indexAsRoot && !stack.isEmpty()) {
            for (ExecCtx ctx : stack) {
                if (ctx instanceof LoopCtx || ctx instanceof LoopIterationCtx || ctx instanceof ConditionCtx || ctx instanceof ConditionPhaseCtx || ctx instanceof LoopPhaseCtx) {
                    indexAsRoot = false;

                    break;
                }
            }
        }

        OutputManager.getIndexFileBuilder().addEvent(event, indexAsRoot);
    }

    private static FrameCtx currentFrameCtx() {
        return FRAME_STACK.get().peek();
    }

    private static void patchSpanEnd(String spanId, String endEventId, TraceLoc endLoc) {
        TraceFile.Builder traceFileBuilder = OutputManager.getTraceFileBuilder();
        for (int i = traceFileBuilder.getSpans().size() - 1; i >= 0; i--) {
            TraceSpan s = traceFileBuilder.getSpans().get(i);
            if (s.spanId().equals(spanId) && s.endEventId().equals("end")) {
                traceFileBuilder.getSpans().set(i, new TraceSpan(s.spanId(), s.parentSpanId(), s.methodId(), s.startEventId(), endEventId, s.startLoc(), endLoc));
                return;
            }
        }
    }

    private static void patchCondition(String conditionEventId, List<String> condIds, List<String> thenIds, List<String> elseIds) {
        TraceFile.Builder traceFileBuilder = OutputManager.getTraceFileBuilder();
        List<TraceEvent> events = traceFileBuilder.getEvents();

        for (int i = events.size() - 1; i >= 0; i--) {
            TraceEvent e = events.get(i);
            if (e instanceof ConditionEvent ce && ce.eventId().equals(conditionEventId)) {
                ConditionEvent patched = new ConditionEvent(
                    ce.eventId(), ce.spanId(), ce.frameId(), ce.location(),
                    condIds.toArray(String[]::new), thenIds.toArray(String[]::new), elseIds.toArray(String[]::new)
                );

                events.set(i, patched);

                return;
            }
        }
    }
}
