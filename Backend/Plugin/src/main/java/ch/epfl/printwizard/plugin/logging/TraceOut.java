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

    private static final ThreadLocal<Deque<FrameCtx>> STACK = ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<Deque<BlockCtx>> BLOCKS = ThreadLocal.withInitial(ArrayDeque::new);

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

        addEvent(new CallEvent(startEventId, spanId, frameId, loc, parent, methodId, method));
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
    public static <T> T recordLocalEvent(String label, T value, String sourceId, int line) {
        FrameCtx ctx = currentFrameCtx();
        TraceLoc loc = new TraceLoc(sourceId, line);

        addEvent(new LocalEvent(
            Ids.nextEventId(), ctx.spanId(), ctx.frameId(), loc, "owner", ctx.methodId(), label, -1, value
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
    public static String recordCondition(Object left, Object right, boolean result, String sourceId, int line) {
        FrameCtx ctx = currentFrameCtx();
        String eventId = Ids.nextEventId();
        TraceLoc loc = new TraceLoc(sourceId, line);

        addEvent(new ConditionEvent(
            eventId, ctx.spanId(), ctx.frameId(), loc, left, right, result, new String[0], new String[0]
        ));

        return eventId;
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

    private static void addEvent(TraceEvent event)
    {
        OutputManager.getTraceFileBuilder().addEvent(event);
        OutputManager.getIndexFileBuilder().addEvent(event);

        var blocks = BLOCKS.get();
        if (!blocks.isEmpty()) {
            blocks.peek().eventIds().add(event.eventId());
        }
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
                    ce.left(), ce.right(), ce.result(),
                    existingConditionIds, bodyEventsIds.toArray(String[]::new)
                );

                events.set(i, patched);

                return;
            }
        }
    }
}
