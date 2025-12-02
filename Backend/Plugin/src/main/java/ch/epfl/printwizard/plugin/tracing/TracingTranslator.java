package ch.epfl.printwizard.plugin.tracing;

import ch.epfl.printwizard.plugin.model.trace.events.ConditionKind;
import ch.epfl.printwizard.plugin.model.trace.events.LoopKind;
import ch.epfl.printwizard.plugin.utils.Ids;
import ch.epfl.printwizard.plugin.model.trace.TraceFile;
import ch.epfl.printwizard.plugin.utils.UserPackages;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Names;
import com.sun.tools.javac.model.JavacElements;

import javax.tools.JavaFileObject;
import java.util.ArrayList;

/**
 * Represents the scanner for the PrintWizard instrumentation.
 */
public class TracingTranslator extends TreeTranslator {
    
    private final JCTree.JCCompilationUnit cu;
    private final TreeMaker mk;
    private final Names names;
    private final JavacElements elements;
    private final Types types;
    private final Symtab symtab;
    private final TraceFile.Builder traceFileBuilder;

    private Symbol.MethodSymbol currentMethod;

    public TracingTranslator(Context ctx, JCTree.JCCompilationUnit cu) {
        this.cu = cu;
        this.mk = TreeMaker.instance(ctx);
        this.names = Names.instance(ctx);
        this.elements = JavacElements.instance(ctx);
        this.types = Types.instance(ctx);
        this.symtab = Symtab.instance(ctx);
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
        Symbol.MethodSymbol previousMethod = this.currentMethod;
        this.currentMethod = jcMethodDecl.sym;

        int startLine = cu.getLineMap().getLineNumber(jcMethodDecl.pos);
        String owner = cu.packge != null ? cu.packge.toString() : "";
        String returnType = (jcMethodDecl.getReturnType() != null) ? jcMethodDecl.getReturnType().toString() : "void";

        boolean isStatic = jcMethodDecl.sym != null && jcMethodDecl.sym.isStatic();
        Symbol.ClassSymbol clsSym = null;
        if (jcMethodDecl.sym != null && jcMethodDecl.sym.owner instanceof Symbol.ClassSymbol cs) {
            clsSym = cs;
        }

        JCTree.JCExpression thisArgExpr;
        if (!isStatic && clsSym != null) {
            Symbol.VarSymbol thisSym = new Symbol.VarSymbol(
                Flags.SYNTHETIC,
                names._this,
                clsSym.type,
                jcMethodDecl.sym
            );
            thisArgExpr = mk.Ident(thisSym);
            thisArgExpr.type = clsSym.type;
        } else {
            JCTree.JCLiteral nullLit = mk.Literal(TypeTag.BOT, null);
            nullLit.type = symtab.botType;
            thisArgExpr = nullLit;
        }

        JCTree.JCExpression argsArrayExpr = makeArgArrayExpr(jcMethodDecl.getParameters(), jcMethodDecl.pos);

        JCTree.JCStatement enterCall = mk.Exec(
            callStatic("ch.epfl.printwizard.plugin.logging.TraceOut", "onEnter",
                List.of(
                    thisArgExpr,
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

            boolean isConstructor = jcMethodDecl.name.contentEquals("<init>");

            if (isConstructor) {
                String typeName = "";
                if (jcMethodDecl.sym != null && jcMethodDecl.sym.owner != null) {
                    typeName = jcMethodDecl.sym.owner.getQualifiedName().toString();
                }

                JCTree.JCStatement newEventCall = mk.Exec(
                    callStatic(
                        "ch.epfl.printwizard.plugin.logging.TraceOut",
                        "recordNewObject",
                        List.of(
                            thisArgExpr,
                            mk.Literal(typeName),
                            mk.Literal(getSourceId()),
                            mk.Literal(startLine)
                        ),
                        jcMethodDecl.pos
                    )
                );

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

                List<JCTree.JCStatement> newStmts = List.of(superOrThisStmt, enterCall, newEventCall).appendList(rest);

                JCTree.JCLiteral nullLit = mk.Literal(TypeTag.BOT, null);
                nullLit.type = symtab.botType;

                JCTree.JCMethodInvocation beginCall = callStatic(
                    "ch.epfl.printwizard.plugin.logging.TraceOut",
                    "beginReturn",
                    List.of(mk.Literal(getSourceId()), mk.Literal(endLine)),
                    ep
                );

                JCTree.JCStatement implicitExitCall = mk.Exec(
                    callStatic(
                        "ch.epfl.printwizard.plugin.logging.TraceOut",
                        "endReturn",
                        List.of(beginCall, nullLit),
                        ep
                    )
                );

                newStmts = newStmts.append(implicitExitCall);

                jcMethodDecl.body = mk.Block(0, newStmts);
            } else {
                List<JCTree.JCStatement> stmts = originalBody.getStatements();

                boolean isVoid = jcMethodDecl.sym != null && jcMethodDecl.sym.getReturnType() != null && jcMethodDecl.sym.getReturnType().getTag() == TypeTag.VOID;

                if (isVoid) {
                    JCTree.JCLiteral nullLit = mk.Literal(TypeTag.BOT, null);
                    nullLit.type = symtab.botType;

                    JCTree.JCMethodInvocation beginCall = callStatic(
                        "ch.epfl.printwizard.plugin.logging.TraceOut",
                        "beginReturn",
                        List.of(mk.Literal(getSourceId()), mk.Literal(endLine)),
                        ep
                    );

                    JCTree.JCStatement implicitExitCall = mk.Exec(
                        callStatic(
                            "ch.epfl.printwizard.plugin.logging.TraceOut",
                            "endReturn",
                            List.of(beginCall, nullLit),
                            ep
                        )
                    );

                    stmts = stmts.append(implicitExitCall);
                }

                jcMethodDecl.body = mk.Block(0, stmts.prepend(enterCall));
            }
        }

        super.visitMethodDef(jcMethodDecl);

        this.currentMethod = previousMethod;
    }

    @Override
    public void visitReturn(JCTree.JCReturn jcReturn) {
        JCTree.JCExpression trExpr = jcReturn.expr == null ? null : translate(jcReturn.expr);

        int line = cu.getLineMap().getLineNumber(jcReturn.pos);
        String sourceId = getSourceId();
        
        if (trExpr == null) {
            JCTree.JCLiteral nullLit = mk.Literal(TypeTag.BOT, null);
            nullLit.type = symtab.botType;

            JCTree.JCMethodInvocation beginCall = callStatic(
                "ch.epfl.printwizard.plugin.logging.TraceOut",
                "beginReturn",
                List.of(mk.Literal(sourceId), mk.Literal(line)),
                jcReturn.pos
            );

            JCTree.JCStatement logStmt = mk.Exec(
                callStatic(
                    "ch.epfl.printwizard.plugin.logging.TraceOut",
                    "endReturn",
                    List.of(beginCall, nullLit),
                    jcReturn.pos
                )
            );

            JCTree.JCReturn newReturn = mk.Return(null);
            this.result = mk.Block(0, List.of(logStmt, newReturn));

            return;
        }
        
        JCTree.JCMethodInvocation beginCall = callStatic(
            "ch.epfl.printwizard.plugin.logging.TraceOut",
            "beginReturn",
            List.of(mk.Literal(sourceId), mk.Literal(line)),
            jcReturn.pos
        );

        JCTree.JCMethodInvocation endCall = callStatic(
            "ch.epfl.printwizard.plugin.logging.TraceOut",
            "endReturn",
            List.of(beginCall, trExpr),
            jcReturn.pos
        );
        
        endCall.type = trExpr.type;

        this.result = mk.Return(endCall);
    }

    @Override
    public void visitExec(JCTree.JCExpressionStatement jcExpressionStatement) {
        if (jcExpressionStatement.expr instanceof JCTree.JCMethodInvocation mi) {
            JCTree.JCMethodInvocation trMi = translate(mi);

            Symbol sym = null;
            if (trMi.meth instanceof JCTree.JCFieldAccess fa) {
                sym = fa.sym;
            } else if (trMi.meth instanceof JCTree.JCIdent id) {
                sym = id.sym;
            }

            if (sym instanceof Symbol.MethodSymbol msym && isExternalMethod(msym)) {
                int line = cu.getLineMap().getLineNumber(mi.pos);
                String ownerFqn = msym.owner.getQualifiedName().toString();
                String methodName = msym.getSimpleName().toString();
                String returnTypeStr = msym.getReturnType().toString();

                List<JCTree.JCStatement> tmpDecls = List.nil();
                java.util.List<JCTree.JCExpression> tmpIdents = new java.util.ArrayList<>();

                List<JCTree.JCExpression> trArgs = trMi.args;
                int idx = 0;
                for (JCTree.JCExpression argExpr : trArgs) {
                    Type argType = (argExpr.type != null) ? argExpr.type : symtab.objectType;

                    String tmpNameStr = "__pw_arg_" + mi.pos + "_" + idx;
                    var tmpName = names.fromString(tmpNameStr);

                    JCTree.JCExpression typeTree = argType.isPrimitive() ? mk.TypeIdent(argType.getTag()) : mk.QualIdent(argType.tsym);

                    JCTree.JCVariableDecl tmpVar = mk.VarDef(
                        mk.Modifiers(Flags.SYNTHETIC),
                        tmpName,
                        typeTree,
                        argExpr
                    );

                    Symbol.VarSymbol tmpSym = new Symbol.VarSymbol(
                        Flags.SYNTHETIC,
                        tmpName,
                        argType,
                        (currentMethod != null ? currentMethod : symtab.noSymbol)
                    );
                    tmpVar.sym = tmpSym;
                    tmpVar.type = argType;

                    tmpDecls = tmpDecls.append(tmpVar);

                    JCTree.JCExpression tmpIdent = mk.Ident(tmpSym);
                    tmpIdent.type = argType;
                    tmpIdents.add(tmpIdent);

                    idx++;
                }

                trMi.args = List.from(tmpIdents);

                JCTree.JCExpression argsArrayExpr = makeArgArrayForCall(trMi.args, msym, mi.pos);

                JCTree.JCStatement callEventStmt = mk.Exec(
                    callStatic(
                        "ch.epfl.printwizard.plugin.logging.TraceOut",
                        "recordCall",
                        List.of(
                            mk.Literal(ownerFqn),
                            mk.Literal(methodName),
                            argsArrayExpr,
                            mk.Literal(returnTypeStr),
                            mk.Literal(true),
                            mk.Literal(getSourceId()),
                            mk.Literal(line)
                        ),
                        mi.pos
                    )
                );

                JCTree.JCStatement originalCallStmt = mk.Exec(trMi);

                this.result = mk.Block(0, tmpDecls.append(callEventStmt).append(originalCallStmt));

                return;
            }

            this.result = mk.Exec(trMi);

            return;
        }

        super.visitExec(jcExpressionStatement);
    }

    @Override
    public void visitIf(JCTree.JCIf jcIf) {
        JCTree.JCExpression condExpr = translate(jcIf.cond);
        JCTree.JCStatement thenStmt = jcIf.thenpart == null ? mk.Block(0, List.nil()) : translate(jcIf.thenpart);
        JCTree.JCStatement elseStmt = jcIf.elsepart == null ? null : translate(jcIf.elsepart);

        int line = cu.getLineMap().getLineNumber(jcIf.pos);
        String sourceId = getSourceId();

        // String __pw_cond_evt_<pos> = TraceOut.beginCondition(sourceId, line);
        String condEvtVarNameStr = "__pw_cond_evt_" + jcIf.pos;
        var condEvtVarName = names.fromString(condEvtVarNameStr);

        JCTree.JCExpression stringTypeTree = mk.QualIdent(symtab.stringType.tsym);
        stringTypeTree.type = symtab.stringType;

        JCTree.JCMethodInvocation beginCondCall = callStatic(
            "ch.epfl.printwizard.plugin.logging.TraceOut",
            "beginCondition",
            List.of(mk.Literal(sourceId), mk.Literal(line), mk.Literal(ConditionKind.IF_STATEMENT.name())),
            jcIf.pos
        );

        JCTree.JCVariableDecl condEvtVar = mk.VarDef(mk.Modifiers(Flags.SYNTHETIC), condEvtVarName, stringTypeTree, beginCondCall);
        Symbol.VarSymbol condEvtSym = new Symbol.VarSymbol(
            Flags.SYNTHETIC,
            condEvtVarName,
            symtab.stringType,
            (currentMethod != null ? currentMethod : symtab.noSymbol)
        );
        condEvtVar.sym = condEvtSym;
        condEvtVar.type = symtab.stringType;

        // boolean __pw_cond_<pos> = <condition>;
        String condVarNameStr = "__pw_cond_" + jcIf.pos;
        var condVarName = names.fromString(condVarNameStr);

        JCTree.JCExpression boolTypeTree = mk.TypeIdent(TypeTag.BOOLEAN);
        boolTypeTree.type = symtab.booleanType;

        JCTree.JCVariableDecl condVar = mk.VarDef(mk.Modifiers(Flags.SYNTHETIC), condVarName, boolTypeTree, condExpr);
        Symbol.VarSymbol condSym = new Symbol.VarSymbol(
            Flags.SYNTHETIC,
            condVarName,
            symtab.booleanType,
            (currentMethod != null ? currentMethod : symtab.noSymbol)
        );
        condVar.sym = condSym;
        condVar.type = symtab.booleanType;

        // TraceOut.endCondition(__pw_cond_evt_<pos>);
        JCTree.JCStatement endCondStmt = mk.Exec(
            callStatic(
                "ch.epfl.printwizard.plugin.logging.TraceOut",
                "endCondition",
                List.of(mk.Ident(condEvtSym)),   // ident AVEC sym
                jcIf.pos
            )
        );

        /*
         * then/else
         *
         * if (__pw_cond_<pos>) {
         *   TraceOut.beginThenBlock(__pw_cond_evt_<pos>);
         *   <then>
         *   TraceOut.endThenBlock(__pw_cond_evt_<pos>);
         * } else {
         *   TraceOut.beginElseBlock(__pw_cond_evt_<pos>);
         *   <else>
         *   TraceOut.endElseBlock(__pw_cond_evt_<pos>);
         * }
         */
        JCTree.JCStatement beginThen = mk.Exec(
            callStatic(
                "ch.epfl.printwizard.plugin.logging.TraceOut",
                "beginThenBlock",
                List.of(mk.Ident(condEvtSym)),
                jcIf.pos
            )
        );
        JCTree.JCStatement endThen = mk.Exec(
            callStatic(
                "ch.epfl.printwizard.plugin.logging.TraceOut",
                "endThenBlock",
                List.of(mk.Ident(condEvtSym)),
                jcIf.pos
            )
        );
        JCTree.JCBlock tracedThen = mk.Block(0, List.of(beginThen, thenStmt, endThen));

        JCTree.JCStatement tracedElse = null;
        if (elseStmt != null) {
            JCTree.JCStatement beginElse = mk.Exec(
                callStatic(
                    "ch.epfl.printwizard.plugin.logging.TraceOut",
                    "beginElseBlock",
                    List.of(mk.Ident(condEvtSym)),
                    jcIf.pos
                )
            );
            JCTree.JCStatement endElse = mk.Exec(
                callStatic(
                    "ch.epfl.printwizard.plugin.logging.TraceOut",
                    "endElseBlock",
                    List.of(mk.Ident(condEvtSym)),
                    jcIf.pos
                )
            );
            tracedElse = mk.Block(0, List.of(beginElse, elseStmt, endElse));
        }

        JCTree.JCIf newIf = mk.If(mk.Ident(condSym), tracedThen, tracedElse);

        /*
         * {
         *   String __pw_cond_evt = TraceOut.beginCondition(...);
         *   boolean __pw_cond = ...;
         *   if (__pw_cond) { ... } else { ... }
         *   TraceOut.endCondition(__pw_cond_evt);
         * }
         */
        this.result = mk.Block(0, List.of(condEvtVar, condVar, newIf, endCondStmt));
    }

    @Override
    public void visitConditional(JCTree.JCConditional jcConditional) {
        JCTree.JCExpression condExpr = translate(jcConditional.cond);
        JCTree.JCExpression thenExpr = translate(jcConditional.truepart);
        JCTree.JCExpression elseExpr = translate(jcConditional.falsepart);

        int line = cu.getLineMap().getLineNumber(jcConditional.pos);
        String sourceId = getSourceId();

        mk.at(jcConditional.pos);
        
        JCTree.JCExpression condThunk = makeBoolLambda(condExpr, jcConditional.pos);
        JCTree.JCExpression thenThunk = makeLambda(thenExpr, jcConditional.pos);
        JCTree.JCExpression elseThunk = makeLambda(elseExpr, jcConditional.pos);
        
        JCTree.JCMethodInvocation call = callStatic(
            "ch.epfl.printwizard.plugin.logging.TraceOut",
            "evalTernary",
            List.of(
                mk.Literal(sourceId), mk.Literal(line),
                condThunk, thenThunk, elseThunk
            ),
            jcConditional.pos
        );
        call.type = symtab.objectType;
        
        JCTree.JCExpression targetTypeTree = jcConditional.type != null && !jcConditional.type.isPrimitive()
            ? mk.QualIdent(jcConditional.type.tsym) : mk.TypeIdent(TypeTag.BOT);

        JCTree.JCTypeCast cast = mk.TypeCast(targetTypeTree, call);
        cast.type = jcConditional.type;

        this.result = cast;
    }

    @Override
    public void visitWhileLoop(JCTree.JCWhileLoop jcWhileLoop) {
        JCTree.JCExpression condExpr = translate(jcWhileLoop.cond);
        JCTree.JCStatement bodyStmt = jcWhileLoop.body == null ? mk.Block(0, List.nil()) : translate(jcWhileLoop.body);

        this.result = makeInstrumentedLoop(
            LoopKind.WHILE,
            condExpr,
            bodyStmt,
            List.nil(),
            List.nil(),
            jcWhileLoop.pos
        );
    }
    
    @Override
    public void visitForLoop(JCTree.JCForLoop jcForLoop) {
        List<JCTree.JCStatement> initStmts = List.nil();
        for (JCTree.JCStatement init : jcForLoop.init) {
            JCTree.JCStatement trInit = translate(init);
            initStmts = initStmts.append(trInit);
        }

        JCTree.JCExpression condExpr;
        if (jcForLoop.cond == null) {
            mk.at(jcForLoop.pos);
            JCTree.JCLiteral litTrue = mk.Literal(true);
            litTrue.type = symtab.booleanType;
            condExpr = litTrue;
        } else {
            condExpr = translate(jcForLoop.cond);
        }

        JCTree.JCStatement bodyStmt = jcForLoop.body == null ? mk.Block(0, List.nil()) : translate(jcForLoop.body);

        List<JCTree.JCStatement> updateStmts = List.nil();
        for (JCTree.JCExpressionStatement step : jcForLoop.step) {
            JCTree.JCStatement trStep = translate(step);
            updateStmts = updateStmts.append(trStep);
        }

        this.result = makeInstrumentedLoop(
            LoopKind.FOR,
            condExpr,
            bodyStmt,
            initStmts,
            updateStmts,
            jcForLoop.pos
        );
    }
    
    @Override
    public void visitDoLoop(JCTree.JCDoWhileLoop jcDoWhileLoop) {
        super.visitDoLoop(jcDoWhileLoop);
    }
    
    @Override
    public void visitForeachLoop(JCTree.JCEnhancedForLoop jcEnhancedForLoop) {
        super.visitForeachLoop(jcEnhancedForLoop);
    }

    @Override
    public void visitBinary(JCTree.JCBinary jcBinary) {
        super.visitBinary(jcBinary);

        int line = cu.getLineMap().getLineNumber(jcBinary.pos);

        if (isArithmetic(jcBinary.getTag())) {
            String op = jcBinary.getTag().toString();
            String sourceId = getSourceId();

            JCTree.JCMethodInvocation beginArith = callStatic(
                "ch.epfl.printwizard.plugin.logging.TraceOut",
                "beginArithmetic",
                List.of(mk.Literal(op), mk.Literal(sourceId), mk.Literal(line)),
                jcBinary.pos
            );

            JCTree.JCMethodInvocation beginLeft = callStatic(
                "ch.epfl.printwizard.plugin.logging.TraceOut",
                "beginArithmeticLeft",
                List.nil(),
                jcBinary.pos
            );

            JCTree.JCMethodInvocation endLeft = callStatic(
                "ch.epfl.printwizard.plugin.logging.TraceOut",
                "endArithmeticLeft",
                List.of(beginLeft, jcBinary.lhs),
                jcBinary.pos
            );

            JCTree.JCMethodInvocation beginRight = callStatic(
                "ch.epfl.printwizard.plugin.logging.TraceOut",
                "beginArithmeticRight",
                List.nil(),
                jcBinary.pos
            );

            JCTree.JCMethodInvocation endRight = callStatic(
                "ch.epfl.printwizard.plugin.logging.TraceOut",
                "endArithmeticRight",
                List.of(beginRight, jcBinary.rhs),
                jcBinary.pos
            );

            result = callStatic(
                "ch.epfl.printwizard.plugin.logging.TraceOut",
                "endArithmetic",
                List.of(beginArith, endLeft, endRight, (JCTree.JCExpression) result),
                jcBinary.pos
            );
        }

        if (isComparison(jcBinary.getTag())) {
            JCTree.JCMethodInvocation beginCmp = callStatic(
                "ch.epfl.printwizard.plugin.logging.TraceOut",
                "beginComparison",
                List.of(mk.Literal(jcBinary.getTag().toString()), mk.Literal(getSourceId()), mk.Literal(line)),
                jcBinary.pos
            );

            JCTree.JCMethodInvocation beginLeft = callStatic(
                "ch.epfl.printwizard.plugin.logging.TraceOut",
                "beginComparisonLeft",
                List.nil(),
                jcBinary.pos
            );

            JCTree.JCMethodInvocation endLeft = callStatic(
                "ch.epfl.printwizard.plugin.logging.TraceOut",
                "endComparisonLeft",
                List.of(beginLeft, jcBinary.lhs),
                jcBinary.pos
            );

            JCTree.JCMethodInvocation beginRight = callStatic(
                "ch.epfl.printwizard.plugin.logging.TraceOut",
                "beginComparisonRight",
                List.nil(),
                jcBinary.pos
            );

            JCTree.JCMethodInvocation endRight = callStatic(
                "ch.epfl.printwizard.plugin.logging.TraceOut",
                "endComparisonRight",
                List.of(beginRight, jcBinary.rhs),
                jcBinary.pos
            );

            JCTree.JCMethodInvocation endCmp = callStatic(
                "ch.epfl.printwizard.plugin.logging.TraceOut",
                "endComparison",
                List.of(beginCmp, endLeft, endRight),
                jcBinary.pos
            );
            endCmp.type = symtab.booleanType;

            result = endCmp;
        }
    }

    // Example : int i = 3; int[] array = { 1, 2, 3 };
    @Override
    public void visitVarDef(JCTree.JCVariableDecl jcVariableDecl) {
        if (jcVariableDecl.sym instanceof Symbol.VarSymbol varSym && varSym.owner instanceof Symbol.MethodSymbol) {

            if (jcVariableDecl.init != null) {
                int line = cu.getLineMap().getLineNumber(jcVariableDecl.pos);
                String sourceId = getSourceId();
                String label = makeLabel(varSym);

                JCTree.JCExpression translatedInit = translate(jcVariableDecl.init);

                JCTree.JCMethodInvocation beginCall = callStatic(
                    "ch.epfl.printwizard.plugin.logging.TraceOut",
                    "beginLocal",
                    List.of(
                        mk.Literal(label),
                        mk.Literal(jcVariableDecl.getName().toString()),
                        mk.Literal(sourceId),
                        mk.Literal(line)
                    ),
                    jcVariableDecl.pos
                );

                jcVariableDecl.init = callStatic(
                    "ch.epfl.printwizard.plugin.logging.TraceOut",
                    "endLocal",
                    List.of(beginCall, translatedInit),
                    jcVariableDecl.pos
                );

                this.result = jcVariableDecl;

                return;
            }
        }

        super.visitVarDef(jcVariableDecl);
    }

    // Examples : i = 3; array[2] = 3;
    @Override
    public void visitAssign(JCTree.JCAssign jcAssign) {
        int line = cu.getLineMap().getLineNumber(jcAssign.pos);
        String sourceId = getSourceId();

        // array[index] = rhs;
        if (jcAssign.lhs instanceof JCTree.JCArrayAccess arrAccess) {
            String arrayNameText = arrAccess.indexed.toString();
            String label = arrayNameText;
            if (arrAccess.indexed instanceof JCTree.JCIdent id && id.sym instanceof Symbol.VarSymbol varSym && varSym.owner instanceof Symbol.MethodSymbol) {
                label = makeLabel(varSym);
            }

            JCTree.JCExpression translatedRhs = translate(jcAssign.rhs);

            JCTree.JCMethodInvocation beginCall = callStatic(
                "ch.epfl.printwizard.plugin.logging.TraceOut",
                "beginArrayStore",
                List.of(
                    mk.Literal(label),
                    arrAccess.indexed,
                    mk.Literal(arrayNameText),
                    arrAccess.index,
                    mk.Literal(sourceId),
                    mk.Literal(line)
                ),
                jcAssign.pos
            );

            JCTree.JCAssign assignExpr = mk.Assign(jcAssign.lhs, translatedRhs);
            assignExpr.type = jcAssign.type;

            this.result = callStatic(
                "ch.epfl.printwizard.plugin.logging.TraceOut",
                "endArrayStore",
                List.of(beginCall, assignExpr),
                jcAssign.pos
            );

            return;
        }

        // 2) obj.field = rhs;
        if (jcAssign.lhs instanceof JCTree.JCFieldAccess fieldAccess) {
            JCTree.JCExpression targetExpr = fieldAccess.selected;
            String fieldName = fieldAccess.name.toString();
            
            JCTree.JCExpression translatedRhs = translate(jcAssign.rhs);
            
            JCTree.JCAssign assignExpr = mk.Assign(jcAssign.lhs, translatedRhs);
            assignExpr.type = jcAssign.type;
            
            JCTree.JCMethodInvocation beginCall = callStatic(
                "ch.epfl.printwizard.plugin.logging.TraceOut",
                "beginFieldWrite",
                List.of(targetExpr, mk.Literal(fieldName), mk.Literal(sourceId), mk.Literal(line)),
                jcAssign.pos
            );
            
            this.result = callStatic(
                "ch.epfl.printwizard.plugin.logging.TraceOut",
                "endFieldWrite",
                List.of(beginCall, assignExpr),
                jcAssign.pos
            );

            return;
        }

        // 3) localVar = rhs;
        if (jcAssign.lhs instanceof JCTree.JCIdent id && id.sym instanceof Symbol.VarSymbol varSym && varSym.owner instanceof Symbol.MethodSymbol) {

            String label = makeLabel(varSym);
            String varName = id.getName().toString();

            JCTree.JCExpression translatedRhs = translate(jcAssign.rhs);

            JCTree.JCAssign assignExpr = mk.Assign(jcAssign.lhs, translatedRhs);
            assignExpr.type = jcAssign.type;

            JCTree.JCMethodInvocation beginCall = callStatic(
                "ch.epfl.printwizard.plugin.logging.TraceOut",
                "beginLocal",
                List.of(mk.Literal(label), mk.Literal(varName), mk.Literal(sourceId), mk.Literal(line)),
                jcAssign.pos
            );

            this.result = callStatic(
                "ch.epfl.printwizard.plugin.logging.TraceOut",
                "endLocal",
                List.of(beginCall, assignExpr),
                jcAssign.pos
            );

            return;
        }

        super.visitAssign(jcAssign);
    }
    
    // Example : i += 3;
    @Override
    public void visitAssignop(JCTree.JCAssignOp jcAssignOp) {
        super.visitAssignop(jcAssignOp);

        int line = cu.getLineMap().getLineNumber(jcAssignOp.pos);
        String sourceId = getSourceId();

        JCTree.JCExpression assigned = (JCTree.JCExpression) result;

        // 1) array[index] op= rhs
        if (jcAssignOp.lhs instanceof JCTree.JCArrayAccess arrAccess) {
            String arrayNameText = arrAccess.indexed.toString();
            String label = arrayNameText;
            if (arrAccess.indexed instanceof JCTree.JCIdent id && id.sym instanceof Symbol.VarSymbol varSym && varSym.owner instanceof Symbol.MethodSymbol) {
                label = makeLabel(varSym);
            }

            JCTree.JCMethodInvocation beginCall = callStatic(
                "ch.epfl.printwizard.plugin.logging.TraceOut",
                "beginArrayStore",
                List.of(
                    mk.Literal(label),
                    arrAccess.indexed,
                    mk.Literal(arrayNameText),
                    arrAccess.index,
                    mk.Literal(sourceId),
                    mk.Literal(line)
                ),
                jcAssignOp.pos
            );

            result = callStatic(
                "ch.epfl.printwizard.plugin.logging.TraceOut",
                "endArrayStore",
                List.of(beginCall, assigned),
                jcAssignOp.pos
            );

            return;
        }

        // 2) localVar op= rhs
        if (jcAssignOp.lhs instanceof JCTree.JCIdent id && id.sym instanceof Symbol.VarSymbol varSym && varSym.owner instanceof Symbol.MethodSymbol) {

            String lhsText = id.toString();
            String label = makeLabel(varSym);

            JCTree.JCMethodInvocation beginCall = callStatic(
                "ch.epfl.printwizard.plugin.logging.TraceOut",
                "beginLocal",
                List.of(mk.Literal(label), mk.Literal(lhsText), mk.Literal(sourceId), mk.Literal(line)),
                jcAssignOp.pos
            );

            result = callStatic(
                "ch.epfl.printwizard.plugin.logging.TraceOut",
                "endLocal",
                List.of(beginCall, assigned),
                jcAssignOp.pos
            );
        }
    }

    @Override
    public void visitUnary(JCTree.JCUnary jcUnary) {
        super.visitUnary(jcUnary);

        int line = cu.getLineMap().getLineNumber(jcUnary.pos);
        String sourceId = getSourceId();
        JCTree.Tag tag = jcUnary.getTag();

        JCTree.JCUnary translatedUnary = (JCTree.JCUnary) result;
        JCTree.JCExpression argExpr = translatedUnary.arg;

        // ++i, i++, --i, i--
        if (tag == JCTree.Tag.PREINC || tag == JCTree.Tag.POSTINC || tag == JCTree.Tag.PREDEC || tag == JCTree.Tag.POSTDEC) {
            
            if (argExpr instanceof JCTree.JCArrayAccess arrAccess) {
                String arrayNameText = arrAccess.indexed.toString();

                String label = arrayNameText;
                if (arrAccess.indexed instanceof JCTree.JCIdent id && id.sym instanceof Symbol.VarSymbol varSym && varSym.owner instanceof Symbol.MethodSymbol) {
                    label = makeLabel(varSym);
                }
                
                result = callStatic(
                    "ch.epfl.printwizard.plugin.logging.TraceOut",
                    "recordArrayStore",
                    List.of(
                        mk.Literal(label),
                        mk.Literal(arrayNameText),
                        arrAccess.index,
                        translatedUnary,
                        mk.Literal(sourceId),
                        mk.Literal(line)
                    ),
                    jcUnary.pos
                );

                return;
            }
            
            String varText = argExpr.toString();
            String label = varText;
            if (argExpr instanceof JCTree.JCIdent id && id.sym instanceof Symbol.VarSymbol varSym && varSym.owner instanceof Symbol.MethodSymbol) {
                label = makeLabel(varSym);
            }

            result = callRecordLocalEvent(label, varText, translatedUnary, sourceId, line, jcUnary.pos);

            return;
        }
        
        if (tag == JCTree.Tag.NEG || tag == JCTree.Tag.POS) {
            JCTree.JCLiteral zeroLit;

            if (argExpr.type != null && argExpr.type.isPrimitive()) {
                switch (argExpr.type.getTag()) {
                    case LONG -> zeroLit = mk.Literal(0L);
                    case FLOAT -> zeroLit = mk.Literal(0.0f);
                    case DOUBLE -> zeroLit = mk.Literal(0.0d);
                    default -> zeroLit = mk.Literal(0);
                }
            } else {
                zeroLit = mk.Literal(0);
            }

            result = callStatic(
                "ch.epfl.printwizard.plugin.logging.TraceOut",
                "recordArithmetic",
                List.of(
                    mk.Literal(tag.toString()),
                    zeroLit,
                    argExpr,
                    translatedUnary,
                    mk.Literal(sourceId),
                    mk.Literal(line)
                ),
                jcUnary.pos
            );
        }
    }

    private JCTree.JCBlock makeInstrumentedLoop(
        LoopKind kind,
        JCTree.JCExpression condExpr,
        JCTree.JCStatement bodyStmt,
        List<JCTree.JCStatement> initStmts,
        List<JCTree.JCStatement> updateStmts,
        int pos
    ) {
        int line = cu.getLineMap().getLineNumber(pos);
        String sourceId = getSourceId();

        String loopEvtVarNameStr = "__pw_loop_evt_" + pos;
        var loopEvtVarName = names.fromString(loopEvtVarNameStr);

        JCTree.JCExpression stringTypeTree = mk.QualIdent(symtab.stringType.tsym);
        stringTypeTree.type = symtab.stringType;

        JCTree.JCMethodInvocation beginLoopCall = callStatic(
            "ch.epfl.printwizard.plugin.logging.TraceOut",
            "beginLoop",
            List.of(mk.Literal(sourceId), mk.Literal(line), mk.Literal(kind.name())),
            pos
        );

        JCTree.JCVariableDecl loopEvtVar = mk.VarDef(
            mk.Modifiers(Flags.SYNTHETIC),
            loopEvtVarName,
            stringTypeTree,
            beginLoopCall
        );
        Symbol.VarSymbol loopEvtSym = new Symbol.VarSymbol(
            Flags.SYNTHETIC,
            loopEvtVarName,
            symtab.stringType,
            (currentMethod != null ? currentMethod : symtab.noSymbol)
        );
        loopEvtVar.sym = loopEvtSym;
        loopEvtVar.type = symtab.stringType;

        JCTree.JCExpression boolTypeTree = mk.TypeIdent(TypeTag.BOOLEAN);
        boolTypeTree.type = symtab.booleanType;

        String continuedNameStr = "__pw_loop_continued_" + pos;
        var continuedName = names.fromString(continuedNameStr);
        JCTree.JCVariableDecl continuedVar = mk.VarDef(
            mk.Modifiers(Flags.SYNTHETIC),
            continuedName,
            boolTypeTree,
            mk.Literal(true)
        );
        Symbol.VarSymbol continuedSym = new Symbol.VarSymbol(
            Flags.SYNTHETIC,
            continuedName,
            symtab.booleanType,
            (currentMethod != null ? currentMethod : symtab.noSymbol)
        );
        continuedVar.sym = continuedSym;
        continuedVar.type = symtab.booleanType;

        String hasRunNameStr = "__pw_loop_hasRun_" + pos;
        var hasRunName = names.fromString(hasRunNameStr);
        JCTree.JCVariableDecl hasRunVar = mk.VarDef(
            mk.Modifiers(Flags.SYNTHETIC),
            hasRunName,
            boolTypeTree,
            mk.Literal(false)
        );
        Symbol.VarSymbol hasRunSym = new Symbol.VarSymbol(
            Flags.SYNTHETIC,
            hasRunName,
            symtab.booleanType,
            (currentMethod != null ? currentMethod : symtab.noSymbol)
        );
        hasRunVar.sym = hasRunSym;
        hasRunVar.type = symtab.booleanType;

        mk.at(pos);
        JCTree.JCExpression whileCond = mk.Ident(continuedSym);
        whileCond.type = symtab.booleanType;

        JCTree.JCExpression hasRunIdent = mk.Ident(hasRunSym);
        hasRunIdent.type = symtab.booleanType;

        String iterEvtVarNameStr = "__pw_iter_evt_" + pos;
        var iterEvtVarName = names.fromString(iterEvtVarNameStr);

        JCTree.JCMethodInvocation beginIterCall = callStatic(
            "ch.epfl.printwizard.plugin.logging.TraceOut",
            "beginLoopIteration",
            List.of(mk.Ident(loopEvtSym)),
            pos
        );

        JCTree.JCVariableDecl iterEvtVar = mk.VarDef(
            mk.Modifiers(Flags.SYNTHETIC),
            iterEvtVarName,
            stringTypeTree,
            beginIterCall
        );
        Symbol.VarSymbol iterEvtSym = new Symbol.VarSymbol(
            Flags.SYNTHETIC,
            iterEvtVarName,
            symtab.stringType,
            (currentMethod != null ? currentMethod : symtab.noSymbol)
        );
        iterEvtVar.sym = iterEvtSym;
        iterEvtVar.type = symtab.stringType;

        JCTree.JCStatement endIterStmt = mk.Exec(
            callStatic(
                "ch.epfl.printwizard.plugin.logging.TraceOut",
                "endLoopIteration",
                List.of(mk.Ident(iterEvtSym)),
                pos
            )
        );

        List<JCTree.JCStatement> bodyStmts = List.of(bodyStmt);
        if (!updateStmts.isEmpty()) {
            JCTree.JCStatement beginUpdate = mk.Exec(
                callStatic(
                    "ch.epfl.printwizard.plugin.logging.TraceOut",
                    "beginLoopUpdate",
                    List.of(mk.Ident(loopEvtSym)),
                    pos
                )
            );
            JCTree.JCStatement endUpdate = mk.Exec(
                callStatic(
                    "ch.epfl.printwizard.plugin.logging.TraceOut",
                    "endLoopUpdate",
                    List.of(mk.Ident(loopEvtSym)),
                    pos
                )
            );
            bodyStmts = bodyStmts.append(beginUpdate).appendList(updateStmts).append(endUpdate);
        }

        JCTree.JCBlock tryBlock = mk.Block(0, bodyStmts);
        JCTree.JCBlock finallyBlock = mk.Block(0, List.of(endIterStmt));
        JCTree.JCTry tryFinally = mk.Try(tryBlock, List.nil(), finallyBlock);

        JCTree.JCBlock iterBlock = mk.Block(0, List.of(iterEvtVar, tryFinally));
        JCTree.JCIf ifHasRun = mk.If(hasRunIdent, iterBlock, null);

        JCTree.JCAssign assignHasRunTrue = mk.Assign(mk.Ident(hasRunSym), mk.Literal(true));
        assignHasRunTrue.type = symtab.booleanType;
        JCTree.JCStatement hasRunAssignStmt = mk.Exec(assignHasRunTrue);

        JCTree.JCStatement beginCondStmt = mk.Exec(
            callStatic(
                "ch.epfl.printwizard.plugin.logging.TraceOut",
                "beginLoopCondition",
                List.of(mk.Ident(loopEvtSym)),
                pos
            )
        );

        JCTree.JCAssign assignContinued = mk.Assign(mk.Ident(continuedSym), condExpr);
        assignContinued.type = symtab.booleanType;
        JCTree.JCStatement continuedAssignStmt = mk.Exec(assignContinued);

        JCTree.JCStatement endCondStmt = mk.Exec(
            callStatic(
                "ch.epfl.printwizard.plugin.logging.TraceOut",
                "endLoopCondition",
                List.of(mk.Ident(loopEvtSym)),
                pos
            )
        );

        JCTree.JCBlock whileBody = mk.Block(0, List.of(ifHasRun, hasRunAssignStmt, beginCondStmt, continuedAssignStmt, endCondStmt));

        JCTree.JCWhileLoop newWhileLoop = mk.WhileLoop(whileCond, whileBody);

        JCTree.JCStatement endLoopStmt = mk.Exec(
            callStatic(
                "ch.epfl.printwizard.plugin.logging.TraceOut",
                "endLoop",
                List.of(mk.Ident(loopEvtSym)),
                pos
            )
        );

        List<JCTree.JCStatement> stmts = List.nil();
        stmts = stmts.append(loopEvtVar);

        if (!initStmts.isEmpty()) {
            JCTree.JCStatement beginInitStmt = mk.Exec(
                callStatic(
                    "ch.epfl.printwizard.plugin.logging.TraceOut",
                    "beginLoopInit",
                    List.of(mk.Ident(loopEvtSym)),
                    pos
                )
            );
            JCTree.JCStatement endInitStmt = mk.Exec(
                callStatic(
                    "ch.epfl.printwizard.plugin.logging.TraceOut",
                    "endLoopInit",
                    List.of(mk.Ident(loopEvtSym)),
                    pos
                )
            );
            stmts = stmts.append(beginInitStmt).appendList(initStmts).append(endInitStmt);
        }

        stmts = stmts.append(continuedVar).append(hasRunVar).append(newWhileLoop).append(endLoopStmt);

        return mk.Block(0, stmts);
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

    private JCTree.JCMethodInvocation callRecordLocalEvent(String label, String varName, JCTree.JCExpression value, String source, int line, int pos) {
        return callStatic(
            "ch.epfl.printwizard.plugin.logging.TraceOut",
            "recordLocalEvent",
            List.of(mk.Literal(label), mk.Literal(varName), value, mk.Literal(source), mk.Literal(line)),
            pos
        );
    }

    private JCTree.JCExpression makeArgArrayExpr(List<JCTree.JCVariableDecl> args, int pos) {
        mk.at(pos);
        String argFqn = "ch.epfl.printwizard.plugin.model.trace.Arg";

        Symbol.ClassSymbol argSym = elements.getTypeElement(argFqn);
        if (argSym == null) {
            throw new IllegalStateException("Type not found: " + argFqn);
        }
        JCTree.JCExpression argTypeExpr = mk.Ident(argSym);

        java.util.List<JCTree.JCExpression> argInits = new ArrayList<>();

        for (JCTree.JCVariableDecl param : args) {
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

    private JCTree.JCExpression makeArgArrayForCall(List<JCTree.JCExpression> args, Symbol.MethodSymbol msym, int pos) {
        mk.at(pos);
        String argFqn = "ch.epfl.printwizard.plugin.model.trace.Arg";

        Symbol.ClassSymbol argSym = elements.getTypeElement(argFqn);
        if (argSym == null) {
            throw new IllegalStateException("Type not found: " + argFqn);
        }
        JCTree.JCExpression argTypeExpr = mk.Ident(argSym);

        java.util.List<JCTree.JCExpression> argInits = new java.util.ArrayList<>();

        List<Symbol.VarSymbol> params = msym.getParameters();
        int i = 0;
        for (JCTree.JCExpression argExpr : args) {
            String paramName;
            String paramTypeStr;

            if (i < params.size()) {
                Symbol.VarSymbol p = params.get(i);
                paramName = p.getSimpleName().toString();
                paramTypeStr = p.type.toString();
            } else {
                paramName = "arg" + i;
                paramTypeStr = (argExpr.type != null) ? argExpr.type.toString() : "java.lang.Object";
            }

            JCTree.JCMethodInvocation argCall = callStatic(
                argFqn,
                "of",
                List.of(mk.Literal(paramName), argExpr, mk.Literal(paramTypeStr)),
                pos
            );

            argInits.add(argCall);
            i++;
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

    private boolean isComparison(JCTree.Tag tag) {
        return tag == JCTree.Tag.EQ
            || tag == JCTree.Tag.NE
            || tag == JCTree.Tag.LT
            || tag == JCTree.Tag.LE
            || tag == JCTree.Tag.GT
            || tag == JCTree.Tag.GE;
    }

    private String makeLabel(Symbol.VarSymbol varSym) {
        String simpleName = varSym.getSimpleName().toString();
        String sourceId = getSourceId();
        int declPos = varSym.pos;
        
        return "local:" + simpleName + "@" + sourceId + ":" + declPos;
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

    private boolean isExternalMethod(Symbol.MethodSymbol msym) {
        if (!(msym.owner instanceof Symbol.ClassSymbol clsSym)) {
            return true;
        }

        String ownerFqn = clsSym.getQualifiedName().toString();

        if (msym.isConstructor() && ownerFqn.equals("java.lang.Object")) {
            return false;
        }

        String ownerPkg = clsSym.packge().getQualifiedName().toString();

        if (ownerPkg.startsWith("ch.epfl.printwizard.plugin")) {
            return false;
        }

        return !UserPackages.isUserPackage(ownerPkg);
    }

    private JCTree.JCExpression makeBoolLambda(JCTree.JCExpression condExpr, int pos) {
        mk.at(pos);
        
        Symbol.ClassSymbol boolLambdaSym = elements.getTypeElement("ch.epfl.printwizard.plugin.logging.BoolLambda");

        JCTree.JCExpression boolLambdaType = mk.QualIdent(boolLambdaSym);
        boolLambdaType.type = boolLambdaSym.type;
        
        JCTree.JCMethodDecl getMethod = mk.MethodDef(
            mk.Modifiers(Flags.PUBLIC), names.fromString("get"), mk.TypeIdent(TypeTag.BOOLEAN),
            List.nil(), List.nil(), List.nil(),
            mk.Block(0, List.of(mk.Return(condExpr))), null
        );
        
        JCTree.JCClassDecl anonClass = mk.ClassDef(
            mk.Modifiers(0), names.empty, List.nil(),
            null, List.of(boolLambdaType), List.of(getMethod)
        );

        JCTree.JCNewClass newClass = mk.NewClass(
            null, List.nil(), boolLambdaType, List.nil(), anonClass
        );
        newClass.type = boolLambdaSym.type;

        return newClass;
    }

    private JCTree.JCExpression makeLambda(JCTree.JCExpression expr, int pos) {
        mk.at(pos);
        
        Symbol.ClassSymbol lambdaSym = elements.getTypeElement("ch.epfl.printwizard.plugin.logging.Lambda");

        JCTree.JCExpression lambdaType = mk.QualIdent(lambdaSym);
        lambdaType.type = lambdaSym.type;
        
        JCTree.JCExpression objTypeTree = mk.QualIdent(symtab.objectType.tsym);
        objTypeTree.type = symtab.objectType;
        
        JCTree.JCMethodDecl getMethod = mk.MethodDef(
            mk.Modifiers(Flags.PUBLIC), names.fromString("get"), objTypeTree,
            List.nil(), List.nil(), List.nil(),
            mk.Block(0, List.of(mk.Return(expr))),
            null
        );
        
        JCTree.JCClassDecl anonClass = mk.ClassDef(
            mk.Modifiers(0),
            names.empty,
            List.nil(),
            null,
            List.of(lambdaType),
            List.of(getMethod)
        );

        JCTree.JCNewClass newClass = mk.NewClass(null, List.nil(), lambdaType, List.nil(), anonClass);
        newClass.type = lambdaSym.type;

        return newClass;
    }
}
