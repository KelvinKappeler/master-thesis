package ch.epfl.printwizard.examples;

public class Main {
    
    public static void main(String[] args) {

        int i = 0;

        if (i < 3) {
            i = add(i, 1);
        }
    }

    private static int add(int a, int b) {
        return a + b;
    }
}
