package ch.epfl.printwizard.examples;

public class Main {

    public static void main(String[] args) {
        System.out.println("Hello, World!");

        int i = 0;
        i = i + 1;
        //i = 5 + 2;
        
        float v = 4.3f;
        v = v * 2.1f;

        i = add(i, 1);

        if (i == 1) {
            System.out.println("The condition is true");
        }
        else if (i == 2) {
            System.out.println("The condition is false");
        }
        else {
            System.out.println("Else block");
        }

        //int[] array = new int[5];
        //array[2] = 10;
        
        //Player player = new Player(100);
        //System.out.println("Player health points: " + player.getHealthPoints());
        //player.setHealthPoints(80);
        //System.out.println("Player health points after damage: " + player.getHealthPoints());
    }

    private static int add(int a, int b) {
        return a + b;
    }
}
