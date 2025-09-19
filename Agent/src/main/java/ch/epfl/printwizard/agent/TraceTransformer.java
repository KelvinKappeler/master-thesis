package ch.epfl.printwizard.agent;

import ch.epfl.printwizard.agent.utils.Preconditions;
import org.objectweb.asm.*;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

/**
 * Represents a class file transformer for modifying bytecode at runtime.
 */
public final class TraceTransformer implements ClassFileTransformer {
    
    private final TraceConfig config;
    
    public TraceTransformer(TraceConfig config) {
        Preconditions.requireNonNull(config, "config cannot be null");
        
        this.config = config;
    }
    
    @Override
    public byte[] transform(
            Module module,
            ClassLoader loader,
            String className,
            Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain,
            byte[] classfileBuffer) {
        
        if (className == null || !config.shouldInstrument(className)) {
            return null;
        }

        ClassReader cr = new ClassReader(classfileBuffer);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

        ClassVisitor cv = new ClassVisitor(Opcodes.ASM9, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] ex) {
                MethodVisitor mv = super.visitMethod(access, name, desc, sig, ex);
                return new TraceMethodVisitor(api, mv, access, name, desc, config, className);
            }
        };

        cr.accept(cv, ClassReader.EXPAND_FRAMES);
        return cw.toByteArray();
    }
    
}
