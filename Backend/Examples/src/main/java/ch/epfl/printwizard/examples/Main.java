package ch.epfl.printwizard.examples;

public class Main {
    
    public static void main(String[] args) {


        Player p = new Player(10, 100);
        p.setHealthPoints(120);

        String testStr = "Hello World!";
        print(testStr);
        int i = add(3, 4) / 2;
        
        int j = 0;
        if (i < 3) {
            j += 30;
        } else if (i > 3) {
            j += 20;
        } else {
            j += 10;
        }
        
        if (i == 3) {
            j = 25;
        }
        
        for (int k = 0; k < 3; k++) {
            j += 1;
        }
        
        i = 0;
        while (i < 3) {
            i += 1;
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
