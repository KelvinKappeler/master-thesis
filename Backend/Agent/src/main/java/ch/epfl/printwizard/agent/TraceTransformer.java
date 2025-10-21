package ch.epfl.printwizard.agent;

import ch.epfl.printwizard.agent.utils.Preconditions;
import org.objectweb.asm.*;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

/**
 * Represents a class file transformer for modifying bytecode at runtime.
 */
public record TraceTransformer(TraceConfig config) implements ClassFileTransformer {

    public TraceTransformer {
        Preconditions.requireNonNull(config, "config cannot be null");
    }

    @Override
    public byte[] transform(
        Module module,
        ClassLoader loader,
        String className,
        Class<?> classBeingRedefined,
        ProtectionDomain protectionDomain,
        byte[] classfileBuffer
    ) {

        if (className == null || !config.shouldInstrument(className)) {
            return null;
        }

        ClassReader cr = new ClassReader(classfileBuffer);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        ClassVisitor cv = new TraceClassVisitor(Opcodes.ASM9, cw, config);

        cr.accept(cv, ClassReader.EXPAND_FRAMES);
        
        return cw.toByteArray();
    }

}
