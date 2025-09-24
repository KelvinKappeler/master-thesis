package ch.epfl.printwizard.parser.extractor.java.mapper;

import ch.epfl.printwizard.shared.model.program.LocalVar;
import ch.epfl.printwizard.shared.model.program.structures.*;
import ch.epfl.printwizard.shared.model.program.structures.expr.ExprCode;
import com.github.javaparser.ast.stmt.*;

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
        */
        
        return switch (s) {
            case BlockStmt n -> {
                var kids = n.getStatements().stream().map(this::mapStmt).toList();
                yield new BlockNode(nextId(StructureKind.BLOCK.getPrefixId()), StructureKind.BLOCK, startLine, endLine, kids);
            }
            
            case IfStmt n -> {
                var cond = new ExprCode(n.getCondition().toString(), startLine, endLine);
                var thenNode = mapStmt(n.getThenStmt());
                var elseNode = n.getElseStmt().map(this::mapStmt).orElse(null);
                yield new IfNode(nextId(StructureKind.IF.getPrefixId()), startLine, endLine, cond, thenNode, elseNode);
            }
            
            case ForStmt n -> {
                var init = new ExprCode(n.getInitialization().toString(), startLine, endLine);
                var compare = new ExprCode(n.getCompare().toString(), startLine, endLine);
                var update = new ExprCode(n.getUpdate().toString(), startLine, endLine);
                var body = mapStmt(n.getBody());
                yield new ForNode(nextId(StructureKind.FOR.getPrefixId()), startLine, endLine, init, compare, update, body);
            }
            
            case ExpressionStmt n -> {
                var e = n.getExpression();
                yield new ExprStmtNode(nextId(StructureKind.EXPR_STMT.getPrefixId()), startLine, endLine,
                        new ExprCode(e.toString(), startLine, endLine));
            }

            default -> new ExprStmtNode(nextId(StructureKind.EXPR_STMT.getPrefixId()), startLine, endLine,
                    new ExprCode(s.toString(), startLine, endLine));
        };
    }

    private String nextId(String prefix) {
        int v = counters.getOrDefault(prefix, 0) + 1;
        counters.put(prefix, v);
        return prefix + ":" + v;
    }
}
