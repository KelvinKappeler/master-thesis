package ch.epfl.printwizard.examples;

public class Main {

    public static void main(String[] args) {
        Player p1 = new Player(100, 20);
        Player[] array = {p1};
        Player[] arrayWithoutInit = new Player[1];

        //int[] array = new int[2];
        //array[0] = 1;
        //array[1] += 2;
        /*Player p1 = new Player(100, 20);
        Player p2 = new Player(17, 59);
        int currentHealthPointsP1 = p1.getHealthPoints();
        p1.setHealthPoints(add(10, 15));
        System.out.println("Player health points after damage: " + p1.getHealthPoints());
        int z = Math.abs(32);*/
    }

    private static int add(int a, int b) {
        return a + b;
    }
}
