package ch.epfl.printwizard.plugin.tracing;

import ch.epfl.printwizard.plugin.logging.Ids;
import ch.epfl.printwizard.plugin.logging.OutputManager;
import ch.epfl.printwizard.plugin.model.trace.TraceFile;
import ch.epfl.printwizard.plugin.model.trace.TraceLoc;
import ch.epfl.printwizard.plugin.model.trace.TraceSpan;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
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

    /*@Override
    public void visitMethodDef(JCTree.JCMethodDecl jcMethodDecl) {
        JavaFileObject sfo = cu.getSourceFile();
        String sourcePath = (sfo != null ? sfo.toUri().getPath() : jcMethodDecl.name.toString());
        String sourceId = "src:" + sourcePath;

        int startPos = jcMethodDecl.getStartPosition();
        int endPos = jcMethodDecl.getPreferredPosition();
        if (cu.endPositions != null) {
            int ep = jcMethodDecl.getEndPosition(cu.endPositions);
            if (ep >= 0) endPos = ep;
        }

        int startLine = cu.getLineMap() != null ? cu.getLineMap().getLineNumber(startPos) : 1;
        int endLine = cu.getLineMap() != null ? cu.getLineMap().getLineNumber(endPos) : startLine;

        String methodId = "testMethodId";

        String spanId = Ids.nextSpanId();
        TraceLoc start = new TraceLoc(sourceId, startLine);
        TraceLoc end = new TraceLoc(sourceId, Math.max(startLine, endLine));

        TraceSpan span = new TraceSpan(spanId, "null", methodId, "null", "null", start, end, "OK");

        OutputManager.getTraceFileBuilder().addSpan(span);

        super.visitMethodDef(jcMethodDecl);
    }*/

    // Example : int i = 3;
    @Override
    public void visitVarDef(JCTree.JCVariableDecl jcVariableDecl) {
        if (jcVariableDecl.init != null) {
            int line = cu.getLineMap().getLineNumber(jcVariableDecl.pos);
            jcVariableDecl.init = callStatic(
                "ch.epfl.printwizard.plugin.logging.TraceOut",
                "recordLocalEvent",
                java.util.List.of(
                        mk.Literal(jcVariableDecl.getName().toString() + "@" + line),
                        jcVariableDecl.init
                ),
                jcVariableDecl.pos
            );
        }
        super.visitVarDef(jcVariableDecl);
    }

    // Example : i = 3;
    @Override
    public void visitAssign(JCTree.JCAssign jcAssign) {
        int line = cu.getLineMap().getLineNumber(jcAssign.pos);
        jcAssign.rhs = callStatic(
            "ch.epfl.printwizard.plugin.logging.TraceOut",
            "recordLocalEvent",
            java.util.List.of(
                    mk.Literal(jcAssign.lhs.toString() + "@" + line),
                    jcAssign.rhs
            ),
            jcAssign.pos
        );
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

        JCTree.JCExpression modifiedRhs = callStatic(
            "ch.epfl.printwizard.plugin.logging.TraceOut",
            "recordLocalEvent",
            java.util.List.of(
                    mk.Literal(lhsLabel + "@" + line),
                    bin
            ),
            jcAssignOp.pos
        );

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

}
