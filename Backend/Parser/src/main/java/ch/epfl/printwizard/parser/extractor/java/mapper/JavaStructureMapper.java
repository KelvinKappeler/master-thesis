package ch.epfl.printwizard.parser.extractor.java.mapper;

import ch.epfl.printwizard.shared.model.program.LocalVar;
import ch.epfl.printwizard.shared.model.program.ProgramPosition;
import ch.epfl.printwizard.shared.model.program.structures.*;
import ch.epfl.printwizard.shared.model.program.structures.expr.ExprCode;
import ch.epfl.printwizard.shared.model.program.structures.expr.ExprNode;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
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
        ProgramPosition start = lineStart(b);
        ProgramPosition end = lineEnd(b);
        
        return new BlockNode(nextId(StructureKind.BLOCK.getPrefixId()), StructureKind.BLOCK, start, end, kids);
    }

    /**
     * Maps any statement to a StructureNode.
     * @param s the statement to map
     * @return the corresponding StructureNode
     */
    public StructureNode mapStmt(Statement s) {
        ProgramPosition start = lineStart(s);
        ProgramPosition end = lineEnd(s);
        
        return switch (s) {
            case BlockStmt n -> {
                var kids = n.getStatements().stream().map(this::mapStmt).toList();
                yield new BlockNode(nextId(StructureKind.BLOCK.getPrefixId()), StructureKind.BLOCK, start, end, kids);
            }
            
            case IfStmt n -> {
                var cond = new ExprCode(n.getCondition().toString(), start, end);
                var thenNode = mapStmt(n.getThenStmt());
                var elseNode = n.getElseStmt().map(this::mapStmt).orElse(null);

                var content = "if (" + n.getCondition() + ")";
                Optional<Node> parent = n.getParentNode();
                if (parent.isPresent() && parent.get() instanceof IfStmt p) {
                    content = "else if (" + n.getCondition() + ")";
                }

                yield new IfNode(nextId(StructureKind.IF.getPrefixId()), content, start, end, cond, thenNode, elseNode);
            }
            
            case ForStmt n -> {
                var init = new ExprCode(n.getInitialization().toString(), start, end);
                var compare = new ExprCode(n.getCompare().toString(), start, end);
                var update = new ExprCode(n.getUpdate().toString(), start, end);
                var body = mapStmt(n.getBody());
                var content = n.toString();

                yield new ForNode(nextId(StructureKind.FOR.getPrefixId()), content, start, end, init, compare, update, body);
            }

            case ReturnStmt n -> {
                var expr = n.getExpression().map(e -> new ExprCode(e.toString(), start, end)).orElse(null);
                var content = n.toString();

                yield new ReturnNode(nextId(StructureKind.RETURN.getPrefixId()), content, start, end, expr);
            }

            case ExpressionStmt expr -> new ExprStmtNode(nextId(StructureKind.EXPR_STMT.getPrefixId()), start, end, mapExpr(expr));

            default -> throw new IllegalArgumentException("Unsupported statement resultType: " + s.getClass());
        };
    }

    private ExprNode mapExpr(ExpressionStmt expr) {
        ProgramPosition start = lineStart(expr);
        ProgramPosition end = lineEnd(expr);

        return switch (expr.getExpression()) {
            case VariableDeclarationExpr varExpr -> {
                varExpr.getVariables().forEach(v -> {
                    String name = v.getNameAsString();
                    String typeId = v.getType().toString();
                    LocalVar localVar = new LocalVar(nextLocalSlot++, name, typeId);
                    locals.add(localVar);
                });

                yield new ExprCode(expr.getExpression().toString(), start, end);
            }

            default -> new ExprCode(expr.getExpression().toString(), start, end);
        };
    }

    private String nextId(String prefix) {
        int v = counters.getOrDefault(prefix, 0) + 1;
        counters.put(prefix, v);
        return prefix + ":" + v;
    }

    private ProgramPosition lineStart(Statement s) {
        int startLine = s.getRange().map(r -> r.begin.line).orElse(0);
        int startColumn = s.getRange().map(r -> r.begin.column).orElse(0);

        return new ProgramPosition(startLine, startColumn);
    }

    private ProgramPosition lineEnd(Statement s) {
        int endLine = s.getRange().map(r -> r.end.line).orElse(0);
        int endColumn = s.getRange().map(r -> r.end.column).orElse(0);

        return new ProgramPosition(endLine, endColumn);
    }
}
