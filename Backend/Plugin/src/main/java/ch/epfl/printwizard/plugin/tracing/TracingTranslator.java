package ch.epfl.printwizard.plugin.tracing;

import ch.epfl.printwizard.plugin.model.trace.events.LoopKind;
import ch.epfl.printwizard.plugin.utils.Ids;
import ch.epfl.printwizard.plugin.model.trace.TraceFile;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Names;
import com.sun.tools.javac.model.JavacElements;

import javax.tools.JavaFileObject;
import java.awt.*;

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
    private final Symtab symtab;

    private final TraceFile.Builder traceFileBuilder;

    private Symbol.MethodSymbol currentMethod;

    public TracingTranslator(Context ctx, JCTree.JCCompilationUnit cu) {
        this.ctx = ctx;
        this.cu = cu;

        this.mk = TreeMaker.instance(ctx);
        this.names = Names.instance(ctx);
        this.trees = JavacTrees.instance(ctx);
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

        JCTree.JCExpression argsArrayExpr = makeArgArrayExpr(jcMethodDecl.getParameters(), jcMethodDecl.pos);

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

        this.currentMethod = previousMethod;
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
            List.of(mk.Literal(sourceId), mk.Literal(line)),
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

        JCTree.JCExpression boolTypeTree = mk.TypeIdent(com.sun.tools.javac.code.TypeTag.BOOLEAN);
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

        if (isComparison(jcBinary.getTag())) {
            result = callStatic(
                "ch.epfl.printwizard.plugin.logging.TraceOut",
                "recordComparison",
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
    }

    // Example : int i = 3; int[] array = { 1, 2, 3 };
    @Override
    public void visitVarDef(JCTree.JCVariableDecl jcVariableDecl) {
        if (jcVariableDecl.init != null) {
            int line = cu.getLineMap().getLineNumber(jcVariableDecl.pos);
            String label = makeLabel(jcVariableDecl.sym);

            if (jcVariableDecl.init instanceof JCTree.JCNewArray newArr && newArr.elems != null) {
                JCTree.JCExpression call = callStatic(
                    "ch.epfl.printwizard.plugin.logging.TraceOut",
                    "recordArrayInit",
                    List.of(
                        mk.Literal(label),
                        mk.Literal(jcVariableDecl.getName().toString()),
                        jcVariableDecl.init,
                        mk.Literal(getSourceId()),
                        mk.Literal(line)
                    ),
                    jcVariableDecl.pos
                );

                JCTree.JCExpression casted = mk.TypeCast(jcVariableDecl.vartype, call);
                casted.type = jcVariableDecl.vartype.type;

                jcVariableDecl.init = casted;
            }
            else {
                jcVariableDecl.init = callRecordLocalEvent(label, jcVariableDecl.getName().toString(), jcVariableDecl.init, getSourceId(), line, jcVariableDecl.pos);
            }
        }
        super.visitVarDef(jcVariableDecl);
    }

    // Examples : i = 3; array[2] = 3;
    @Override
    public void visitAssign(JCTree.JCAssign jcAssign) {
        int line = cu.getLineMap().getLineNumber(jcAssign.pos);
        String sourceId = getSourceId();

        if (jcAssign.lhs instanceof JCTree.JCArrayAccess arrAccess) {

            String label = arrAccess.indexed.toString();
            if (arrAccess.indexed instanceof JCTree.JCIdent id && id.sym instanceof Symbol.VarSymbol varSym && varSym.owner instanceof Symbol.MethodSymbol) {
                label = makeLabel(varSym);
            }
            
            jcAssign.rhs = callStatic(
                "ch.epfl.printwizard.plugin.logging.TraceOut",
                "recordArrayStore",
                List.of(
                    mk.Literal(label),
                    mk.Literal(arrAccess.indexed.toString()),
                    arrAccess.index,
                    jcAssign.rhs,
                    mk.Literal(sourceId),
                    mk.Literal(line)
                ),
                jcAssign.pos
            );

            super.visitAssign(jcAssign);

            return;
        }

        if (jcAssign.lhs instanceof JCTree.JCFieldAccess fieldAccess) {
            JCTree.JCExpression targetExpr = fieldAccess.selected;
            String fieldName = fieldAccess.name.toString();

            JCTree.JCExpression translatedRhs = translate(jcAssign.rhs);

            jcAssign.rhs = callStatic(
                "ch.epfl.printwizard.plugin.logging.TraceOut",
                "recordFieldWrite",
                List.of(targetExpr, mk.Literal(fieldName), translatedRhs, mk.Literal(sourceId), mk.Literal(line)),
                jcAssign.pos
            );

            super.visitAssign(jcAssign);

            return;
        }

        String label = jcAssign.lhs.toString();
        if (jcAssign.lhs instanceof JCTree.JCIdent id && id.sym instanceof Symbol.VarSymbol varSym && varSym.owner instanceof Symbol.MethodSymbol) {
            label = makeLabel(varSym);
        }
        jcAssign.rhs = callRecordLocalEvent(label, jcAssign.lhs.toString(), jcAssign.rhs, sourceId, line, jcAssign.pos);

        super.visitAssign(jcAssign);
    }
    
    // Example : i += 3;
    @Override
    public void visitAssignop(JCTree.JCAssignOp jcAssignOp) {
        super.visitAssignop(jcAssignOp);

        int line = cu.getLineMap().getLineNumber(jcAssignOp.pos);
        String sourceId = getSourceId();
        
        if (jcAssignOp.lhs instanceof JCTree.JCArrayAccess arrAccess) {
            String arrayNameText = arrAccess.indexed.toString();
            
            String label = arrayNameText;
            if (arrAccess.indexed instanceof JCTree.JCIdent id && id.sym instanceof Symbol.VarSymbol varSym && varSym.owner instanceof Symbol.MethodSymbol) {
                label = makeLabel(varSym);
            }
            
            JCTree.JCExpression assigned = (JCTree.JCExpression) result;
            
            result = callStatic(
                "ch.epfl.printwizard.plugin.logging.TraceOut",
                "recordArrayStore",
                List.of(
                    mk.Literal(label),
                    mk.Literal(arrayNameText),
                    arrAccess.index,
                    assigned,
                    mk.Literal(sourceId),
                    mk.Literal(line)
                ),
                jcAssignOp.pos
            );

            return;
        }
        
        String lhsText = jcAssignOp.lhs.toString();
        String label = lhsText;
        if (jcAssignOp.lhs instanceof JCTree.JCIdent id && id.sym instanceof Symbol.VarSymbol varSym && varSym.owner instanceof Symbol.MethodSymbol) {
            label = makeLabel(varSym);
        }

        JCTree.JCExpression assigned = (JCTree.JCExpression) result;

        result = callRecordLocalEvent(label, lhsText, assigned, sourceId, line, jcAssignOp.pos);
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

    @Override
    public void visitNewClass(JCTree.JCNewClass jcNewClass) {
        super.visitNewClass(jcNewClass);

        int line = cu.getLineMap().getLineNumber(jcNewClass.pos);
        String sourceId = getSourceId();
        
        String typeName;
        if (jcNewClass.clazz != null && jcNewClass.clazz.type != null) {
            typeName = jcNewClass.clazz.type.toString();
        } else if (jcNewClass.type != null) {
            typeName = jcNewClass.type.toString();
        } else {
            typeName = jcNewClass.clazz != null ? jcNewClass.clazz.toString() : "java.lang.Object";
        }
        
        JCTree.JCExpression newExpr = (JCTree.JCExpression) this.result;

        JCTree.JCExpression wrapped = callStatic(
            "ch.epfl.printwizard.plugin.logging.TraceOut",
            "recordNewObject",
            List.of(newExpr, mk.Literal(typeName), mk.Literal(sourceId), mk.Literal(line)),
            jcNewClass.pos
        );
        
        wrapped.type = jcNewClass.type;

        this.result = wrapped;
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

        JCTree.JCExpression boolTypeTree = mk.TypeIdent(com.sun.tools.javac.code.TypeTag.BOOLEAN);
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

        java.util.List<JCTree.JCExpression> argInits = new java.util.ArrayList<>();

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
}
