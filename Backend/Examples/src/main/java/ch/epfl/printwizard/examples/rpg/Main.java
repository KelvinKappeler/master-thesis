package ch.epfl.printwizard.examples.rpg;

public class Main {

    public static void main(String[] args) {
        Player p1 = new Player("Name1", 10.3f);
        Player p2 = new Player("Name2", 10.3f);
        
        Weapon w1 = new Weapon("Sword", 15, false);
        p1.setWeapon(w1);
        
        p1.attack(p2);
        p1.attack(p2);
        p1.attack(p2);
        
        System.out.println(Player.getBattleHistory().getFirst());
    }
}
