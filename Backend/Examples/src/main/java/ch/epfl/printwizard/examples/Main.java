package ch.epfl.printwizard.examples;

public class Main {

    public static void main(String[] args) {
        Player p1 = new Player(100, 20);
        Player p2 = p1;
        int currentHealthPointsP1 = p1.getHealthPoints();
        //System.out.println("Player health points: " + player.getHealthPoints());
        //player.setHealthPoints(80);
        //System.out.println("Player health points after damage: " + player.getHealthPoints());
    }

    private static int add(int a, int b) {
        return a + b;
    }
}
