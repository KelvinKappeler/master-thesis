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

    private static final ThreadLocal<Deque<FrameCtx>> STACK = ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<Deque<BlockCtx>> BLOCKS = ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<Deque<CondBlockCtx>> COND_BLOCKS = ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<Deque<LoopCtx>> LOOPS = ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<Deque<LoopIterationCtx>> LOOP_ITER_STACK = ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<Deque<String>> LOOP_COND_STACK = ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<Deque<String>> LOOP_INIT_STACK = ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<Deque<String>> LOOP_UPDATE_STACK = ThreadLocal.withInitial(ArrayDeque::new);

    private TraceOut() {}

    @SuppressWarnings("unused")
    public static void onEnter(String owner, String method, Arg[] args, String returnType, String sourceId, int line) {
        String frameId = Ids.nextFrameId();
        String spanId = Ids.nextSpanId();
        String[] argsTypes = Arrays.stream(args).map(Arg::type).toArray(String[]::new);
        String methodId = Ids.createNewMethodId(owner, method, argsTypes, returnType);
        String parent = STACK.get().isEmpty() ? "null" : currentFrameCtx().spanId();
        String startEventId = Ids.nextEventId();
        TraceLoc loc = new TraceLoc(sourceId, line);

        addEvent(new CallEvent(startEventId, spanId, frameId, loc, parent, methodId, method, false, args));

        OutputManager.getTraceFileBuilder().addSpan(new TraceSpan(
            spanId, parent, methodId, startEventId, "end",
            loc, new TraceLoc("source", 1))
        );
        OutputManager.getTraceFileBuilder().addFrame(new TraceFrame(frameId, spanId, methodId, null, List.of(args)));

        STACK.get().push(new FrameCtx(spanId, frameId, methodId));
    }

    @SuppressWarnings("unused")
    public static void onReturn(Object ret, String sourceId, int line) {
        var stack = STACK.get();
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
    public static <T> T recordLocalEvent(String label, T value, String sourceId, int line) {
        FrameCtx ctx = currentFrameCtx();
        TraceLoc loc = new TraceLoc(sourceId, line);

        addEvent(new LocalEvent(
            Ids.nextEventId(), ctx.spanId(), ctx.frameId(), loc, "owner", ctx.methodId(), label, value
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
    public static <T> T recordArrayStore(String arrayName, int index, T value, String sourceId, int line) {
        FrameCtx ctx = currentFrameCtx();
        TraceLoc loc = new TraceLoc(sourceId, line);

        addEvent(new ArrayStoreEvent(Ids.nextEventId(), ctx.spanId(), ctx.frameId(), loc, arrayName, index, value));

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
    public static Object recordArrayInit(String arrayName, Object arrayRef, String sourceId, int line) {
        FrameCtx ctx = currentFrameCtx();
        TraceLoc loc = new TraceLoc(sourceId, line);

        int length = Array.getLength(arrayRef);
        for (int i = 0; i < length; i++) {
            Object value = Array.get(arrayRef, i);
            addEvent(new ArrayStoreEvent(
                Ids.nextEventId(), ctx.spanId(), ctx.frameId(), loc, arrayName, i, value
            ));
        }

        return arrayRef;
    }

    @SuppressWarnings("unused")
    public static void beginBlock(String parentEventId) {
        BLOCKS.get().push(new BlockCtx(parentEventId, new ArrayList<>()));
    }

    @SuppressWarnings("unused")
    public static void endBlock(String parentEventId) {
        Deque<BlockCtx> blocks = BLOCKS.get();
        if (blocks.isEmpty()) return;

        BlockCtx block = blocks.pop();
        if (!Objects.equals(block.parentEventId(), parentEventId)) {
            throw new IllegalStateException("Invalid block end");
        }

        patchBlockEvents(parentEventId, block.eventIds());
    }

    @SuppressWarnings("unused")
    public static String beginCondition(String sourceId, int line) {
        FrameCtx ctx = currentFrameCtx();
        String eventId = Ids.nextEventId();
        TraceLoc loc = new TraceLoc(sourceId, line);

        addEvent(new ConditionEvent(eventId, ctx.spanId(), ctx.frameId(), loc, new String[0], new String[0], new String[0]));

        COND_BLOCKS.get().push(new CondBlockCtx(eventId, new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));

        return eventId;
    }

    @SuppressWarnings("unused")
    public static void endCondition(String conditionEventId) {
        Deque<CondBlockCtx> stack = COND_BLOCKS.get();
        if (stack.isEmpty()) return;

        CondBlockCtx ctx = stack.pop();
        if (!Objects.equals(ctx.conditionEventId(), conditionEventId)) {
            throw new IllegalStateException("Invalid condition end");
        }

        patchCondition(conditionEventId, ctx.conditionEvents(), ctx.thenEvents(), ctx.elseEvents());
    }

    @SuppressWarnings("unused")
    public static void beginThenBlock(String conditionEventId) {
        BLOCKS.get().push(new BlockCtx(conditionEventId, new ArrayList<>()));
    }

    @SuppressWarnings("unused")
    public static void endThenBlock(String conditionEventId) {
        Deque<BlockCtx> blocks = BLOCKS.get();
        if (blocks.isEmpty()) return;

        BlockCtx block = blocks.pop();
        if (!Objects.equals(block.parentEventId(), conditionEventId)) {
            throw new IllegalStateException("Invalid then block end");
        }

        attachThenEvents(conditionEventId, block.eventIds());

        Deque<CondBlockCtx> condStack = COND_BLOCKS.get();
        for (CondBlockCtx c : condStack) {
            if (Objects.equals(c.conditionEventId(), conditionEventId)) {
                c.thenEvents().addAll(block.eventIds());
                break;
            }
        }
    }

    @SuppressWarnings("unused")
    public static void beginElseBlock(String conditionEventId) {
        BLOCKS.get().push(new BlockCtx(conditionEventId, new ArrayList<>()));
    }

    @SuppressWarnings("unused")
    public static void endElseBlock(String conditionEventId) {
        Deque<BlockCtx> blocks = BLOCKS.get();
        if (blocks.isEmpty()) return;

        BlockCtx block = blocks.pop();
        if (!Objects.equals(block.parentEventId(), conditionEventId)) {
            throw new IllegalStateException("Invalid else block end");
        }

        attachElseEvents(conditionEventId, block.eventIds());

        Deque<CondBlockCtx> condStack = COND_BLOCKS.get();
        for (CondBlockCtx c : condStack) {
            if (Objects.equals(c.conditionEventId(), conditionEventId)) {
                c.elseEvents().addAll(block.eventIds());
                break;
            }
        }
    }

    @SuppressWarnings("unused")
    public static String beginLoop(String sourceId, int line, String kindName) {
        FrameCtx frame = currentFrameCtx();
        String loopEventId = Ids.nextEventId();
        TraceLoc loc = new TraceLoc(sourceId, line);
        LoopKind loopKind = LoopKind.valueOf(kindName);

        LoopCtx loopCtx = new LoopCtx(
            loopEventId, frame.spanId(), frame.frameId(),
            loc, loopKind, 0,
            new ArrayList<>(), new ArrayList<>(), new ArrayList<>()
        );

        LOOPS.get().push(loopCtx);
        return loopEventId;
    }

    @SuppressWarnings("unused")
    public static void endLoop(String loopEventId) {
        Deque<LoopCtx> loops = LOOPS.get();
        if (loops.isEmpty()) return;

        LoopCtx ctx = loops.peek();
        if (!Objects.equals(ctx.loopEventId(), loopEventId)) {
            throw new IllegalStateException("Invalid loop end");
        }

        if (!ctx.pendingConditionEvents().isEmpty()) {
            String[] condIds = ctx.pendingConditionEvents().toArray(String[]::new);
            String[] bodyIds = new String[0];
            String[] updateIds = new String[0];

            String iterEventId = Ids.nextEventId();

            LoopIterationEvent exitIteration = new LoopIterationEvent(
                iterEventId, ctx.spanId(), ctx.frameId(),
                ctx.location(), ctx.nextIterationIndex(),
                condIds, bodyIds, updateIds
            );

            addEvent(exitIteration);
            ctx.iterationEventIds().add(iterEventId);

            ctx.pendingConditionEvents().clear();
        }

        loops.pop();

        String[] initEventIds = ctx.initEventIds().toArray(String[]::new);
        String[] iterationEventIds = ctx.iterationEventIds().toArray(String[]::new);

        LoopEvent loopEvent = new LoopEvent(
            ctx.loopEventId(), ctx.spanId(), ctx.frameId(),
            ctx.location(), ctx.kind(),
            initEventIds, iterationEventIds
        );

        addEvent(loopEvent);
    }

    @SuppressWarnings("unused")
    public static String beginLoopIteration(String loopEventId) {
        Deque<LoopCtx> loops = LOOPS.get();
        LoopCtx ctx = null;
        for (LoopCtx c : loops) {
            if (Objects.equals(c.loopEventId(), loopEventId)) {
                ctx = c;
                break;
            }
        }
        if (ctx == null) {
            throw new IllegalStateException("Loop iteration without active loop context");
        }

        int iterationIndex = ctx.nextIterationIndex();
        loops.remove(ctx);
        loops.push(new LoopCtx(
            ctx.loopEventId(), ctx.spanId(), ctx.frameId(),
            ctx.location(), ctx.kind(),
            iterationIndex + 1,
            ctx.initEventIds(), ctx.iterationEventIds(), ctx.pendingConditionEvents()
        ));

        String iterEventId = Ids.nextEventId();

        LoopIterationCtx iterCtx = new LoopIterationCtx(
            loopEventId, iterEventId, iterationIndex,
            new ArrayList<>(ctx.pendingConditionEvents()), new ArrayList<>(), new ArrayList<>()
        );
        LOOP_ITER_STACK.get().push(iterCtx);

        return iterEventId;
    }

    @SuppressWarnings("unused")
    public static void endLoopIteration(String iterationEventId) {
        Deque<LoopIterationCtx> iterStack = LOOP_ITER_STACK.get();
        if (iterStack.isEmpty()) {
            return;
        }

        LoopIterationCtx iterCtx = iterStack.pop();
        if (!Objects.equals(iterCtx.iterationEventId(), iterationEventId)) {
            throw new IllegalStateException("Invalid loop iteration end");
        }

        Deque<LoopCtx> loops = LOOPS.get();
        LoopCtx loopCtx = null;
        for (LoopCtx c : loops) {
            if (Objects.equals(c.loopEventId(), iterCtx.loopEventId())) {
                loopCtx = c;
                break;
            }
        }
        if (loopCtx == null) {
            throw new IllegalStateException("Loop context not found for iteration");
        }

        String[] condIds = iterCtx.conditionEventIds().toArray(String[]::new);
        String[] bodyIds = iterCtx.bodyEventIds().toArray(String[]::new);
        String[] updateIds = iterCtx.updateEventIds().toArray(String[]::new);

        LoopIterationEvent iterEvent = new LoopIterationEvent(
            iterCtx.iterationEventId(), loopCtx.spanId(), loopCtx.frameId(),
            loopCtx.location(), iterCtx.iterationIndex(),
            condIds, bodyIds, updateIds
        );

        addEvent(iterEvent);
        loopCtx.iterationEventIds().add(iterCtx.iterationEventId());
    }

    @SuppressWarnings("unused")
    public static void beginLoopCondition(String loopEventId) {
        Deque<LoopCtx> loops = LOOPS.get();
        LoopCtx ctx = null;
        for (LoopCtx c : loops) {
            if (Objects.equals(c.loopEventId(), loopEventId)) {
                ctx = c;
                break;
            }
        }
        if (ctx == null) {
            throw new IllegalStateException("No loop context for id " + loopEventId);
        }

        ctx.pendingConditionEvents().clear();
        LOOP_COND_STACK.get().push(loopEventId);
    }

    @SuppressWarnings("unused")
    public static void endLoopCondition(String loopEventId) {
        Deque<String> stack = LOOP_COND_STACK.get();
        if (stack.isEmpty()) {
            return;
        }
        String top = stack.pop();
        if (!Objects.equals(top, loopEventId)) {
            throw new IllegalStateException("Invalid loop condition end");
        }
    }

    @SuppressWarnings("unused")
    public static void beginLoopInit(String loopEventId) {
        Deque<LoopCtx> loops = LOOPS.get();
        LoopCtx ctx = null;
        for (LoopCtx c : loops) {
            if (Objects.equals(c.loopEventId(), loopEventId)) {
                ctx = c;
                break;
            }
        }
        if (ctx == null) {
            throw new IllegalStateException("No loop context for id " + loopEventId);
        }
        LOOP_INIT_STACK.get().push(loopEventId);
    }

    @SuppressWarnings("unused")
    public static void endLoopInit(String loopEventId) {
        Deque<String> stack = LOOP_INIT_STACK.get();
        if (stack.isEmpty()) {
            return;
        }
        String top = stack.pop();
        if (!Objects.equals(top, loopEventId)) {
            throw new IllegalStateException("Invalid loop init end");
        }
    }

    @SuppressWarnings("unused")
    public static void beginLoopUpdate(String loopEventId) {
        Deque<LoopCtx> loops = LOOPS.get();
        LoopCtx ctx = null;
        for (LoopCtx c : loops) {
            if (Objects.equals(c.loopEventId(), loopEventId)) {
                ctx = c;
                break;
            }
        }
        if (ctx == null) {
            throw new IllegalStateException("No loop context for id " + loopEventId);
        }
        LOOP_UPDATE_STACK.get().push(loopEventId);
    }

    @SuppressWarnings("unused")
    public static void endLoopUpdate(String loopEventId) {
        Deque<String> stack = LOOP_UPDATE_STACK.get();
        if (stack.isEmpty()) {
            return;
        }
        String top = stack.pop();
        if (!Objects.equals(top, loopEventId)) {
            throw new IllegalStateException("Invalid loop update end");
        }
    }

    private static void addEvent(TraceEvent event)
    {
        OutputManager.getTraceFileBuilder().addEvent(event);

        Deque<String> initStack = LOOP_INIT_STACK.get();
        if (!initStack.isEmpty()) {
            String loopEventId = initStack.peek();
            Deque<LoopCtx> loops = LOOPS.get();
            for (LoopCtx c : loops) {
                if (Objects.equals(c.loopEventId(), loopEventId)) {
                    c.initEventIds().add(event.eventId());
                    break;
                }
            }
        }

        Deque<String> updateStack = LOOP_UPDATE_STACK.get();
        if (!updateStack.isEmpty()) {
            String loopEventId = updateStack.peek();
            Deque<LoopIterationCtx> iterStack = LOOP_ITER_STACK.get();
            for (LoopIterationCtx c : iterStack) {
                if (Objects.equals(c.loopEventId(), loopEventId)) {
                    c.updateEventIds().add(event.eventId());
                    break;
                }
            }
        }

        Deque<String> condLoopStack = LOOP_COND_STACK.get();
        if (!condLoopStack.isEmpty()) {
            String loopEventId = condLoopStack.peek();
            Deque<LoopCtx> loops = LOOPS.get();
            for (LoopCtx c : loops) {
                if (Objects.equals(c.loopEventId(), loopEventId)) {
                    c.pendingConditionEvents().add(event.eventId());
                    break;
                }
            }
        } else {
            Deque<LoopIterationCtx> iterStack = LOOP_ITER_STACK.get();
            if (!iterStack.isEmpty()
                    && !(event instanceof LoopIterationEvent)
                    && !(event instanceof LoopEvent)) {
                LoopIterationCtx iterCtx = iterStack.peek();
                if (iterCtx != null && LOOP_UPDATE_STACK.get().isEmpty()) {
                    iterCtx.bodyEventIds().add(event.eventId());
                }
            }
        }

        var condStack = COND_BLOCKS.get();
        var blockStack = BLOCKS.get();
        var loopsStack = LOOPS.get();

        boolean inCond = !condStack.isEmpty();
        boolean inBlock = !blockStack.isEmpty();
        boolean inLoop = !loopsStack.isEmpty();

        if (inCond && inBlock && blockStack.peek().parentEventId().equals(condStack.peek().conditionEventId())) {
            blockStack.peek().eventIds().add(event.eventId());
            OutputManager.getIndexFileBuilder().addEvent(event, false);

            return;
        }

        if (inCond) {
            condStack.peek().conditionEvents().add(event.eventId());
            OutputManager.getIndexFileBuilder().addEvent(event, false);

            return;
        }

        if (inBlock) {
            blockStack.peek().eventIds().add(event.eventId());
            OutputManager.getIndexFileBuilder().addEvent(event, false);

            return;
        }

        if (inLoop && !(event instanceof LoopEvent)) {
            OutputManager.getIndexFileBuilder().addEvent(event, false);

            return;
        }

        boolean indexAsRoot = !(event instanceof LoopIterationEvent);
        OutputManager.getIndexFileBuilder().addEvent(event, indexAsRoot);
    }

    private static FrameCtx currentFrameCtx() {
        return STACK.get().peek();
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

    private static void patchBlockEvents(String parentEventId, List<String> bodyEventsIds) {
        TraceFile.Builder traceFileBuilder = OutputManager.getTraceFileBuilder();
        List<TraceEvent> events = traceFileBuilder.getEvents();

        for (int i = events.size() - 1; i >= 0; i--) {
            TraceEvent e = events.get(i);
            if (e instanceof ConditionEvent ce && ce.eventId().equals(parentEventId)) {
                String[] existingConditionIds = ce.conditionEventIds() != null ? ce.conditionEventIds() : new String[0];

                ConditionEvent patched = new ConditionEvent(
                    ce.eventId(), ce.spanId(), ce.frameId(), ce.location(),
                    existingConditionIds, existingConditionIds, bodyEventsIds.toArray(String[]::new)
                );

                events.set(i, patched);

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

    private static void attachThenEvents(String conditionEventId, List<String> thenIds) {
        TraceFile.Builder traceFileBuilder = OutputManager.getTraceFileBuilder();
        List<TraceEvent> events = traceFileBuilder.getEvents();

        for (int i = events.size() - 1; i >= 0; i--) {
            TraceEvent e = events.get(i);
            if (e instanceof ConditionEvent ce && ce.eventId().equals(conditionEventId)) {
                ConditionEvent patched = new ConditionEvent(
                    ce.eventId(), ce.spanId(), ce.frameId(), ce.location(),
                    ce.conditionEventIds(),
                    thenIds.toArray(String[]::new),
                    ce.elseEventIds()
                );

                events.set(i, patched);

                return;
            }
        }
    }

    private static void attachElseEvents(String conditionEventId, List<String> elseIds) {
        TraceFile.Builder traceFileBuilder = OutputManager.getTraceFileBuilder();
        List<TraceEvent> events = traceFileBuilder.getEvents();

        for (int i = events.size() - 1; i >= 0; i--) {
            TraceEvent e = events.get(i);
            if (e instanceof ConditionEvent ce && ce.eventId().equals(conditionEventId)) {
                ConditionEvent patched = new ConditionEvent(
                    ce.eventId(), ce.spanId(), ce.frameId(), ce.location(),
                    ce.conditionEventIds(), ce.thenEventIds(), elseIds.toArray(String[]::new)
                );

                events.set(i, patched);

                return;
            }
        }
    }
}
