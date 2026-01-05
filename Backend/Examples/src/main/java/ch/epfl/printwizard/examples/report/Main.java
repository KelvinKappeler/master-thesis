package ch.epfl.printwizard.examples.report;

import java.util.*;

public final class Main {

    public static void main(String[] args) {
        long seed = 123456789L;
        Simulation sim = new Simulation(seed);

        Player player = new Player("PlayerName", 50);
        List<Effect> effects = sim.generateEffects(300);
        
        effects.set(120, Effect.hit(12));
        effects.set(121, Effect.shield(8));
        effects.set(122, Effect.vulnerability(2));
        effects.set(123, Effect.hit(25));
        
        for (int turn = 0; turn < effects.size(); turn++) {
            Effect e = effects.get(turn);

            sim.applyEffect(player, e, turn);
        }

        System.out.println("\nFinal: " + player.debugString());
    }

    private static class Simulation {
        private final Random rng;

        Simulation(long seed) {
            this.rng = new Random(seed);
        }

        List<Effect> generateEffects(int n) {
            List<Effect> out = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                out.add(randomEffect());
            }
            return out;
        }

        private Effect randomEffect() {
            int roll = rng.nextInt(100);
            if (roll < 55) return Effect.hit(1 + rng.nextInt(10));
            if (roll < 70) return Effect.heal(1 + rng.nextInt(6));
            if (roll < 85) return Effect.shield(1 + rng.nextInt(8));
            if (roll < 95) return Effect.vulnerability(1 + rng.nextInt(2));
            return Effect.regen(1 + rng.nextInt(3));
        }

        void applyEffect(Player player, Effect effect, int turn) {
            tickStatus(player, turn);

            if (effect.type == Effect.Type.HIT) handleHit(player, effect.amount, turn);
            else if (effect.type == Effect.Type.HEAL) handleHeal(player, effect.amount);
            else if (effect.type == Effect.Type.SHIELD) handleShield(player, effect.amount);
            else if (effect.type == Effect.Type.VULNERABILITY) handleVulnerability(player, effect.amount);
            else if (effect.type == Effect.Type.REGEN) handleRegen(player, effect.amount);
            
            clampHealth(player);
        }

        private void tickStatus(Player player, int turn) {
            player.status.decayShield();
            player.status.decayVulnerability();
            
            if (player.status.regenPerTurn > 0) {
                player.heal(player.status.regenPerTurn);
                if (turn % 17 == 0) {
                    player.status.regenPerTurn = 0;
                }
            }
        }

        private void handleHit(Player player, int rawDamage, int turn) {
            int multiplier = 1 + player.status.vulnerabilityStacks;
            int scaledDamage = rawDamage * multiplier;
            
            boolean crit = (rng.nextInt(100) < 10);
            int finalDamage = crit ? (scaledDamage + 5) : scaledDamage;
            
            player.applyDamage(finalDamage, DamageSource.ENEMY);
            
            if (turn % 41 == 0 && rng.nextInt(100) < 20) {
                player.applyDamage(7, DamageSource.TRAP);
            }
        }

        private void handleHeal(Player player, int amount) {
            player.heal(amount);
        }

        private void handleShield(Player player, int amount) {
            player.status.addShield(amount);
        }

        private void handleVulnerability(Player player, int stacks) {
            player.status.addVulnerability(stacks);
        }

        private void handleRegen(Player player, int perTurn) {
            player.status.regenPerTurn = Math.max(player.status.regenPerTurn, perTurn);
        }

        private void clampHealth(Player player) {
            if (player.health < 0) {
                player.health = 0;
            }
        }
    }

    enum DamageSource { ENEMY, TRAP }

    private static class Player {
        private final String name;
        private int health;
        private final Status status;

        Player(String name, int health) {
            this.name = name;
            this.health = health;
            this.status = new Status();
        }

        int getHealth() {
            return health;
        }

        int heal(int amount) {
            int before = health;
            health = health + Math.max(0, amount);
            return health - before;
        }
        
        void applyDamage(int damage, DamageSource source) {
            int incoming = Math.max(0, damage);
            
            int absorbed = status.absorbWithShield(incoming);
            int remaining = incoming - absorbed;

            // --- Intended: health -= remaining; ---
            // BUG: in this branch we subtract (remaining + absorbed) == incoming
            // even though absorbed was already handled by shield reduction.
            if (status.wasShieldUsedThisCall && source == DamageSource.ENEMY) {
                health -= (remaining + absorbed);
            } else {
                health -= remaining;
            }
        }

        String debugString() {
            return "Player{name=" + name
                + ", health=" + health
                + ", shield=" + status.shield
                + ", vuln=" + status.vulnerabilityStacks
                + ", regen=" + status.regenPerTurn
                + "}";
        }
    }

    static class Status {
        int shield = 0;
        int vulnerabilityStacks = 0;
        int regenPerTurn = 0;
        
        boolean wasShieldUsedThisCall = false;

        void addShield(int amount) {
            shield += Math.max(0, amount);
        }

        void addVulnerability(int stacks) {
            vulnerabilityStacks += Math.max(0, stacks);
        }

        void decayShield() {
            if (shield > 0) shield -= 1;
        }

        void decayVulnerability() {
            if (vulnerabilityStacks > 0 && (vulnerabilityStacks % 2 == 0)) {
                vulnerabilityStacks -= 1;
            }
        }

        int absorbWithShield(int incomingDamage) {
            wasShieldUsedThisCall = false;
            if (incomingDamage <= 0) return 0;
            if (shield <= 0) return 0;

            wasShieldUsedThisCall = true;
            int absorbed = Math.min(shield, incomingDamage);
            shield -= absorbed; // shield is consumed here
            return absorbed;
        }
    }

    private static class Effect {
        enum Type { HIT, HEAL, SHIELD, VULNERABILITY, REGEN }

        final Type type;
        final int amount;

        private Effect(Type type, int amount) {
            this.type = type;
            this.amount = amount;
        }

        static Effect hit(int dmg) { return new Effect(Type.HIT, dmg); }
        static Effect heal(int hp) { return new Effect(Type.HEAL, hp); }
        static Effect shield(int s) { return new Effect(Type.SHIELD, s); }
        static Effect vulnerability(int stacks) { return new Effect(Type.VULNERABILITY, stacks); }
        static Effect regen(int perTurn) { return new Effect(Type.REGEN, perTurn); }

        @Override
        public String toString() {
            return "Effect{" + type + ", amount=" + amount + "}";
        }
    }
}
