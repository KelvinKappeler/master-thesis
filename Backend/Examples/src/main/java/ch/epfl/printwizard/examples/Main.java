package ch.epfl.printwizard.examples;

public class Main {

    public static void main(String[] args) {
        int i = 1;
        i = 4;
        int j = 0;

        if (i == 0) {
            System.out.println("The condition is true");
            j = -1;
        }
        else if (i > 0) {
            System.out.println("The condition is true and i > 0");
            j += 1;
            i++;
            j = i + 1;
            j += 4;
            j--;
            j -= 2;
            j -= 1;
            
            double r = 3.4;
            r += 1.2;
            r *= 3.2;
            i = j % 2;
            long l = 2L;
            l += 1L;
            float f = 1.2f;
            f += 1.2f;
            
            int test1 = 10/3;
            float test = 1/3f;
        }

        //int[] array = new int[5];
        //array[0] = 1;
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
