package ch.epfl.printwizard.examples;

public class Main {

    public static void main(String[] args) {
        Player p1 = new Player(100, 20);
        Player p2 = new Player(17, 59);
        int currentHealthPointsP1 = p1.getHealthPoints();
        p1.setHealthPoints(add(10, 15));
        System.out.println("Player health points after damage: " + p1.getHealthPoints());

        String testString = "Hello";
        double[] testArray = {1.0, 2.0, 3.0};

        if (testArray.length > 0) {
            testArray[0] = 10.0;
        }
    }

    private static int add(int a, int b) {
        return a + b;
    }
}
