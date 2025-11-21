package ch.epfl.printwizard.examples;

public final class Player {
    
    private int healthPoints;
    private int manaPoints;
    
    public Player(int healthPoints, int manaPoints) {
        this.healthPoints = healthPoints;
        this.manaPoints = manaPoints;
    }
    
    public int getHealthPoints() {
        return healthPoints;
    }
    
    public void setHealthPoints(int healthPoints) {
        this.healthPoints = healthPoints;
    }

    public int getManaPoints() {
        return manaPoints;
    }

    public void setManaPoints(int manaPoints) {
        this.manaPoints = manaPoints;
    }
    
}
