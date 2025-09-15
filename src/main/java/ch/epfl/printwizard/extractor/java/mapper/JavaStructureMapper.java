package ch.epfl.printwizard.extractor.java.mapper;

import ch.epfl.printwizard.model.LocalVar;
import ch.epfl.printwizard.model.structures.BlockNode;
import ch.epfl.printwizard.model.structures.ExprStmtNode;
import ch.epfl.printwizard.model.structures.StructureKind;
import ch.epfl.printwizard.model.structures.StructureNode;
import ch.epfl.printwizard.model.structures.expr.ExprCode;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;

import java.util.*;

/**
 * Maps JavaParser statements to StructureNode.
 */
public final class JavaStructureMapper {

    private final Map<String, Integer> counters = new HashMap<>();
    private int nextLocalSlot;
    private final List<LocalVar> locals = new ArrayList<>();

    public JavaStructureMapper(int paramCountStartSlot) {
        this.nextLocalSlot = paramCountStartSlot;
    }

    /**
     * Gets the local variables defined in the method.
     * @return an unmodifiable list of local variables
     */
    public List<LocalVar> getLocals() {
        return Collections.unmodifiableList(locals);
    }

    /**
     * Maps a block statement to a BlockNode.
     * @param b the block statement to map
     * @return the corresponding BlockNode
     */
    public BlockNode mapBlock(BlockStmt b) {
        List<StructureNode> kids = new ArrayList<>();
        for (Statement s : b.getStatements()) kids.add(mapStmt(s));
        int startLine = b.getRange().map(r -> r.begin.line).orElse(0);
        int endLine = b.getRange().map(r -> r.end.line).orElse(0);
        
        return new BlockNode(nextId(StructureKind.BLOCK.getPrefixId()), StructureKind.BLOCK, startLine, endLine, kids);
    }

    /**
     * Maps any statement to a StructureNode.
     * @param s the statement to map
     * @return the corresponding StructureNode
     */
    public StructureNode mapStmt(Statement s) {
        int startLine = s.getRange().map(r -> r.begin.line).orElse(0);
        int endLine = s.getRange().map(r -> r.end.line).orElse(0);
        
        /*
        if (s.isLocalDeclarationStmt()) {
            LocalDeclarationStmt n = s.asLocalDeclarationStmt();
            List<LocalVar> declared = new ArrayList<>();
            n.getVariables().forEach(v -> {
                String name = v.getNameAsString();
                String typeId = "t:" + v.getType().toString();
                locals.add(new LocalVar(nextLocalSlot, name, typeId));
                declared.add(new LocalVar(nextLocalSlot++, name, typeId));
            });
            return new VarDeclNode(nextId("vardecl"), StructureKind.VAR_DECL, ls(n), le(n), declared);
        }

        if (s.isExpressionStmt()) {
            ExpressionStmt n = s.asExpressionStmt();
            Expression e = n.getExpression();
            return new ExprStmtNode(nextId("expr"), StructureKind.EXPR_STMT, ls(n), le(n), exprRef(e));
        }
        */

        if (s.isBlockStmt()) {
            return mapBlock(s.asBlockStmt());
        }

        // Fallback: store exact code as an "expr stmt" node for unhandled kinds.
        ExprCode exprCode = new ExprCode(
            s.toString(),
            startLine,
            endLine
        );
        
        return new ExprStmtNode(
            nextId(StructureKind.EXPR_STMT.getPrefixId()),
            startLine, endLine,
            exprCode
        );
    }

    private String nextId(String prefix) {
        int v = counters.getOrDefault(prefix, 0) + 1;
        counters.put(prefix, v);
        return prefix + ":" + v;
    }
}
