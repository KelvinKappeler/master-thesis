package ch.epfl.printwizard.agent;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.Method;

import java.util.HashMap;
import java.util.Map;

public final class TraceMethodVisitor extends AdviceAdapter {
    
    private final TraceConfig config;
    private final String ownerInternal;

    private final Map<Integer, String> varNameByIndex = new HashMap<>();
    private final Map<Integer, String> varDescByIndex = new HashMap<>();

    TraceMethodVisitor(int api, MethodVisitor mv, int access, String name, String desc,
                       TraceConfig config, String ownerInternal) {
        super(api, mv, access, name, desc);
        
        this.config = config;
        this.ownerInternal = ownerInternal;
        
        TraceSink.init(config);
    }

    @Override
    public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
        varNameByIndex.put(index, name);
        varDescByIndex.put(index, desc);
        
        super.visitLocalVariable(name, desc, signature, start, end, index);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        if (config.isTraceFields() && opcode == PUTFIELD) {
            Type t = Type.getType(desc);
            int valueLocal  = newLocal(t);
            int objectLocal = newLocal(Type.getType(Object.class));
            
            storeLocal(valueLocal, t);
            storeLocal(objectLocal, Type.getType(Object.class));

            // logPutField(ownerInternal, field, desc, instanceOrNull, value)
            push(owner);
            push(name);
            push(desc);
            loadLocal(objectLocal);
            loadLocal(valueLocal);
            box(t);
            invokeStatic(Type.getType(TraceSink.class),
                new Method("logPutField",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V"));

            // Reload for the original PUTFIELD
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
                    int valueLocal  = newLocal(vtype);
                    int indexLocal  = newLocal(Type.INT_TYPE);
                    int arrayLocal  = newLocal(Type.getType(Object.class));
                    
                    storeLocal(valueLocal, vtype);
                    storeLocal(indexLocal, Type.INT_TYPE);
                    storeLocal(arrayLocal, Type.getType(Object.class));

                    // logArrayStore(arrayRef, index, valueBoxed)
                    loadLocal(arrayLocal);
                    loadLocal(indexLocal);
                    loadLocal(valueLocal);
                    if (opcode != AASTORE) {
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
    public void visitVarInsn(int opcode, int var) {
        if (config.isTraceLocals()
            && (opcode == ISTORE || opcode == LSTORE || opcode == FSTORE
            || opcode == DSTORE || opcode == ASTORE)) {

            super.visitVarInsn(opcode, var);

            String desc = varDescByIndex.get(var);
            if (desc == null) {
                desc = switch (opcode) {
                    case ISTORE -> "I";
                    case LSTORE -> "J";
                    case FSTORE -> "F";
                    case DSTORE -> "D";
                    case ASTORE -> "Ljava/lang/Object;";
                    default -> "Ljava/lang/Object;";
                };
            }
            String name = varNameByIndex.getOrDefault(var, "#" + var);

            // logLocal : owner, method, varName, index, value(Object)
            push(ownerInternal);
            push(getName());
            push(name);
            push(var);

            Type t = Type.getType(desc);
            loadLocal(var, t);
            if (t.getSort() != Type.OBJECT && t.getSort() != Type.ARRAY) {
                box(t);
            }

            invokeStatic(Type.getType(TraceSink.class),
                    new Method("logLocal",
                            "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ILjava/lang/Object;)V"));
        }
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
