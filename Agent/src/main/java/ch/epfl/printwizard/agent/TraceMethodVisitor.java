package ch.epfl.printwizard.agent;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.Method;

public final class TraceMethodVisitor extends AdviceAdapter {
    
    private final TraceConfig config;
    private final String ownerInternal;

    TraceMethodVisitor(int api, MethodVisitor mv, int access, String name, String desc,
                       TraceConfig config, String ownerInternal) {
        super(api, mv, access, name, desc);
        
        this.config = config;
        this.ownerInternal = ownerInternal;
        
        TraceSink.init(config);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        if (config.isTraceFields() && opcode == PUTFIELD) {
            Type t = Type.getType(desc);
            int vLocal = newLocal(t);
            int oLocal = newLocal(Type.getType(Object.class));
            // stack: ..., obj, value
            storeLocal(vLocal);          // pops value
            storeLocal(oLocal);          // pops obj
            // original
            loadLocal(oLocal);
            loadLocal(vLocal);
            super.visitFieldInsn(opcode, owner, name, desc);
            // log
            push(owner);                 // internal owner
            push(name);
            push(desc);
            loadLocal(oLocal);
            box(t);                      // value -> Object
            invokeStatic(Type.getType(TraceSink.class),
                    new Method("logPutField",
                            "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V"));
            return;
        }
        if (config.isTraceFields() && opcode == PUTSTATIC) {
            Type t = Type.getType(desc);
            int vLocal = newLocal(t);
            storeLocal(vLocal);
            super.visitFieldInsn(opcode, owner, name, desc);
            push(owner);
            push(name);
            push(desc);
            visitInsn(ACONST_NULL);
            loadLocal(vLocal);
            box(t);
            invokeStatic(Type.getType(TraceSink.class),
                    new Method("logPutField",
                            "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V"));
            return;
        }
        
        super.visitFieldInsn(opcode, owner, name, desc);
    }

    @Override
    public void visitInsn(int opcode) {
        if (config.isTraceArrays()) {
            switch (opcode) {
                case IASTORE: case LASTORE: case FASTORE: case DASTORE:
                case AASTORE: case BASTORE: case CASTORE: case SASTORE: {
                    Type vtype = valueTypeForArrayStore(opcode);
                    int vLocal = newLocal(vtype);
                    int idxLocal = newLocal(Type.INT_TYPE);
                    int arrLocal = newLocal(Type.getType(Object.class));
                    // stack: ..., arrayref, index, value
                    storeLocal(vLocal);
                    storeLocal(idxLocal);
                    storeLocal(arrLocal);
                    // original
                    loadLocal(arrLocal);
                    loadLocal(idxLocal);
                    loadLocal(vLocal);
                    super.visitInsn(opcode);
                    // log
                    box(vtype);
                    invokeStatic(Type.getType(TraceSink.class),
                            new Method("logArrayStore", "(Ljava/lang/Object;ILjava/lang/Object;)V"));
                    return;
                }
            }
        }
        super.visitInsn(opcode);
    }

    @Override
    protected void onMethodExit(int opcode) {
        // Log "new" after constructors return normally
        if (config.isTraceNew() && "<init>".equals(getName()) && opcode == RETURN) {
            loadThis();
            push(ownerInternal);
            invokeStatic(Type.getType(TraceSink.class),
                    new Method("logNew", "(Ljava/lang/Object;Ljava/lang/String;)V"));
        }
    }

    private static Type valueTypeForArrayStore(int opcode) {
        return switch (opcode) {
            case LASTORE -> Type.LONG_TYPE;
            case DASTORE -> Type.DOUBLE_TYPE;
            case FASTORE -> Type.FLOAT_TYPE;
            case IASTORE, BASTORE, CASTORE, SASTORE -> Type.INT_TYPE;
            default -> Type.getType(Object.class);
        };
    }
}
