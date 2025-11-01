package ch.epfl.printwizard.plugin;

import ch.epfl.printwizard.plugin.model.trace.TraceFileJson;
import ch.epfl.printwizard.plugin.tracing.TracingTranslator;
import com.sun.source.util.*;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.tree.JCTree;

import java.io.IOException;
import java.util.Arrays;

/**
 * Represents the plugin for the PrintWizard instrumentation.
 * It is the main entry point for the backend.
 */
public class PrintWizardPlugin implements Plugin {
    
    @Override public String getName() {
        return "PrintWizardPlugin";
    }

    @Override public void init(JavacTask task, String... args) {
        System.out.println("[PrintWizard] loaded with args: " + Arrays.toString(args));
        BasicJavacTask basicTask = (BasicJavacTask) task;

        task.addTaskListener(new TaskListener() {
            @Override
            public void started(com.sun.source.util.TaskEvent e) {
                // ...
            }

            @Override
            public void finished(TaskEvent e) {
                if (e.getKind() != TaskEvent.Kind.ENTER) return;

                if (!(e.getCompilationUnit() instanceof JCTree.JCCompilationUnit cu)) return;

                TracingTranslator tracingTranslator = new TracingTranslator(basicTask.getContext(), cu);
                tracingTranslator.translate();

                try {
                    TraceFileJson.write("test.json", tracingTranslator.getTraceFile());
                } catch (IOException ex) {
                    System.err.println("Error writing trace file: " + ex.getMessage());
                }
            }
        });
    }
}
