package ch.epfl.printwizard.plugin.tracing;

import ch.epfl.printwizard.plugin.utils.Ids;
import ch.epfl.printwizard.plugin.model.trace.TraceFile;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.code.Types;
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
    private final Types types;

    private final TraceFile.Builder traceFileBuilder;

    public TracingTranslator(Context ctx, JCTree.JCCompilationUnit cu) {
        this.ctx = ctx;
        this.cu = cu;

        this.mk = TreeMaker.instance(ctx);
        this.names = Names.instance(ctx);
        this.trees = JavacTrees.instance(ctx);
        this.elements = JavacElements.instance(ctx);
        this.types = Types.instance(ctx);

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
                    mk.Literal(getSourceId()),
                    mk.Literal(startLine)
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
                    List.of(mk.Literal(""), mk.Literal(getSourceId()), mk.Literal(endLine)),
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
    public void visitIf(JCTree.JCIf jcIf) {
        int line = cu.getLineMap().getLineNumber(jcIf.pos);
        String sourceId = getSourceId();

        JCTree.JCExpression condExpr = translate(jcIf.cond);

        String condVarNameStr = "__pw_cond_" + jcIf.pos;
        var condVarName = names.fromString(condVarNameStr);

        // boolean __pw_cond_xxx = ...;
        JCTree.JCVariableDecl condVar = mk.VarDef(
            mk.Modifiers(0),
            condVarName,
            mk.TypeIdent(TypeTag.BOOLEAN),
            condExpr
        );

        JCTree.JCMethodInvocation recCondCall = callStatic(
            "ch.epfl.printwizard.plugin.logging.TraceOut",
            "recordCondition",
            List.of(
                mk.Ident(condVarName),
                mk.Literal(true),
                mk.Ident(condVarName),
                mk.Literal(sourceId),
                mk.Literal(line)
            ),
            jcIf.pos
        );

        // String __pw_cond_evt_xxx = TraceOut.recordCondition(...);
        String condEvtVarNameStr = "__pw_cond_evt_" + jcIf.pos;
        var condEvtVarName = names.fromString(condEvtVarNameStr);
        JCTree.JCVariableDecl condEvtVar = mk.VarDef(
            mk.Modifiers(0),
            condEvtVarName,
            mk.Ident(elements.getTypeElement("java.lang.String")),
            recCondCall
        );

        JCTree.JCStatement thenStmt = jcIf.thenpart == null ? mk.Block(0, List.nil()) : translate(jcIf.thenpart);

        // TraceOut.beginBlock(__pw_cond_evt_xxx);
        JCTree.JCStatement beginBlock = mk.Exec(
            callStatic(
                "ch.epfl.printwizard.plugin.logging.TraceOut",
                "beginBlock",
                List.of(mk.Ident(condEvtVarName)),
                jcIf.pos
            )
        );

        // TraceOut.endBlock(__pw_cond_evt_xxx);
        JCTree.JCStatement endBlock = mk.Exec(
            callStatic(
                "ch.epfl.printwizard.plugin.logging.TraceOut",
                "endBlock",
                List.of(mk.Ident(condEvtVarName)),
                jcIf.pos
            )
        );

        JCTree.JCBlock tracedThenBlock = mk.Block(0, List.of(beginBlock, thenStmt, endBlock));

        JCTree.JCStatement elseStmt = jcIf.elsepart == null ? null : translate(jcIf.elsepart);

        // if (__pw_cond_xxx) { ...tracedThenBlock... } else { ... }
        JCTree.JCIf newIf = mk.If(mk.Ident(condVarName), tracedThenBlock, elseStmt);

        // We replace the if by a block:
        // {
        //   boolean __pw_cond_xxx = ...;
        //   String  __pw_cond_evt_xxx = ...;
        //   if (__pw_cond_xxx) { ... } else { ... }
        // }
        this.result = mk.Block(0, List.of(condVar, condEvtVar, newIf));
    }

    @Override
    public void visitBinary(JCTree.JCBinary jcBinary) {
        super.visitBinary(jcBinary);

        if (!isArithmetic(jcBinary.getTag())) return;

        int line = cu.getLineMap().getLineNumber(jcBinary.pos);

        result = callStatic(
            "ch.epfl.printwizard.plugin.logging.TraceOut",
            "recordArithmetic",
            List.of(
                mk.Literal(jcBinary.getTag().toString()),
                jcBinary.lhs,
                jcBinary.rhs,
                (JCTree.JCExpression) result,
                mk.Literal(getSourceId()),
                mk.Literal(line)
            ),
            jcBinary.pos
        );
    }

    // Example : int i = 3;
    @Override
    public void visitVarDef(JCTree.JCVariableDecl jcVariableDecl) {
        if (jcVariableDecl.init != null) {
            int line = cu.getLineMap().getLineNumber(jcVariableDecl.pos);

            jcVariableDecl.init = callRecordLocalEvent(jcVariableDecl.getName().toString(), jcVariableDecl.init, getSourceId(), line, jcVariableDecl.pos);
        }
        super.visitVarDef(jcVariableDecl);
    }

    // Example : i = 3;
    @Override
    public void visitAssign(JCTree.JCAssign jcAssign) {
        int line = cu.getLineMap().getLineNumber(jcAssign.pos);

        jcAssign.rhs = callRecordLocalEvent(jcAssign.lhs.toString(), jcAssign.rhs, getSourceId(), line, jcAssign.pos);

        super.visitAssign(jcAssign);
    }
    
    // Example : i += 3;
    @Override
    public void visitAssignop(JCTree.JCAssignOp jcAssignOp) {
        super.visitAssignop(jcAssignOp);

        int line = cu.getLineMap().getLineNumber(jcAssignOp.pos);
        String lhsLabel = jcAssignOp.lhs.toString();

        JCTree.JCExpression assigned = (JCTree.JCExpression) result;

        result = callRecordLocalEvent(
            lhsLabel,
            assigned,
            getSourceId(),
            line,
            jcAssignOp.pos
        );
    }

    private JCTree.JCMethodInvocation callStatic(String ownerFqn, String method, List<JCTree.JCExpression> args, int pos) {
        Symbol.ClassSymbol ownerSym = elements.getTypeElement(ownerFqn);
        if (ownerSym == null) {
            throw new IllegalStateException("Type not found: " + ownerFqn);
        }

        Symbol.MethodSymbol msym = null;
        for (Symbol sym : ownerSym.members().getSymbolsByName(names.fromString(method))) {
            if (sym instanceof Symbol.MethodSymbol m) {
                msym = m;
                break;
            }
        }
        if (msym == null) {
            throw new IllegalStateException("Method not found: " + ownerFqn + "." + method);
        }

        mk.at(pos);
        JCTree.JCExpression owner = mk.Ident(ownerSym);
        JCTree.JCFieldAccess sel = mk.Select(owner, msym.name);
        sel.sym = msym;
        sel.type = msym.type;

        JCTree.JCMethodInvocation apply = mk.Apply(List.nil(), sel, List.from(args));
        apply.type = msym.getReturnType();

        return apply;
    }

    private JCTree.JCMethodInvocation callRecordLocalEvent(String label, JCTree.JCExpression value, String source, int line, int pos) {
        return callStatic(
            "ch.epfl.printwizard.plugin.logging.TraceOut",
            "recordLocalEvent",
            List.of(mk.Literal(label), value, mk.Literal(source), mk.Literal(line)),
            pos
        );
    }

    private JCTree.JCExpression makeArgArrayExpr(JCTree.JCMethodDecl md, int pos) {
        mk.at(pos);
        String argFqn = "ch.epfl.printwizard.plugin.model.trace.Arg";

        Symbol.ClassSymbol argSym = elements.getTypeElement(argFqn);
        if (argSym == null) {
            throw new IllegalStateException("Type not found: " + argFqn);
        }
        JCTree.JCExpression argTypeExpr = mk.Ident(argSym);

        java.util.List<JCTree.JCExpression> argInits = new java.util.ArrayList<>();

        for (JCTree.JCVariableDecl param : md.getParameters()) {
            String paramName = param.getName().toString();
            String paramTypeStr = param.vartype.toString();

            JCTree.JCExpression paramIdent = mk.Ident(param.sym);

            JCTree.JCMethodInvocation argCall = callStatic(
                argFqn,
                "of",
                List.of(mk.Literal(paramName), paramIdent, mk.Literal(paramTypeStr)),
                pos
            );

            argInits.add(argCall);
        }

        JCTree.JCNewArray newArr = mk.NewArray(argTypeExpr, List.nil(), List.from(argInits));

        newArr.type = types.makeArrayType(argSym.type);

        return newArr;
    }

    private boolean isArithmetic(JCTree.Tag tag) {
        return tag == JCTree.Tag.PLUS
            || tag == JCTree.Tag.MINUS
            || tag == JCTree.Tag.MUL
            || tag == JCTree.Tag.DIV
            || tag == JCTree.Tag.MOD;
    }

    private String getSourceId() {
        JavaFileObject sfo = cu.getSourceFile();

        String pkgPath = "";
        if (cu.packge != null) {
            pkgPath = cu.packge.toString().replace('.', '/');
        }

        String fileName = "Unknown.java";
        if (sfo != null) {
            String path = sfo.toUri().getPath();

            int lastSlash = path.lastIndexOf('/');
            if (lastSlash >= 0 && lastSlash < path.length() - 1) {
                fileName = path.substring(lastSlash + 1);
            } else {
                fileName = path;
            }
        }

        if (!pkgPath.isEmpty()) {
            return Ids.createSourceId(pkgPath + "/" + fileName);
        } else {
            return Ids.createSourceId(fileName);
        }
    }
}
