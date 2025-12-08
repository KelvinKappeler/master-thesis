package ch.epfl.printwizard.examples;

public class Main {
    
    public static void main(String[] args) {

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
}
