package ch.epfl.printwizard.plugin;

import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;

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
        System.out.println("MyPlugin loaded with args: " + Arrays.toString(args));
    }
}
