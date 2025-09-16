package ch.epfl.printwizard.agent;

import java.lang.instrument.Instrumentation;

/**
 * Represents a tracing agent for monitoring and logging application behavior.
 */
public final class TraceAgent {

    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("TraceAgent initialized with args: " + agentArgs);
    }
}
