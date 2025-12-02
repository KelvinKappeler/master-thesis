package ch.epfl.printwizard.examples.rpg;

public final class BattleHistory {
    
    private final String attackerName;
    private final String defenderName;
    private final int damageDealt;
    private final int defenderPreviousHealthPoints;
    private final int defenderNewHealthPoints;
    private final boolean isDefenderDead;
    
    public BattleHistory(String attackerName, String defenderName, int damageDealt, int defenderPreviousHealthPoints, int defenderNewHealthPoints, boolean isDefenderDead) {
        this.attackerName = attackerName;
        this.defenderName = defenderName;
        this.damageDealt = damageDealt;
        this.defenderPreviousHealthPoints = defenderPreviousHealthPoints;
        this.defenderNewHealthPoints = defenderNewHealthPoints;
        this.isDefenderDead = isDefenderDead;
    }
    
    public String getAttackerName() {
        return attackerName;
    }
    
    public String getDefenderName() {
        return defenderName;
    }
    
    public int getDamageDealt() {
        return damageDealt;
    }
    
    public int getDefenderPreviousHealthPoints() {
        return defenderPreviousHealthPoints;
    }
    
    public int getDefenderNewHealthPoints() {
        return defenderNewHealthPoints;
    }
    
    public boolean isDefenderDead() {
        return isDefenderDead;
    }
}
