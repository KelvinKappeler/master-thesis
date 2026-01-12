package ch.epfl.printwizard.examples;

public class Main {
    
    public static void main(String[] args) {
        int i = 3;
        int b = 4;

        if (i > addWithLog(1, 1) || b < addWithLog(2, 2)) {
            System.out.println("OK1");
        }

        if (b < addWithLog(2, 7) && b < addWithLog(2, 5)) {
            System.out.println("OK2");
        }
    }

    private static int addWithLog(int a, int b) {
        System.out.println("Adding " + a + " and " + b);
        return a + b;
    }

    private static int add(int a, int b) {
        return a + b;
    }

    private static void print(String str) {
        System.out.println(str);
    }
}
