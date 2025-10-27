package ch.epfl.printwizard.examples;

public class Main {

    public static void main(String[] args) {
        int i = 1;

        if (i == 0) {
            System.out.println("The condition is true");
            i = 2;
        }
        else {
            System.out.println("The condition is false");
            i = -2;
        }

        if (i < 0) {
            System.out.println("The condition is true and i < 0");
        }

        if (i > 0) {
            System.out.println("The condition is true and i > 0");
        }
        /*else if (i > 0) {
            System.out.println("The condition is true and i > 0");
        }
        else {
            System.out.println("The condition is false");
        }*/

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
