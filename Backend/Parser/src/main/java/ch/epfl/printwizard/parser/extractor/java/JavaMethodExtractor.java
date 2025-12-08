package ch.epfl.printwizard.parser.extractor.java;

import ch.epfl.printwizard.parser.extractor.IExtractor;
import ch.epfl.printwizard.parser.extractor.java.mapper.JavaStructureMapper;
import ch.epfl.printwizard.shared.IdGenerator;
import ch.epfl.printwizard.shared.model.program.*;
import ch.epfl.printwizard.shared.model.program.structures.StructureNode;
import ch.epfl.printwizard.shared.utils.Preconditions;
import ch.epfl.printwizard.shared.utils.TypeIdUtils;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.stmt.BlockStmt;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Represents an extractor for Java methods.
 * @param source the source of the class
 * @param typeInfo the class/interface/record information
 */
public record JavaMethodExtractor(Source source, BaseTypeInfo typeInfo) implements IExtractor<TypeDeclaration<?>, MethodInfo> {

    private static final String CONSTRUCTOR_NAME = "<init>";

    /**
     * Constructs a JavaMethodExtractor with the given source ID.
     * @param source the source of the class
     * @param typeInfo the class/interface/record information
     */
    public JavaMethodExtractor {
        Preconditions.requireNonNull(source, "Source cannot be null");
        Preconditions.requireNonNull(typeInfo, "TypeInfo cannot be null");
    }

    @Override
    public List<MethodInfo> extract(TypeDeclaration<?> declaration) {
        Preconditions.requireNonNull(declaration, "Declaration cannot be null");

        List<MethodInfo> methods = new ArrayList<>();
        List<CallableDeclaration<?>> callables = collectCallables(declaration);

        for (CallableDeclaration<?> c : callables) {
            methods.add(extractOne(c));
        }
        return methods;
    }

    private static List<CallableDeclaration<?>> collectCallables(TypeDeclaration<?> typeDecl) {
        List<CallableDeclaration<?>> res = new ArrayList<>();
        res.addAll(typeDecl.findAll(MethodDeclaration.class));
        res.addAll(typeDecl.findAll(ConstructorDeclaration.class));

        return res;
    }

    private MethodInfo extractOne(CallableDeclaration<?> c) {
        final String name = c.getNameAsString();
        final boolean isConstructor = c.isConstructorDeclaration();
        
        final String returnType = isConstructor ? CONSTRUCTOR_NAME : ((MethodDeclaration) c).getType().asString();

        // Parameters
        List<ParameterInfo> params = new ArrayList<>();
        List<String> paramTypes = new ArrayList<>();
        int pIndex = 0;
        for (Parameter p : c.getParameters()) {
            paramTypes.add(/*ch.epfl.printwizard.parser.java.TypeIdUtils.canonical(*/p.getType().toString()/*)*/);
            params.add(new ParameterInfo(pIndex++, p.getNameAsString(), "t:" + p.getType().toString()));
        }

        // Method ID "m:Class.method(Args)"
        String classInternal = typeInfo.getPackageName() + "." + typeInfo.getName();
        String methodId = IdGenerator.methodId(classInternal, name, String.join(",", paramTypes), returnType);

        // Lines
        int startLine = c.getRange().map(r -> r.begin.line).orElse(0);
        int endLine = c.getRange().map(r -> r.end.line).orElse(0);

        // Structures and Local Variables
        JavaStructureMapper mapper = new JavaStructureMapper(params.size());
        BlockStmt stmt = methodBodyOf(c).orElse(null);
        StructureNode root = stmt == null ? null : mapper.mapBlock(stmt);
        List<LocalVar> locals = new ArrayList<>(mapper.getLocals());

        return new MethodInfo(
            methodId, typeInfo.getId(),
            name,
            returnType,
            startLine, endLine,
            params,
            root,
            locals
        );
    }

    private static Optional<BlockStmt> methodBodyOf(CallableDeclaration<?> c) {
        if (c instanceof MethodDeclaration md) {
            return md.getBody();
        }
        if (c instanceof ConstructorDeclaration cd) {
            return Optional.of(cd.getBody());
        }
        return Optional.empty();
    }
}
