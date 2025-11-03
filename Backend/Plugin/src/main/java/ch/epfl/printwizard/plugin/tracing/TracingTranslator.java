package ch.epfl.printwizard.plugin.tracing;

import ch.epfl.printwizard.plugin.utils.Ids;
import ch.epfl.printwizard.plugin.model.trace.TraceFile;
import ch.epfl.printwizard.plugin.model.trace.TraceLoc;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Names;
import com.sun.tools.javac.model.JavacElements;

import javax.tools.JavaFileObject;

/**
 * Represents the scanner for the PrintWizard instrumentation.
 */
public class TracingTranslator extends TreeTranslator {

    private final Context ctx;
    private final JCTree.JCCompilationUnit cu;

    private final TreeMaker mk;
    private final Names names;
    private final JavacTrees trees;
    private final JavacElements elements;

    private final TraceFile.Builder traceFileBuilder;

    public TracingTranslator(Context ctx, JCTree.JCCompilationUnit cu) {
        this.ctx = ctx;
        this.cu = cu;

        this.mk = TreeMaker.instance(ctx);
        this.names = Names.instance(ctx);
        this.trees = JavacTrees.instance(ctx);
        this.elements = JavacElements.instance(ctx);

        this.traceFileBuilder = new TraceFile.Builder();
    }

    /**
     * Translates the compilation unit.
     */
    public void translate() {
        cu.accept(this);
    }

    /**
     * Gets the trace file.
     * @return the trace file
     */
    public TraceFile getTraceFile() {
        return traceFileBuilder.build();
    }

    @Override
    public void visitClassDef(JCTree.JCClassDecl jcClassDecl) {
        System.out.println("TracingTranslator: Visiting class: " + jcClassDecl.getSimpleName());
        super.visitClassDef(jcClassDecl);
    }

    @Override
    public void visitMethodDef(JCTree.JCMethodDecl jcMethodDecl) {
        int startLine = cu.getLineMap().getLineNumber(jcMethodDecl.pos);
        JavaFileObject sfo = cu.getSourceFile();
        String sourcePath = (sfo != null ? sfo.toUri().getPath() : jcMethodDecl.name.toString());
        String sourceId = Ids.createSourceId(sourcePath);
        String owner = cu.packge != null ? cu.packge + "." + jcMethodDecl.getName().toString() : jcMethodDecl.getName().toString();
        String returnType = (jcMethodDecl.getReturnType() != null) ? jcMethodDecl.getReturnType().toString() : "void";

        JCTree.JCExpression argsArrayExpr = makeArgArrayExpr(jcMethodDecl, jcMethodDecl.pos);

        JCTree.JCStatement enterCall = mk.Exec(
            callStatic("ch.epfl.printwizard.plugin.logging.TraceOut", "onEnter",
                List.of(
                    mk.Literal(owner),
                    mk.Literal(jcMethodDecl.getName().toString()),
                    argsArrayExpr,
                    mk.Literal(returnType),
                    makeTraceLocExpr(new TraceLoc(sourceId, startLine), jcMethodDecl.pos)
                ),
                jcMethodDecl.pos
            )
        );

        JCTree.JCBlock originalBody = jcMethodDecl.getBody();
        if (originalBody != null) {
            int ep = jcMethodDecl.getEndPosition(cu.endPositions);
            int endLine = cu.getLineMap().getLineNumber(ep);
            if (endLine == 0) endLine = startLine;

            JCTree.JCStatement exitCall = mk.Exec(
                callStatic("ch.epfl.printwizard.plugin.logging.TraceOut", "onReturn",
                    List.of(mk.Literal("1"), makeTraceLocExpr(new TraceLoc(sourceId, endLine), ep)),
                    ep
                )
            );

            boolean isConstructor = jcMethodDecl.name.contentEquals("<init>");

            if (isConstructor) {
                List<JCTree.JCStatement> origStmts = originalBody.getStatements();

                JCTree.JCStatement superOrThisStmt = null;
                List<JCTree.JCStatement> rest = origStmts;
                if (!origStmts.isEmpty()) {
                    JCTree.JCStatement first = origStmts.getFirst();
                    boolean firstIsCtorCall =
                        first instanceof JCTree.JCExpressionStatement es
                        && es.expr instanceof JCTree.JCMethodInvocation mi
                        && (mi.meth.toString().equals("super") || mi.meth.toString().equals("this"));
                    if (firstIsCtorCall) {
                        superOrThisStmt = first;
                        rest = origStmts.tail;
                    }
                }

                if (superOrThisStmt == null) {
                    mk.at(jcMethodDecl.pos);
                    JCTree.JCExpression superIdent = mk.Ident(names._super);
                    JCTree.JCExpression superCall = mk.Apply(List.nil(), superIdent, List.nil());
                    superOrThisStmt = mk.Exec(superCall);
                }

                List<JCTree.JCStatement> newStmts = List.of(superOrThisStmt, enterCall).appendList(rest).append(exitCall);

                jcMethodDecl.body = mk.Block(0, newStmts);
            } else {
                JCTree.JCBlock tryBlock = mk.Block(0, originalBody.getStatements());
                JCTree.JCBlock finallyBlock = mk.Block(0, List.of(exitCall));
                JCTree.JCTry tryFinally = mk.Try(tryBlock, List.nil(), finallyBlock);

                jcMethodDecl.body = mk.Block(0, List.of(enterCall, tryFinally));
            }
        }

        super.visitMethodDef(jcMethodDecl);
    }

    @Override
    public void visitBinary(JCTree.JCBinary jcBinary) {
        super.visitBinary(jcBinary);

        if (!isArithmetic(jcBinary.getTag())) return;

        int line = cu.getLineMap().getLineNumber(jcBinary.pos);
        JCTree.JCExpression locExpr = makeTraceLocExpr(new TraceLoc(cu.getSourceFile().toUri().getPath(), line), jcBinary.pos);
        JCTree.JCExpression recomputed = mk.Binary(jcBinary.getTag(), jcBinary.lhs, jcBinary.rhs);

        result = callStatic(
            "ch.epfl.printwizard.plugin.logging.TraceOut",
            "recordArithmetic",
            java.util.List.of(
                mk.Literal(jcBinary.getTag().toString()),
                jcBinary.lhs,
                jcBinary.rhs,
                recomputed,
                locExpr
            ),
            jcBinary.pos
        );
    }

    // Example : int i = 3;
    @Override
    public void visitVarDef(JCTree.JCVariableDecl jcVariableDecl) {
        if (jcVariableDecl.init != null) {
            int line = cu.getLineMap().getLineNumber(jcVariableDecl.pos);

            TraceLoc loc = new TraceLoc(cu.getSourceFile().toUri().getPath(), line);
            jcVariableDecl.init = callRecordLocalEvent(jcVariableDecl.getName().toString() + "@" + line, jcVariableDecl.init, loc, jcVariableDecl.pos);
        }
        super.visitVarDef(jcVariableDecl);
    }

    // Example : i = 3;
    @Override
    public void visitAssign(JCTree.JCAssign jcAssign) {
        int line = cu.getLineMap().getLineNumber(jcAssign.pos);

        TraceLoc loc = new TraceLoc(cu.getSourceFile().toUri().getPath(), line);
        jcAssign.rhs = callRecordLocalEvent(jcAssign.lhs.toString() + "@" + line, jcAssign.rhs, loc, jcAssign.pos);

        super.visitAssign(jcAssign);
    }

    // Example : i += 3;
    @Override
    public void visitAssignop(JCTree.JCAssignOp jcAssignOp) {
        super.visitAssignop(jcAssignOp);

        JCTree.Tag binOpTag = toBinaryTag(jcAssignOp.getTag());
        JCTree.JCExpression lhs = jcAssignOp.lhs;
        JCTree.JCExpression rhs = jcAssignOp.rhs;
        JCTree.JCBinary bin = mk.Binary(binOpTag, lhs, rhs);

        String lhsLabel = lhs.toString();
        int line = cu.getLineMap().getLineNumber(jcAssignOp.pos);

        TraceLoc loc = new TraceLoc(cu.getSourceFile().toUri().getPath(), line);
        JCTree.JCExpression modifiedRhs = callRecordLocalEvent(lhsLabel + "@" + line, bin, loc, jcAssignOp.pos);

        result = mk.Assign(lhs, modifiedRhs);
    }

    private JCTree.JCMethodInvocation callStatic(String ownerFqn, String method, java.util.List<JCTree.JCExpression> args, int pos) {
        Symbol.ClassSymbol ownerSym = elements.getTypeElement(ownerFqn);
        if (ownerSym == null) {
            throw new IllegalStateException("Type not found: " + ownerFqn);
        }

        mk.at(pos);
        JCTree.JCExpression owner = mk.Ident(ownerSym);
        JCTree.JCExpression sel = mk.Select(owner, names.fromString(method));

        return mk.Apply(com.sun.tools.javac.util.List.nil(), sel, com.sun.tools.javac.util.List.from(args));
    }

    private JCTree.Tag toBinaryTag(JCTree.Tag assignOp) {
        return switch (assignOp) {
            case PLUS_ASG -> JCTree.Tag.PLUS;
            case MINUS_ASG -> JCTree.Tag.MINUS;
            case MUL_ASG -> JCTree.Tag.MUL;
            case DIV_ASG -> JCTree.Tag.DIV;
            case MOD_ASG -> JCTree.Tag.MOD;
            case BITAND_ASG -> JCTree.Tag.BITAND;
            case BITOR_ASG -> JCTree.Tag.BITOR;
            case BITXOR_ASG -> JCTree.Tag.BITXOR;
            case SL_ASG -> JCTree.Tag.SL;
            case SR_ASG -> JCTree.Tag.SR;
            case USR_ASG -> JCTree.Tag.USR;
            default -> throw new IllegalArgumentException("Unsupported assign op: " + assignOp);
        };
    }

    private JCTree.JCMethodInvocation callRecordLocalEvent(String label, JCTree.JCExpression value, TraceLoc loc, int pos) {
        return callStatic(
            "ch.epfl.printwizard.plugin.logging.TraceOut",
            "recordLocalEvent",
            java.util.List.of(mk.Literal(label), value, makeTraceLocExpr(loc, pos)),
            pos
        );
    }

    private JCTree.JCExpression makeTraceLocExpr(TraceLoc loc, int pos) {
        String traceLocFqn = "ch.epfl.printwizard.plugin.model.trace.TraceLoc";
        Symbol.ClassSymbol traceLocSym = elements.getTypeElement(traceLocFqn);

        mk.at(pos);
        return mk.NewClass(
            null,
            List.nil(),
            traceLocSym != null ? mk.Ident(traceLocSym) : mk.Ident(names.fromString(traceLocFqn)),
            List.of(mk.Literal(loc.sourceId()), mk.Literal(loc.line())),
            null
        );
    }

    private JCTree.JCExpression makeArgArrayExpr(JCTree.JCMethodDecl md, int pos) {
        String argFqn = "ch.epfl.printwizard.plugin.model.trace.Arg";
        Symbol.ClassSymbol argSym = elements.getTypeElement(argFqn);

        mk.at(pos);

        java.util.List<JCTree.JCExpression> argInits = new java.util.ArrayList<>();
        for (JCTree.JCVariableDecl param : md.getParameters()) {
            String paramName = param.getName().toString();
            String paramTypeStr = param.vartype.toString();

            JCTree.JCExpression paramIdent = mk.Ident(param.getName());

            JCTree.JCExpression newArg = mk.NewClass(
                null,
                List.nil(),
                argSym != null ? mk.Ident(argSym) : mk.Ident(names.fromString(argFqn)),
                List.of(mk.Literal(paramName), paramIdent, mk.Literal(paramTypeStr)),
                null
            );

            argInits.add(newArg);
        }

        JCTree.JCExpression argTypeExpr = argSym != null ? mk.Ident(argSym) : mk.Ident(names.fromString(argFqn));

        return mk.NewArray(argTypeExpr, List.nil(), List.from(argInits));
    }

    private boolean isArithmetic(JCTree.Tag tag) {
        return tag == JCTree.Tag.PLUS
            || tag == JCTree.Tag.MINUS
            || tag == JCTree.Tag.MUL
            || tag == JCTree.Tag.DIV
            || tag == JCTree.Tag.MOD;
    }
}
