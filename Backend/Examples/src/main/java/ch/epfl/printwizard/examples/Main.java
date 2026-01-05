package ch.epfl.printwizard.examples;

public class Main {
    
    public static void main(String[] args) {
        int i = add(3, 4) / 2;
        
        Player player = new Player(50, 70);
        player.setHealthPoints(32);
        
        for (int j = 0; j < 5; j++) {
            i = add(i, j);
        }
    }

    private static int add(int a, int b) {
        return a + b;
    }

    private static void print(String str) {
        System.out.println(str);
    }
}
