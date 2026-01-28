package ch.epfl.printwizard.examples.example;

public class Main {

    public static void main(String[] args) {
        int i = 3 + 2;

        if (i > 4) {
            System.out.println("i is greater than 4");
        }

        Player player = new Player(100);
        player.setHealthPoints(80);
    }
}
