package ch.epfl.printwizard.agent;

import java.lang.instrument.Instrumentation;
import java.util.List;

/**
 * Represents a tracing agent for monitoring and logging application behavior.
 */
public final class TraceAgent {

    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("TraceAgent initialized with args: " + agentArgs);
        
        TraceConfig config = new TraceConfig(
                List.of("ch/epfl/printwizard/examples/"),
                List.of("java/", "javax/", "sun/", "com/sun/", "ch/epfl/printwizard/agent/"),
                true,
                true,
                true,
                true,
                ""
        );
        
        inst.addTransformer(new TraceTransformer(config), true);
    }
}
