package ch.epfl.printwizard.examples;

public class Main {

    public static void main(String[] args) {
        int i = 0;

        while (i < 10) {
            if (i == 5) {
                i++;
            }
            i++;
        }

        //Player player = new Player(100);
        //System.out.println("Player health points: " + player.getHealthPoints());
        //player.setHealthPoints(80);
        //System.out.println("Player health points after damage: " + player.getHealthPoints());
    }

    private static int add(int a, int b) {
        return a + b;
    }
}
