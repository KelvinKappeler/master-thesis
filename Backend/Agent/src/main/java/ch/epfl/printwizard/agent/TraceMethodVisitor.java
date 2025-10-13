package ch.epfl.printwizard.agent;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.Method;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a method visitor that instruments methods for tracing.
 */
public final class TraceMethodVisitor extends AdviceAdapter {

    private final TraceConfig config;
    private final String ownerInternal;
    private final String sourceFile;
    private final boolean isStatic;

    private final Map<Integer, String> varNameByIndex = new HashMap<>();
    private final Map<Integer, String> varDescByIndex = new HashMap<>();
    private int currentLine = 1;

    private final Label tryStart = new Label();
    private final Label tryEnd   = new Label();
    private final Label handler  = new Label();

    TraceMethodVisitor(int api, MethodVisitor mv, int access, String name, String desc, TraceConfig config, String ownerInternal, String sourceFile) {
        super(api, mv, access, name, desc);

        this.config = config;
        this.ownerInternal = ownerInternal;
        this.sourceFile = sourceFile;
        this.isStatic = (access & ACC_STATIC) != 0;
    }

    @Override public void visitLineNumber(int line, Label start) {
        currentLine = line;
        super.visitLineNumber(line, start);
    }

    @Override public void visitCode() {
        super.visitCode();
        visitLabel(tryStart);
    }

    private void pushSourceIdAndLine() {
        push(ownerInternal);
        push(sourceFile);
        invokeStatic(Type.getType(TraceSink.class), new Method("sourceIdForOwner",
        "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"));
        push(currentLine);
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

    @Override
    public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
        System.out.println("var " + name + " " + desc + " at index " + index);
        varNameByIndex.put(index, name);
        varDescByIndex.put(index, desc);

        super.visitLocalVariable(name, desc, signature, start, end, index);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        if (opcode == PUTFIELD || opcode == PUTSTATIC) {
            Type t = Type.getType(desc);
            int valueLocal  = newLocal(t);
            if (opcode == PUTFIELD) {
                int objectLocal = newLocal(Type.getType(Object.class));

                storeLocal(valueLocal, t);
                storeLocal(objectLocal, Type.getType(Object.class));

                // logPutField(owner, name, desc, instance, value, sourceId, line)
                push(owner);
                push(name);
                push(desc);
                loadLocal(objectLocal);
                loadLocal(valueLocal);
                if (t.getSort() != Type.OBJECT && t.getSort() != Type.ARRAY) box(t);
                pushSourceIdAndLine();
                invokeStatic(Type.getType(TraceSink.class), new Method("logPutField",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;I)V"));

                loadLocal(objectLocal);
            } else {
                storeLocal(valueLocal, t);

                push(owner);
                push(name);
                push(desc);
                visitInsn(ACONST_NULL);
                loadLocal(valueLocal);
                if (t.getSort() != Type.OBJECT && t.getSort() != Type.ARRAY) box(t);
                pushSourceIdAndLine();
                invokeStatic(Type.getType(TraceSink.class), new Method("logPutField",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;I)V"));

            }
            loadLocal(valueLocal);
            super.visitFieldInsn(opcode, owner, name, desc);
            return;
        }
        
        super.visitFieldInsn(opcode, owner, name, desc);
    }

    @Override
    public void visitInsn(int opcode) {
        switch (opcode) {
            case IASTORE: case LASTORE: case FASTORE: case DASTORE:
            case AASTORE: case BASTORE: case CASTORE: case SASTORE: {
                Type vtype = valueTypeForArrayStore(opcode);
                
                int valueLocal = newLocal(vtype);
                int indexLocal = newLocal(Type.INT_TYPE);
                int arrayLocal = newLocal(Type.getType(Object.class));

                storeLocal(valueLocal, vtype);
                storeLocal(indexLocal, Type.INT_TYPE);
                storeLocal(arrayLocal, Type.getType(Object.class));

                // logArrayStore(array, index, value, sourceId, line)
                loadLocal(arrayLocal);
                loadLocal(indexLocal);
                loadLocal(valueLocal);
                if (opcode != AASTORE) box(vtype);
                pushSourceIdAndLine();
                invokeStatic(Type.getType(TraceSink.class), new Method("logArrayStore",
                "(Ljava/lang/Object;ILjava/lang/Object;Ljava/lang/String;I)V"));

                loadLocal(arrayLocal);
                loadLocal(indexLocal);
                loadLocal(valueLocal);
            }
        }
        
        super.visitInsn(opcode);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        boolean store = opcode == ISTORE || opcode == LSTORE || opcode == FSTORE || opcode == DSTORE || opcode == ASTORE;

        if (store) {
            super.visitVarInsn(opcode, var);

            String desc = varDescByIndex.get(var);
            if (desc == null) {
                desc = switch (opcode) {
                    case ISTORE -> "I";
                    case LSTORE -> "J";
                    case FSTORE -> "F";
                    case DSTORE -> "D";
                    default -> "Ljava/lang/Object;";
                };
            }

            System.out.println("TEST " + var);
            varNameByIndex.forEach((k, v) -> System.out.println(k + " -> " + v));
            String name = varNameByIndex.getOrDefault(var, "#" + var);

            push(ownerInternal);
            push(getName());
            push(name);
            push(var);

            Type t = Type.getType(desc);
            loadLocal(var, t);
            if (t.getSort() != Type.OBJECT && t.getSort() != Type.ARRAY) box(t);

            pushSourceIdAndLine();
            invokeStatic(Type.getType(TraceSink.class), new Method("logLocal",
            "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ILjava/lang/Object;Ljava/lang/String;I)V"));

            return;
        }

        super.visitVarInsn(opcode, var);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        if ((opcode == INVOKEVIRTUAL || opcode == INVOKESPECIAL || opcode == INVOKESTATIC || opcode == INVOKEINTERFACE)) {
            push(this.ownerInternal);
            push(getName());
            push(methodDesc);
            push(owner);
            push(name);
            push(desc);
            pushSourceIdAndLine();
            invokeStatic(Type.getType(TraceSink.class), new Method("beforeCall",
            "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;"
                    + "Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;"
                    + "Ljava/lang/String;I)V"));
        }
        
        super.visitMethodInsn(opcode, owner, name, desc, itf);
    }

    @Override
    protected void onMethodEnter() {
        // onEnter(Object thisRef, Object[] args, String owner, String name, String desc, String sourceId, int line)
        if (isStatic) {
            visitInsn(ACONST_NULL);
        }
        else {
            loadThis();
        }

        loadArgArray();

        push(ownerInternal);
        push(getName());
        push(methodDesc);
        pushSourceIdAndLine();

        invokeStatic(Type.getType(TraceSink.class), new Method("onEnter",
        "(Ljava/lang/Object;[Ljava/lang/Object;"
                + "Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;"
                + "Ljava/lang/String;I)V"));
    }

    @Override
    protected void onMethodExit(int opcode) {
        if ("<init>".equals(getName()) && opcode == RETURN) {
            loadThis();
            push(ownerInternal);
            pushSourceIdAndLine();
            invokeStatic(Type.getType(TraceSink.class), new Method("logNew", "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;I)V"));
        }

        switch (opcode) {
            case RETURN -> visitInsn(ACONST_NULL);
            case ARETURN -> dup();
            case IRETURN -> {
                dup();
                box(Type.INT_TYPE);
            }
            case FRETURN -> {
                dup();
                box(Type.FLOAT_TYPE);
            }
            case LRETURN -> {
                dup2();
                box(Type.LONG_TYPE);
            }
            case DRETURN -> {
                dup2();
                box(Type.DOUBLE_TYPE);
            }
            case ATHROW -> {
                return;
            }
        }
        pushSourceIdAndLine();
        invokeStatic(Type.getType(TraceSink.class),
                new Method("onReturn", "(Ljava/lang/Object;Ljava/lang/String;I)V"));
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        /*
        visitLabel(tryEnd);

        super.visitTryCatchBlock(tryStart, tryEnd, handler, null);
        visitLabel(handler);

        int exLocal = newLocal(Type.getType(Throwable.class));
        storeLocal(exLocal);

        loadLocal(exLocal);
        pushSourceIdAndLine();
        invokeStatic(Type.getType(TraceSink.class), new Method("onThrow", "(Ljava/lang/Throwable;Ljava/lang/String;I)V"));

        loadLocal(exLocal);
        visitInsn(ATHROW);
*/
        super.visitMaxs(maxStack, maxLocals);
    }
}
