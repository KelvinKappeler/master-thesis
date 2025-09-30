package ch.epfl.printwizard.examples;

public class Main {

    public static void main(String[] args) {
        System.out.println("Hello, World!");
        
        int[] array = new int[5];
        array[2] = 10;
        
        Player player = new Player(100);
        System.out.println("Player health points: " + player.getHealthPoints());
        player.setHealthPoints(80);
        System.out.println("Player health points after damage: " + player.getHealthPoints());
    }
}
