package ch.epfl.printwizard.agent;

import ch.epfl.printwizard.agent.utils.JvmDescriptor;
import ch.epfl.printwizard.shared.IdGenerator;
import ch.epfl.printwizard.shared.model.trace.*;
import ch.epfl.printwizard.shared.model.trace.events.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class TraceSink {
    private TraceSink() {}

    private static final TraceBuilder traceFileBuilder = new TraceBuilder();
    private static final IndexBuilder indexFileBuilder = new IndexBuilder();

    private static final ThreadLocal<Deque<FrameCtx>> STACK = ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<Deque<BlockCtx>> BLOCKS = ThreadLocal.withInitial(ArrayDeque::new);

    private static final ConcurrentHashMap<String, String> ownerToSourceId = new ConcurrentHashMap<>();

    private static int nextSpanId = 1;
    private static int nextEventId = 1;
    private static int nextFrameId = 1;

    /**
     * Register the source file for a given owner
     * @param ownerInternal the internal name of the owner
     * @param sourceFile the source file name
     */
    public static void registerSource(String ownerInternal, String sourceFile) {
        String packagePath = ownerInternal.substring(0, ownerInternal.lastIndexOf('/'));
        String path = packagePath + "/" + sourceFile;
        ownerToSourceId.put(ownerInternal, IdGenerator.sourceId(path));
    }

    /**
     * Returns the sourceId for a given owner
     * @param ownerInternal the internal name of the owner
     * @return the sourceId
     */
    public static String sourceIdForOwner(String ownerInternal, String sourceFile) {
        if (ownerToSourceId.containsKey(ownerInternal)) {
            return ownerToSourceId.get(ownerInternal);
        }

        String packagePath = ownerInternal.substring(0, ownerInternal.lastIndexOf('/'));
        String path = packagePath + "/" + sourceFile;
        String sourceId = IdGenerator.sourceId(path);
        ownerToSourceId.put(ownerInternal, sourceId);

        return sourceId;
    }

    /**
     * Writes the collected trace data before a call to a method
     * @param callerOwner Owner of the caller method
     * @param callerName Name of the caller method
     * @param callerDesc Descriptor of the caller method
     * @param calleeOwner Owner of the callee method
     * @param calleeName Name of the callee method
     * @param calleeDesc Descriptor of the callee method
     * @param sourceId Source ID where the call occurs
     * @param line Line number where the call occurs
     */
    public static void beforeCall(String callerOwner, String callerName, String callerDesc,
        String calleeOwner, String calleeName, String calleeDesc, String sourceId, int line) {
        
        String[] paramsAndTypeCaller = JvmDescriptor.toHuman(callerDesc);
        String[] paramsAndTypeCallee = JvmDescriptor.toHuman(calleeDesc);
        String caller = IdGenerator.methodId(callerOwner, callerName, paramsAndTypeCaller[0], paramsAndTypeCaller[1]);
        String callee = IdGenerator.methodId(calleeOwner, calleeName, paramsAndTypeCallee[0], paramsAndTypeCallee[1]);
        addEvent(new CallEvent(nextEventId(), topSpan(), topFrame(), new TraceLoc(sourceId, line),
                caller, callee, calleeName));
    }

    /**
     * Writes the collected trace data after a method entry
     * @param thisRef the 'this' reference of the method
     * @param args the arguments of the method
     * @param owner the owner of the method
     * @param name the name of the method
     * @param desc the descriptor of the method
     * @param sourceId the source ID where the method is defined
     * @param line the line number where the method is defined
     */
    public static void onEnter(Object thisRef, Object[] args, String owner, String name, String desc, String sourceId, int line) {
        String spanId = nextSpanId();
        String frameId = nextFrameId();
        String[] paramsAndType = JvmDescriptor.toHuman(desc);
        String methodId = IdGenerator.methodId(owner, name, paramsAndType[0], paramsAndType[1]);
        String parent = topSpan();
        String startEventId = nextEventId();

        addEvent(new CallEvent(startEventId, spanId, frameId, new TraceLoc(sourceId, line), parent, methodId, name));
        traceFileBuilder.addSpan(new TraceSpan(spanId, parent, methodId, startEventId, "end", new TraceLoc(sourceId, line), new TraceLoc("source", 1), "Waiting"));

        List<Arg> argList = new ArrayList<>();
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                argList.add(new Arg("#" + i, args[i]));
            }
        }

        traceFileBuilder.addFrame(new TraceFrame(frameId, spanId, methodId, thisRef == null ? null : IdGenerator.objectId(thisRef), List.copyOf(argList)));
        STACK.get().push(new FrameCtx(spanId, frameId, methodId));
    }

    /**
     * Writes the collected trace data after a method exit
     * @param ret the return value of the method
     * @param sourceId the source ID where the return occurs
     * @param line the line number where the return occurs
     */
    public static void onReturn(Object ret, String sourceId, int line) {
        clearBlocks();

        var st = STACK.get();
        if (st.isEmpty()) return;
        var f = st.pop();

        String evId = nextEventId();
        addEvent(new ReturnEvent(evId, f.spanId(), f.frameId(), new TraceLoc(sourceId, line), ret));

        patchSpanEnd(f.spanId(), evId, new TraceLoc(sourceId, line), "OK");
    }

    /**
     * Writes the collected trace data after a method throws an exception
     * @param ex the thrown exception
     * @param sourceId the source ID where the throw occurs
     * @param line the line number where the throw occurs
     */
    public static void onThrow(Throwable ex, String sourceId, int line) {
        clearBlocks();

        var st = STACK.get();
        if (st.isEmpty()) return;
        var f = st.pop();

        String evId = nextEventId();
        addEvent(new ThrowEvent(evId, f.spanId(), f.frameId(), new TraceLoc(sourceId, line), ex.getClass().getName(), ex.getMessage()));

        patchSpanEnd(f.spanId(), evId, new TraceLoc(sourceId, line), "THROW:" + ex.getClass().getName());
    }

    /**
     * Writes the collected trace data when a field is modified
     * @param owner the owner of the field
     * @param field the name of the field
     * @param desc the descriptor of the field
     * @param instanceOrNull the instance whose field is being modified, or null (if static)
     * @param value the new value being assigned to the field
     * @param sourceId the source ID where the modification occurs
     * @param line the line number where the modification occurs
     */
    public static void logPutField(String owner, String field, String desc, Object instanceOrNull, Object value, String sourceId, int line) {
        var f = STACK.get().peek();
        if (f == null) return;

        addEvent(
            new PutFieldEvent(nextEventId(), f.spanId(), f.frameId(), new TraceLoc(sourceId, line), owner, field, desc, IdGenerator.objectId(instanceOrNull), value)
        );
    }

    /**
     * Writes the collected trace data when an array element is modified
     * @param array the array being modified
     * @param index the index of the element being modified
     * @param value the new value being assigned to the element
     * @param sourceId the source ID where the modification occurs
     * @param line the line number where the modification occurs
     */
    public static void logArrayStore(Object array, int index, Object value, String sourceId, int line) {
        var f = STACK.get().peek();
        if (f == null) return;

        addEvent(
            new ArrayStoreEvent(nextEventId(), f.spanId(), f.frameId(), new TraceLoc(sourceId, line), IdGenerator.objectId(array), index, value)
        );
    }

    /**
     * Writes the collected trace data when a local variable is modified
     * @param owner the owner of the method
     * @param method the name of the method
     * @param varName the name of the local variable
     * @param index the index of the local variable
     * @param value the new value being assigned to the local variable
     * @param sourceId the source ID where the modification occurs
     * @param line the line number where the modification occurs
     */
    public static void logLocal(String owner, String method, String varName, int index, Object value, String sourceId, int line) {
        var f = STACK.get().peek();
        if (f == null) return;

        addEvent(
            new LocalEvent(nextEventId(), f.spanId(), f.frameId(), new TraceLoc(sourceId, line), owner, method, varName, index, value)
        );
    }

    /**
     * Writes the collected trace data when an arithmetic operation is performed
     * @param op the operation performed
     * @param desc the descriptor of the operation
     * @param left the left operand
     * @param right the right operand
     * @param result the result of the operation
     * @param sourceId the source ID where the computation occurs
     * @param line the line number where the computation occurs
     */
    public static void logArithmetic(String op, String desc, Object left, Object right, Object result, String sourceId, int line) {
        var f = STACK.get().peek();
        if (f == null) return;

        String resultType = JvmDescriptor.toHuman(desc)[1];

        addEvent(
            new ArithmeticEvent(nextEventId(), f.spanId(), f.frameId(), new TraceLoc(sourceId, line), op, resultType, left, right, result)
        );
    }

    /**
     * Writes the collected trace data when a condition is evaluated
     * @param left the left operand of the condition
     * @param right the right operand of the condition
     * @param result the result of the condition
     * @param sourceId the source ID where the condition occurs
     * @param line the line number where the condition occurs
     * @return the event ID of the logged condition
     */
    public static String logCondition(Object left, Object right, boolean result, String sourceId, int line) {
        var f = STACK.get().peek();
        if (f == null) return null;

        String id = nextEventId();
        addEvent(new ConditionEvent(id, f.spanId(), f.frameId(), new TraceLoc(sourceId, line), left, right, result, new String[0]));

        return id;
    }

    /**
     * Writes the collected trace data when a new object is created
     * @param thisObj the newly created object
     * @param ownerInternal the internal name of the owner class
     * @param sourceId the source ID where the creation occurs
     * @param line the line number where the creation occurs
     */
    public static void logNew(Object thisObj, String ownerInternal, String sourceId, int line) {
        var f = STACK.get().peek();
        String typeId = IdGenerator.typeId(ownerInternal.replace('/', '.'));

        addEvent(
            new NewEvent(nextEventId(), f == null ? null : f.spanId(), f == null ? null : f.frameId(),
                new TraceLoc(sourceId, line), typeId, IdGenerator.objectId(thisObj))
        );
    }

    /**
     * Begins a block of events under a parent event
     * @param parentEventId the parent's event ID
     */
    public static void beginBlock(String parentEventId) {
        BLOCKS.get().push(new BlockCtx(parentEventId, new ArrayList<>()));
    }

    /**
     * Ends the current block of events
     * @param parentEventId the parent's event ID
     */
    public static void endBlock(String parentEventId) {
        var bs = BLOCKS.get();
        if (bs.isEmpty()) return;
        var b = bs.pop();
        if (!Objects.equals(b.parentEventId(), parentEventId)) {
            return;
        }

        patchBlockEvents(parentEventId, b.eventIds());
    }

    private static void clearBlocks() {
        while (!BLOCKS.get().isEmpty()) {
            BlockCtx block = BLOCKS.get().peek();
            if (block != null) {
                endBlock(block.parentEventId());
            }
        }
    }

    private static void patchBlockEvents(String parentEventId, List<String> eventIds) {
        var events = traceFileBuilder.getEvents();
        for (int i = events.size() - 1; i >= 0; i--) {
            var e = events.get(i);
            if (e instanceof ConditionEvent ce && ce.eventId().equals(parentEventId)) {
                var patched = new ConditionEvent(
                        ce.eventId(), ce.spanId(), ce.frameId(), ce.location(),
                        ce.left(), ce.right(), ce.result(),
                        eventIds.toArray(String[]::new)
                );
                events.set(i, patched);

                indexFileBuilder.replaceEvent(patched);
                return;
            }
        }
    }

    private static void patchSpanEnd(String spanId, String endEventId, TraceLoc endLoc, String status) {
        for (int i = traceFileBuilder.getSpans().size() - 1; i >= 0; i--) {
            TraceSpan s = traceFileBuilder.getSpans().get(i);
            if (s.spanId().equals(spanId) && s.endEventId().equals("end")) {
                traceFileBuilder.getSpans().set(i, new TraceSpan(s.spanId(), s.parentSpanId(), s.methodId(), s.startEventId(), endEventId, s.startLoc(), endLoc, status));
                return;
            }
        }
    }

    private static String nextSpanId() {
        return IdGenerator.spanId(String.valueOf(nextSpanId++));
    }

    private static String nextEventId() {
        return IdGenerator.eventId(String.valueOf(nextEventId++));
    }

    private static String nextFrameId() {
        return IdGenerator.frameId(String.valueOf(nextFrameId++));
    }

    private static String topSpan() {
        var f = STACK.get().peek();
        return f == null ? null : f.spanId();
    }

    private static String topFrame() {
        var f = STACK.get().peek();
        return f == null ? null : f.frameId();
    }

    private static String currentMethod() {
        var f = STACK.get().peek();
        return f == null ? null : f.methodId();
    }

    private static void addEvent(TraceEvent event)
    {
        traceFileBuilder.addEvent(event);
        indexFileBuilder.addEvent(event);

        var bs = BLOCKS.get();
        if (!bs.isEmpty()) {
            bs.peek().eventIds().add(event.eventId());
        }
    }

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                traceFileBuilder.writeJsonTo(new File("Results/trace.json"));
                indexFileBuilder.writeJsonTo(new File("Results/index.json"));
            }
            catch (Exception e) {
                System.err.println("Failed to write trace/index file: " + e.getMessage());
            }
        }, "trace-flush"));
    }

    private record FrameCtx(String spanId, String frameId, String methodId) {}
    private record BlockCtx(String parentEventId, List<String> eventIds) {}
}
