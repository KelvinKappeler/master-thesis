package ch.epfl.printwizard.examples.rpg;

public final class Weapon {
    
    private final String name;
    private final int damage;
    private final boolean isRanged;
    
    public Weapon(String name, int damage, boolean isRanged) {
        this.name = name;
        this.damage = damage;
        this.isRanged = isRanged;
    }
    
    public String getName() {
        return name;
    }
    
    public int getDamage() {
        return damage;
    }
    
    public boolean isRanged() {
        return isRanged;
    }
}
