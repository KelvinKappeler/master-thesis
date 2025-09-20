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
            int valueLocal  = newLocal(t);
            int objectLocal = newLocal(Type.getType(Object.class));

            // store value, then object (order matters: top of stack is value)
            storeLocal(valueLocal, t); // pops value
            storeLocal(objectLocal, Type.getType(Object.class)); // pops objectref

            // Call: logPutField(ownerInternal, field, desc, instanceOrNull, value)
            push(owner); // internal name already (slashes), pass as-is
            push(name);
            push(desc);
            loadLocal(objectLocal);
            loadLocal(valueLocal);
            box(t);
            invokeStatic(Type.getType(TraceSink.class),
                new Method("logPutField",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V"));

            // Re-load for the original PUTFIELD
            loadLocal(objectLocal);
            loadLocal(valueLocal);
            super.visitFieldInsn(opcode, owner, name, desc);
            return;
        }
        if (config.isTraceFields() && opcode == PUTSTATIC) {
            Type t = Type.getType(desc);
            int valueLocal = newLocal(t);
            storeLocal(valueLocal, t);

            // logPutField(owner, name, desc, null, value)
            push(owner);
            push(name);
            push(desc);
            visitInsn(Opcodes.ACONST_NULL);
            loadLocal(valueLocal);
            box(t);
            invokeStatic(Type.getType(TraceSink.class),
                new Method("logPutField",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V"));

            // Restore for original PUTSTATIC
            loadLocal(valueLocal);
            super.visitFieldInsn(opcode, owner, name, desc);
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
                    // Locals to preserve operands
                    int valueLocal  = newLocal(vtype);
                    int indexLocal  = newLocal(Type.INT_TYPE);
                    int arrayLocal  = newLocal(Type.getType(Object.class));

                    // Pop into locals (top-first: value, then index, then arrayref)
                    storeLocal(valueLocal, vtype);
                    storeLocal(indexLocal, Type.INT_TYPE);
                    storeLocal(arrayLocal, Type.getType(Object.class));

                    // --- Call logger: logArrayStore(arrayRef, index, valueBoxed) ---
                    loadLocal(arrayLocal);              // Object arrayRef
                    loadLocal(indexLocal);              // int index
                    loadLocal(valueLocal);              // <T> value
                    if (opcode != AASTORE) {            // only box primitives
                        box(vtype);
                    }
                    invokeStatic(Type.getType(TraceSink.class),
                            new Method("logArrayStore", "(Ljava/lang/Object;ILjava/lang/Object;)V"));

                    loadLocal(arrayLocal);
                    loadLocal(indexLocal);
                    loadLocal(valueLocal);
                    super.visitInsn(opcode);
                    return;
                }
            }
        }
        super.visitInsn(opcode);
    }

    @Override
    protected void onMethodExit(int opcode) {
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
