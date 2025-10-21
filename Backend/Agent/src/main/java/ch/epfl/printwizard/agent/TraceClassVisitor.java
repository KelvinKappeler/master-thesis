package ch.epfl.printwizard.agent;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

/**
 * Represents a class visitor that instruments methods for tracing.
 */
public class TraceClassVisitor extends ClassVisitor {

    private final TraceConfig config;
    private String ownerInternal;
    private String sourceFile;

    protected TraceClassVisitor(int api, ClassVisitor cv, TraceConfig config) {
        super(api, cv);

        this.config = config;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        ownerInternal = name;

        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public void visitSource(String source, String debug) {
        sourceFile = source;

        TraceSink.registerSource(ownerInternal, sourceFile);
        super.visitSource(source, debug);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        if (mv == null) return null;
        
        return new TraceMethodVisitor(api, mv, access, name, descriptor, config, ownerInternal, sourceFile);
    }
}
