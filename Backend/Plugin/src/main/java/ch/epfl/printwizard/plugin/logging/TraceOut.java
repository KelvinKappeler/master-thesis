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

    private TraceOut() {}

    /**
     * This method is called when a method is entered.
     * @param owner the internal name of the class owning the method
     * @param method the name of the method
     * @param args the arguments passed to the method
     * @param returnType the return type of the method
     * @param loc the location where the method is called
     */
    @SuppressWarnings("unused")
    public static void onEnter(String owner, String method, Arg[] args, String returnType, TraceLoc loc) {
        String frameId = Ids.nextFrameId();
        String spanId = Ids.nextSpanId();
        String[] argsTypes = Arrays.stream(args).map(Arg::type).toArray(String[]::new);
        String methodId = Ids.createNewMethodId(owner, method, argsTypes, returnType);
        String parent = STACK.get().isEmpty() ? "null" : currentFrameCtx().spanId();
        String startEventId = Ids.nextEventId();

        addEvent(new CallEvent(startEventId, spanId, frameId, loc, parent, methodId, method));
        OutputManager.getTraceFileBuilder().addSpan(new TraceSpan(
            spanId, parent, methodId, startEventId, "end",
            loc, new TraceLoc("source", 1))
        );

        OutputManager.getTraceFileBuilder().addFrame(new TraceFrame(frameId, spanId, methodId, null, List.of(args)));
        STACK.get().push(new FrameCtx(spanId, frameId, methodId));
    }

    /**
     * Writes the collected trace data after a method exit
     * @param ret the return value of the method
     * @param loc the location where the method returns
     */
    @SuppressWarnings("unused")
    public static void onReturn(Object ret, TraceLoc loc) {
        var stack = STACK.get();
        if (stack.isEmpty()) return;
        var frameCtx = stack.pop();

        String evId = Ids.nextEventId();
        addEvent(new ReturnEvent(evId, frameCtx.spanId(), frameCtx.frameId(), loc, ret));

        patchSpanEnd(frameCtx.spanId(), evId, loc);
    }

    @SuppressWarnings("unused")
    public static <T> T recordLocalEvent(String label, T value, TraceLoc loc) {
        FrameCtx ctx = currentFrameCtx();

        addEvent(new LocalEvent(
            Ids.nextEventId(), ctx.spanId(), ctx.frameId(), loc, "owner", ctx.methodId(), label, -1, value
        ));

        return value;
    }

    @SuppressWarnings("unused")
    public static Object recordArithmetic(String op, Object left, Object right, Object result, TraceLoc loc) {
        FrameCtx ctx = currentFrameCtx();

        addEvent(new ArithmeticEvent(
            Ids.nextEventId(), ctx.spanId(), ctx.frameId(), loc, op, left, right, result
        ));

        return result;
    }

    private static void addEvent(TraceEvent event)
    {
        OutputManager.getTraceFileBuilder().addEvent(event);
        OutputManager.getIndexFileBuilder().addEvent(event);

        //var bs = BLOCKS.get();
        //if (!bs.isEmpty()) {
        //    bs.peek().eventIds().add(event.eventId());
        //}
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
}
