package ch.epfl.printwizard.examples;

public class Main {
    
    public static void main(String[] args) {
        Player p = new Player(10, 30);
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
