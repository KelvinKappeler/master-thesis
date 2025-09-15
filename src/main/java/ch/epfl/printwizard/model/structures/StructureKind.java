package ch.epfl.printwizard.model.structures;

/**
 * Represents an enumeration of structure kinds, such as loops or conditionals.
 */
public enum StructureKind {
    IF, WHILE, DO_WHILE, FOR, FOR_EACH,
    SWITCH_STMT, SWITCH_EXPR,
    TRY, SYNCHRONIZED, LABELED,
    RETURN, THROW, BREAK, CONTINUE, ASSERT,
    BLOCK, LOCAL_VAR_DECL, EXPR_STMT
}
