package ch.epfl.printwizard.agent;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

public final class TraceSink {
    private static PrintWriter Out;
    private static boolean initialized = false;

    public static void init(TraceConfig cfg) {
        if (initialized) return;
        try {
            if ("".equals(cfg.getOutPath())) {
                Out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8)), true);
            } else {
                Out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(cfg.getOutPath(), true), StandardCharsets.UTF_8)), true);
            }
            initialized = true;
        } catch (Exception e) {
            throw new RuntimeException("TraceSink init failed", e);
        }
    }

    public static void logNew(Object ref, String ownerInternal) {
        long ts = now();
        String refId = ObjectIds.id(ref);
        String typeId = "t:" + ownerInternal.replace('/', '.');
        Out.println("ts: " + ts + ", op: new, ref: " + refId + ", typeId: " + typeId);
    }

    public static void logPutField(String ownerInternal, String field, String desc, Object instanceOrNull, Object value) {
        long ts = now();
        String owner = "t:" + ownerInternal.replace('/', '.');
        String ref = (instanceOrNull == null) ? "static" : ObjectIds.id(instanceOrNull);
        Out.println("ts: " + ts + ", op: " + ((instanceOrNull == null) ? "putstatic" : "putfield") +
                ", ref: " + ref + ", owner: " + owner +
                ", field: " + field + ", value: " + value);
    }

    public static void logArrayStore(Object arrayRef, int index, Object value) {
        long ts = now();
        String ref = ObjectIds.id(arrayRef);
        Out.println("ts: " + ts + ", op: arraystore, ref: " + ref + ", index: " + index + ", value: " + value);
    }

    public static void logLocal(String ownerInternal, String methodName, String varName, int index, Object value) {
        Out.println("ts: " + now() + ", op: local, method: " + ownerInternal.replace('/', '.') + "." + methodName +
                ", varName: " + varName + ", index: " + index + ", value: " + value);
    }

    private static long now() {
        return System.currentTimeMillis();
    }
}
