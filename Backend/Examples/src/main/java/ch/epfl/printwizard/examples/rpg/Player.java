package ch.epfl.printwizard.examples.rpg;

import java.util.ArrayList;
import java.util.List;

public final class Player {
    
    private static final int MAX_HEALTH_POINTS = 100;
    private static final int MAX_MANA_POINTS = 50;
    
    private static final List<BattleHistory> battleHistory = new ArrayList<>();
    
    private final String name;
    private final int maxHealthPoints;
    private final int maxManaPoints;
    
    private int healthPoints;
    private int manaPoints;
    private float experiencePoints;
    private Weapon weapon;
    private boolean isDead;
    
    public Player(String name, float experiencePoints) {
        maxHealthPoints = MAX_HEALTH_POINTS;
        maxManaPoints = MAX_MANA_POINTS;
        
        this.name = name;
        this.healthPoints = maxHealthPoints;
        this.manaPoints = maxManaPoints;
        this.experiencePoints = experiencePoints;
        this.weapon = null;
        this.isDead = false;
    }
    
    public String getName() {
        return name;
    }
    
    public int getHealthPoints() {
        return healthPoints;
    }
    
    public int getManaPoints() {
        return manaPoints;
    }
    
    public float getExperiencePoints() {
        return experiencePoints;
    }
    
    public Weapon getWeapon() {
        return weapon;
    }
    
    public void setHealthPoints(int healthPoints) {
        this.healthPoints = Math.clamp(healthPoints, 0, maxHealthPoints);
        this.isDead = healthPoints <= 0;
    }
    
    public void setManaPoints(int manaPoints) {
        this.manaPoints = Math.clamp(manaPoints, 0, maxManaPoints);
    }
    
    public void addExperiencePoints(float experiencePoints) {
        this.experiencePoints += experiencePoints;
    }
    
    public void setWeapon(Weapon weapon) {
        this.weapon = weapon;
    }
    
    public void hurt(int damage) {
        setHealthPoints(getHealthPoints() - damage);
    }
    
    public boolean isDead() {
        return isDead;
    }
    
    public void attack(Player target) {
        if (weapon != null) {
            int targetHealthPoints = target.getHealthPoints();
            target.hurt(weapon.getDamage());

            battleHistory.add(new BattleHistory(
                this.name, target.getName(),
                weapon.getDamage(),
                targetHealthPoints, target.getHealthPoints(),
                target.isDead()));
        }
    }
    
    public static List<BattleHistory> getBattleHistory() {
        return battleHistory;
    }
}
