package ch.epfl.printwizard.plugin.tracing;

public class TraceOut {
    private TraceOut() {}

    public static <T> T tap(String label, T value) {
        System.out.println("[tap] " + label + " = " + value);
        return value;
    }

    public static int tap(String label, int value) {
        System.out.println("[tap] " + label + " = " + value);
        return value;
    }

    public static long tap(String label, long value) {
        System.out.println("[tap] " + label + " = " + value);
        return value;
    }

    public static double tap(String label, double value) {
        System.out.println("[tap] " + label + " = " + value);
        return value;
    }

    public static float tap(String label, float value) {
        System.out.println("[tap] " + label + " = " + value);
        return value;
    }

    public static boolean tap(String label, boolean value) {
        System.out.println("[tap] " + label + " = " + value);
        return value;
    }

}
