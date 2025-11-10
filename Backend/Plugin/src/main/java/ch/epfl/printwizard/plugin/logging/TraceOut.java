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

    private static void addEvent(TraceEvent event)
    {
        OutputManager.getTraceFileBuilder().addEvent(event);

        var condStack = COND_BLOCKS.get();
        var blockStack = BLOCKS.get();

        boolean inCond = !condStack.isEmpty();
        boolean inBlock = !blockStack.isEmpty();

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

        OutputManager.getIndexFileBuilder().addEvent(event, true);
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
