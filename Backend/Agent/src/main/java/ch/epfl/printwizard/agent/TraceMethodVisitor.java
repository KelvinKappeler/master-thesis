package ch.epfl.printwizard.agent;

import ch.epfl.printwizard.agent.utils.JvmDescriptor;
import ch.epfl.printwizard.shared.IdGenerator;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.Method;

import java.util.*;

/**
 * Represents a method visitor that instruments methods for tracing.
 */
public final class TraceMethodVisitor extends AdviceAdapter {

    private final TraceConfig config;
    private final String ownerInternal;
    private final String sourceFile;
    private final boolean isStatic;
    private final String methodId;

    private final Map<Label, List<Integer>> pendingEndBlocks = new IdentityHashMap<>();

    private int currentLine = 1;

    TraceMethodVisitor(int api, MethodVisitor mv, int access, String name, String desc, TraceConfig config, String ownerInternal, String sourceFile) {
        super(api, mv, access, name, desc);

        this.config = config;
        this.ownerInternal = ownerInternal;
        this.sourceFile = sourceFile;
        this.isStatic = (access & ACC_STATIC) != 0;

        String[] humanDesc  = JvmDescriptor.toHuman(desc);
        this.methodId = IdGenerator.methodId(ownerInternal, name, humanDesc[0], humanDesc[1]);
    }

    @Override public void visitLineNumber(int line, Label start) {
        currentLine = line;
        super.visitLineNumber(line, start);
    }

    @Override
    public void visitLabel(Label label) {
        List<Integer> locals = pendingEndBlocks.remove(label);
        if (locals != null) {
            for (int evLocal : locals) {
                loadLocal(evLocal, Type.getType(String.class));
                invokeStatic(Type.getType(TraceSink.class), new Method("endBlock", "(Ljava/lang/String;)V"));
            }
        }

        super.visitLabel(label);
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        loadLocal(var, Type.INT_TYPE);
        push(increment);
        visitInsn(IADD);
        dup();
        storeLocal(var, Type.INT_TYPE);
        box(Type.INT_TYPE);
        
        push(ownerInternal); swap();
        push(methodId); swap();
        push(String.valueOf(var)); swap();
        push(var); swap();

        pushSourceIdAndLine();
        invokeStatic(Type.getType(TraceSink.class), new Method("logLocal",
        "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ILjava/lang/Object;Ljava/lang/String;I)V"));
    }
    
    /*
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
    }*/
    
    @Override
    public void visitInsn(int opcode) {
        switch (opcode) {
            // Management of array stores
            case IASTORE: case LASTORE: case FASTORE: case DASTORE:
            case AASTORE: case BASTORE: case CASTORE: case SASTORE: {
                Type vtype = valueTypeFor(opcode);
                
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

            // Management of arithmetic operations
            case IADD: case LADD: case FADD: case DADD:
            case ISUB: case LSUB: case FSUB: case DSUB:
            case IMUL: case LMUL: case FMUL: case DMUL:
            case IDIV: case LDIV: case FDIV: case DDIV:
            case IREM: case LREM: case FREM: case DREM: {
                Type t = valueTypeFor(opcode);

                int rightLocal = newLocal(t);
                int leftLocal = newLocal(t);
                storeLocal(rightLocal, t);
                storeLocal(leftLocal, t);

                // Re-execute the original operation to compute the real result
                loadLocal(leftLocal, t);
                loadLocal(rightLocal, t);
                super.visitInsn(opcode); // result now on stack

                // Save the result so we can both log and restore it
                int resLocal = newLocal(t);
                storeLocal(resLocal, t);

                // TraceSink.logComputation(op, resultType, left, right, result, sourceId, line)
                push(getArithmeticOp(opcode));
                push(t.getDescriptor());

                loadLocal(leftLocal, t);
                if (t.getSort() != Type.OBJECT && t.getSort() != Type.ARRAY) box(t);

                loadLocal(rightLocal, t);
                if (t.getSort() != Type.OBJECT && t.getSort() != Type.ARRAY) box(t);

                loadLocal(resLocal, t);
                if (t.getSort() != Type.OBJECT && t.getSort() != Type.ARRAY) box(t);

                pushSourceIdAndLine();
                invokeStatic(Type.getType(TraceSink.class), new Method("logArithmetic",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;I)V"));

                // Put the original result back on the stack for the program to continue
                loadLocal(resLocal, t);

                return;
            }
        }
        
        super.visitInsn(opcode);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        switch (opcode) {
            case ISTORE, LSTORE, FSTORE, DSTORE, ASTORE : {
                String desc = switch (opcode) {
                    case ISTORE -> "I";
                    case LSTORE -> "J";
                    case FSTORE -> "F";
                    case DSTORE -> "D";
                    default -> "Ljava/lang/Object;";
                };

                // long and double take two slots
                if (opcode == LSTORE || opcode == DSTORE) {
                    dup2();
                } else {
                    dup();
                }
                
                super.visitVarInsn(opcode, var);
                
                Type t = Type.getType(desc);
                if (t.getSort() != Type.OBJECT && t.getSort() != Type.ARRAY) box(t);

                // push args and swap the result (mId, res, name) -> (mId, name, res)
                push(ownerInternal); swap();
                push(methodId); swap();
                push(String.valueOf(var)); swap();
                push(var); swap();

                pushSourceIdAndLine();
                invokeStatic(Type.getType(TraceSink.class), new Method("logLocal",
                        "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ILjava/lang/Object;Ljava/lang/String;I)V"));
                
                return;
            }
            default: {
                super.visitVarInsn(opcode, var);
            }
        }
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
    public void visitJumpInsn(int opcode, Label label) {
        switch (opcode) {
            // int vs int
            case IF_ICMPEQ: case IF_ICMPNE: case IF_ICMPLT:
            case IF_ICMPLE: case IF_ICMPGT: case IF_ICMPGE: {
                Type t = Type.INT_TYPE;

                int rightLocal = newLocal(t);
                int leftLocal = newLocal(t);
                storeLocal(rightLocal, t);
                storeLocal(leftLocal, t);

                Label labelTrue = new Label();
                Label labelEnd = new Label();
                int condLocal = newLocal(t);

                loadLocal(leftLocal, t);
                loadLocal(rightLocal, t);
                super.visitJumpInsn(opcode, labelTrue);

                // false
                push(0);
                visitJumpInsn(GOTO, labelEnd);

                // true
                visitLabel(labelTrue);
                push(1);

                visitLabel(labelEnd);
                storeLocal(condLocal, t);

                // == Call logCondition ==
                // left
                loadLocal(leftLocal, t);
                box(t);

                // right
                loadLocal(rightLocal, t);
                box(t);

                loadLocal(condLocal, t);
                push(1);
                visitInsn(IXOR);

                pushSourceIdAndLine();
                invokeStatic(Type.getType(TraceSink.class), new Method("logCondition",
                "(Ljava/lang/Object;Ljava/lang/Object;ZLjava/lang/String;I)Ljava/lang/String;"));

                int evLocal = newLocal(Type.getType(String.class));
                storeLocal(evLocal, Type.getType(String.class));

                enqueueEnd(label, evLocal);

                Label fallThrough = new Label();
                loadLocal(condLocal, t);
                super.visitJumpInsn(IFNE, label);
                visitLabel(fallThrough);

                loadLocal(evLocal, Type.getType(String.class));
                invokeStatic(Type.getType(TraceSink.class), new Method("beginBlock", "(Ljava/lang/String;)V"));

                return;
            }

            // int against 0
            case IFEQ: case IFNE: case IFLT: case IFLE: case IFGT: case IFGE: {
                Type t = Type.INT_TYPE;

                int val = newLocal(t);
                storeLocal(val, t);

                Label labelTrue = new Label();
                Label labelEnd = new Label();
                int condLocal = newLocal(t);

                loadLocal(val, t);
                super.visitJumpInsn(opcode, labelTrue);

                // false
                push(0);
                visitJumpInsn(GOTO, labelEnd);

                // true
                visitLabel(labelTrue);
                push(1);

                visitLabel(labelEnd);
                storeLocal(condLocal);

                // == Call logCondition ==

                // left
                loadLocal(val, t);
                box(t);

                // right
                push(0);
                box(t);

                // result
                loadLocal(condLocal);
                push(1);
                visitInsn(IXOR);

                pushSourceIdAndLine();
                invokeStatic(Type.getType(TraceSink.class), new Method("logCondition",
                "(Ljava/lang/Object;Ljava/lang/Object;ZLjava/lang/String;I)Ljava/lang/String;"));

                int evLocal = newLocal(Type.getType(String.class));
                storeLocal(evLocal, Type.getType(String.class));

                enqueueEnd(label, evLocal);

                Label fallThrough = new Label();
                loadLocal(condLocal);
                super.visitJumpInsn(IFNE, label);
                visitLabel(fallThrough);

                loadLocal(evLocal);
                invokeStatic(Type.getType(TraceSink.class), new Method("beginBlock", "(Ljava/lang/String;)V"));

                return;
            }
        }

        super.visitJumpInsn(opcode, label);
    }

    private static Type valueTypeFor(int opcode) {
        return switch (opcode) {
            case IADD, ISUB, IMUL, IDIV, IREM, IAND, IOR, IXOR,
                 IASTORE, BASTORE, CASTORE, SASTORE -> Type.INT_TYPE;
            case LADD, LSUB, LMUL, LDIV, LREM, LAND, LOR, LXOR,
                 LASTORE -> Type.LONG_TYPE;
            case FADD, FSUB, FMUL, FDIV, FREM,
                 FASTORE -> Type.FLOAT_TYPE;
            case DADD, DSUB, DMUL, DDIV, DREM,
                 DASTORE -> Type.DOUBLE_TYPE;
            default -> Type.getType(Object.class);
        };
    }

    private static String getArithmeticOp(int opcode) {
        return switch (opcode) {
            case IADD, LADD, FADD, DADD -> "+";
            case ISUB, LSUB, FSUB, DSUB -> "-";
            case IMUL, LMUL, FMUL, DMUL -> "*";
            case IDIV, LDIV, FDIV, DDIV -> "/";
            case IREM, LREM, FREM, DREM -> "%";
            case IAND, LAND -> "&";
            case IOR, LOR -> "|";
            case IXOR, LXOR -> "^";
            case IF_ICMPEQ, IF_ACMPEQ, IFEQ -> "==";
            case IF_ICMPNE, IF_ACMPNE, IFNE -> "!=";
            case IF_ICMPLT, IFLT -> "<";
            case IF_ICMPLE, IFLE -> "<=";
            case IF_ICMPGT, IFGT -> ">";
            case IF_ICMPGE, IFGE -> ">=";
            default -> throw new IllegalArgumentException("Invalid opcode: " + opcode);
        };
    }

    private void pushSourceIdAndLine() {
        push(ownerInternal);
        push(sourceFile);
        invokeStatic(Type.getType(TraceSink.class), new Method("sourceIdForOwner",
                "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"));
        push(currentLine);
    }

    private void enqueueEnd(Label label, int evLocal) {
        pendingEndBlocks.computeIfAbsent(label, l -> new ArrayList<>()).add(evLocal);
    }
}
