package ch.epfl.printwizard.agent;

import ch.epfl.printwizard.shared.IdGenerator;
import ch.epfl.printwizard.shared.model.manifest.ManifestFile;
import ch.epfl.printwizard.shared.model.trace.TraceFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.time.LocalDateTime;
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
        
        // TODO: Change place where the output is written
        ManifestFile manifestFile = new ManifestFile(
                "0.1.0",
                LocalDateTime.now(),
                new ch.epfl.printwizard.shared.model.manifest.FileLocations(
                "program.json",
                "trace.json",
                "index.json"
                )
        );

        try {
            ObjectMapper om = new ObjectMapper();
            om.registerModule(new JavaTimeModule());
            om.enable(SerializationFeature.INDENT_OUTPUT);
            om.writeValue(new File("Results/manifest.json"), manifestFile);
        } catch (Exception e) {
            System.err.println("Error writing trace manifest file: " + e.getMessage());
        }
    }
}
