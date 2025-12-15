package ch.epfl.printwizard.examples;

public class Main {
    
    public static void main(String[] args) {

        String testStr = "Hello World!";
        print(testStr);
        int i = add(3, 4) / 2;
        
        int j = 0;
        if (i < 3) {
            j += 30;
        } else {
            j += 10;
        }
        
        System.out.println("Result: " + (i + j));
        
        Player player = new Player(10, 100);
    }

    private static int add(int a, int b) {
        return a + b;
    }

    private static void print(String str) {
        System.out.println(str);
    }
}
