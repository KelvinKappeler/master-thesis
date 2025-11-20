package ch.epfl.printwizard.examples;

public class Main {

    public static void main(String[] args) {
        int i = 0;
        i = 3;
        int j = 23;
        
        int[] test = {1, 2, 3};
        test[2] = 5;
        test[0] += 1;
        test[0]++;

        //Player player = new Player(100);
        //System.out.println("Player health points: " + player.getHealthPoints());
        //player.setHealthPoints(80);
        //System.out.println("Player health points after damage: " + player.getHealthPoints());
    }

    private static int add(int a, int b) {
        return a + b;
    }
}
