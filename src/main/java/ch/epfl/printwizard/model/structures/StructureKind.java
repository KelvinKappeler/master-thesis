package ch.epfl.printwizard.model.structures;

/**
 * Represents an enumeration of structure kinds, such as loops or conditionals.
 */
public enum StructureKind {
    IF("if"),
    WHILE("while"),
    DO_WHILE("do_while"),
    FOR("for"),
    FOR_EACH("for_each"),
    SWITCH_STMT("switch_stmt"),
    SWITCH_EXPR("switch_expr"),
    TRY("try"),
    SYNCHRONIZED("synchronized"),
    LABELED("labeled"),
    RETURN("return"),
    THROW("throw"),
    BREAK("break"),
    CONTINUE("continue"),
    ASSERT("assert"),
    BLOCK("block"),
    LOCAL_VAR_DECL("loc_var_decl"),
    EXPR_STMT("expr_stmt");
    
    private final String prefixId;
    
    StructureKind(String prefixId) {
        this.prefixId = prefixId;
    }

    /**
     * Gets the prefix ID associated with the structure kind.
     * @return the prefix ID
     */
    public String getPrefixId() {
        return prefixId;
    }
}
