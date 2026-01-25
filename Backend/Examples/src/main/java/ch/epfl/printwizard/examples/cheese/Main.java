package ch.epfl.printwizard.examples.cheese;

import java.util.*;

public final class Main {

    public static void main(String[] args) {
        long seed = 1234567890L;
        Simulation sim = new Simulation(seed);

        Batch batch = new Batch("Batch-01");
        List<Effect> effects = sim.generateEffects(50);

        for (int turn = 0; turn < effects.size(); turn++) {
            sim.applyEffect(batch, effects.get(turn), turn);
        }

        CheeseType cheese = CheeseClassifier.classify(batch);
        System.out.println("Final: " + batch.debugString());
        System.out.println("Resulting cheese: " + cheese);
        System.out.println("Why: " + CheeseClassifier.explain(batch, cheese));
    }

    enum MoldSource { AMBIENT, HANDLING }

    enum CheeseType { GRUYERE, RACLETTE, APPENZELLER, VACHERIN, EMMENTAL, FAILED_BATCH, UNKNOWN }

    private static final class Simulation {
        private final Random rng;

        Simulation(long seed) {
            this.rng = new Random(seed);
        }

        List<Effect> generateEffects(int n) {
            List<Effect> out = new ArrayList<>(n);
            for (int i = 0; i < n; i++) out.add(randomEffect());
            return out;
        }

        private Effect randomEffect() {
            int roll = rng.nextInt(100);

            if (roll < 32) return Effect.humidityChange(-2 + rng.nextInt(5));
            if (roll < 54) return Effect.tempChange(-1 + rng.nextInt(3));

            if (roll < 67) return Effect.brineWash(2 + rng.nextInt(7));
            if (roll < 77) return Effect.turnCheese(1 + rng.nextInt(3));

            if (roll < 88) return Effect.contamination(1 + rng.nextInt(2));

            if (roll < 95) return Effect.openDoor(50 + rng.nextInt(151), 1 + rng.nextInt(3));

            MoldSource src;
            if (rng.nextInt(100) < 75) src = MoldSource.AMBIENT;
            else src = MoldSource.HANDLING;

            return Effect.moldGrowth(3 + rng.nextInt(10), src);
        }

        void applyEffect(Batch batch, Effect effect, int turn) {
            tickStatus(batch, turn);

            if (effect.type == Effect.Type.HUMIDITY_CHANGE) handleHumidityChange(batch, effect.a);
            else if (effect.type == Effect.Type.TEMP_CHANGE) handleTempChange(batch, effect.a);
            else if (effect.type == Effect.Type.BRINE_WASH) handleBrineWash(batch, effect.a);
            else if (effect.type == Effect.Type.TURN_CHEESE) handleTurnCheese(batch, effect.a);
            else if (effect.type == Effect.Type.CONTAMINATION) handleContamination(batch, effect.a);
            else if (effect.type == Effect.Type.OPEN_DOOR) handleOpenDoor(batch, effect.a, effect.b);
            else if (effect.type == Effect.Type.MOLD_GROWTH) handleMoldGrowth(batch, effect.a, effect.source, turn);

            clamp(batch);
            batch.env.record(batch.humidity, batch.temperature);
        }

        private void tickStatus(Batch batch, int turn) {
            batch.status.decayBarrier();
            batch.status.decayContamination();

            if (turn % 19 == 0) batch.humidity += 1;

            if (batch.humidity >= 95) batch.moldRisk += 2;

            if (batch.temperature >= 16) batch.moldRisk += 1;

            if (turn % 23 == 0 && batch.env.isStable()) batch.rindIntegrity += 1;
        }

        private void handleHumidityChange(Batch batch, int deltaPoints) {
            batch.humidity += deltaPoints;
        }

        private void handleTempChange(Batch batch, int deltaC) {
            batch.temperature += deltaC;
        }

        private void handleBrineWash(Batch batch, int amount) {
            batch.status.addBrineBarrier(amount);
            batch.salt += Math.max(0, amount / 2);
        }

        private void handleTurnCheese(Batch batch, int effort) {
            batch.moldRisk -= 2 * Math.max(1, effort);
        }

        private void handleContamination(Batch batch, int stacks) {
            batch.status.addContamination(stacks);
        }

        private void handleOpenDoor(Batch batch, int humiditySpikePermille, int tempDropC) {
            // --- BUG HERE ---
            batch.humidity += humiditySpikePermille / 10;

            batch.temperature -= Math.max(0, tempDropC);

            batch.moldRisk += 3;
        }

        private void handleMoldGrowth(Batch batch, int raw, MoldSource source, int turn) {
            int multiplier = 1 + batch.status.contaminationStacks;
            int scaled = raw * multiplier;

            int incoming = scaled;
            if (batch.humidity >= 92) {
                incoming = scaled + 4;
            }

            if (rng.nextInt(100) < 7) incoming += 3;

            int absorbed = batch.status.absorbWithBrineBarrier(incoming);
            int remaining = incoming - absorbed;

            batch.rindIntegrity -= Math.max(0, remaining / 2);

            if (source == MoldSource.AMBIENT) batch.moldRisk += incoming / 6;
            else batch.moldRisk += incoming / 8;

            if (source == MoldSource.HANDLING && turn % 37 == 0 && rng.nextInt(100) < 25) {
                batch.rindIntegrity -= 2;
                batch.moldRisk += 2;
            }
        }

        private void clamp(Batch batch) {
            batch.humidity = Math.max(0, Math.min(100, batch.humidity));
            batch.temperature = Math.max(2, Math.min(20, batch.temperature));

            batch.moldRisk = Math.max(0, Math.min(100, batch.moldRisk));
            batch.rindIntegrity = Math.max(0, Math.min(100, batch.rindIntegrity));
            batch.salt = Math.max(0, Math.min(100, batch.salt));
        }
    }

    private static final class Batch {
        final String id;

        int humidity = 88;
        int temperature = 12;

        int rindIntegrity = 80;
        int moldRisk = 10;
        int salt = 30;

        final Status status = new Status();
        final EnvWindow env = new EnvWindow(50);

        Batch(String id) {
            this.id = id;
            this.env.record(humidity, temperature);
        }

        String debugString() {
            return "Batch{id=" + id
                + ", H=" + humidity + "%, T=" + temperature + "C"
                + ", rind=" + rindIntegrity
                + ", moldRisk=" + moldRisk
                + ", salt=" + salt
                + ", barrier=" + status.brineBarrier
                + ", contam=" + status.contaminationStacks
                + ", avgH=" + env.avgHumidity()
                + ", avgT=" + env.avgTemp()
                + "}";
        }
    }

    private static final class Status {
        int brineBarrier = 0;
        int contaminationStacks = 0;

        void addBrineBarrier(int amount) {
            brineBarrier += Math.max(0, amount);
        }

        void addContamination(int stacks) {
            contaminationStacks = Math.min(6, contaminationStacks + Math.max(0, stacks));
        }

        void decayBarrier() {
            if (brineBarrier > 0) brineBarrier -= 1;
        }

        void decayContamination() {
            if (contaminationStacks > 0) contaminationStacks -= 1;
        }

        int absorbWithBrineBarrier(int incoming) {
            if (incoming <= 0) return 0;
            if (brineBarrier <= 0) return 0;

            int absorbed = Math.min(brineBarrier, incoming);
            brineBarrier -= absorbed;
            return absorbed;
        }
    }

    private static final class EnvWindow {
        private final int cap;
        private final ArrayDeque<Integer> hum = new ArrayDeque<>();
        private final ArrayDeque<Integer> tmp = new ArrayDeque<>();
        private int sumH = 0;
        private int sumT = 0;

        EnvWindow(int cap) {
            this.cap = Math.max(5, cap);
        }

        void record(int humidity, int temp) {
            push(hum, humidity);
            push(tmp, temp);
        }

        private void push(ArrayDeque<Integer> dq, int v) {
            if (dq == hum) sumH += v; else sumT += v;

            dq.addLast(v);
            if (dq.size() > cap) {
                int removed = dq.removeFirst();
                if (dq == hum) sumH -= removed; else sumT -= removed;
            }
        }

        int avgHumidity() {
            if (hum.isEmpty()) return 0;
            return (sumH + hum.size() / 2) / hum.size();
        }

        int avgTemp() {
            if (tmp.isEmpty()) return 0;
            return (sumT + tmp.size() / 2) / tmp.size();
        }

        boolean isStable() {
            int ah = avgHumidity();
            int at = avgTemp();
            return (ah >= 84 && ah <= 92) && (at >= 10 && at <= 14);
        }
    }

    private static final class Effect {
        enum Type {
            HUMIDITY_CHANGE,
            TEMP_CHANGE,
            BRINE_WASH,
            TURN_CHEESE,
            CONTAMINATION,
            OPEN_DOOR,
            MOLD_GROWTH
        }

        final Type type;
        final int a;
        final int b;
        final MoldSource source;

        private Effect(Type type, int a, int b, MoldSource source) {
            this.type = type;
            this.a = a;
            this.b = b;
            this.source = source;
        }

        static Effect humidityChange(int deltaPoints) { return new Effect(Type.HUMIDITY_CHANGE, deltaPoints, 0, null); }
        static Effect tempChange(int deltaC) { return new Effect(Type.TEMP_CHANGE, deltaC, 0, null); }
        static Effect brineWash(int barrierAdd) { return new Effect(Type.BRINE_WASH, barrierAdd, 0, null); }
        static Effect turnCheese(int effort) { return new Effect(Type.TURN_CHEESE, effort, 0, null); }
        static Effect contamination(int stacks) { return new Effect(Type.CONTAMINATION, stacks, 0, null); }

        static Effect openDoor(int humiditySpikePermille, int tempDropC) {
            return new Effect(Type.OPEN_DOOR, humiditySpikePermille, tempDropC, null);
        }

        static Effect moldGrowth(int raw, MoldSource src) {
            return new Effect(Type.MOLD_GROWTH, raw, 0, src);
        }

        @Override
        public String toString() {
            if (type == Type.HUMIDITY_CHANGE) return "Effect{HUMIDITY_CHANGE " + a + " points}";
            if (type == Type.TEMP_CHANGE) return "Effect{TEMP_CHANGE " + a + " C}";
            if (type == Type.BRINE_WASH) return "Effect{BRINE_WASH +" + a + " barrier}";
            if (type == Type.TURN_CHEESE) return "Effect{TURN_CHEESE effort=" + a + "}";
            if (type == Type.CONTAMINATION) return "Effect{CONTAMINATION +" + a + "}";
            if (type == Type.OPEN_DOOR) return "Effect{OPEN_DOOR humiditySpikePermille=" + a + "‰, tempDrop=" + b + "C}";
            if (type == Type.MOLD_GROWTH) return "Effect{MOLD_GROWTH raw=" + a + ", source=" + source + "}";

            return "Effect{UNKNOWN}";
        }
    }

    private static final class CheeseClassifier {

        static CheeseType classify(Batch b) {
            int avgH = b.env.avgHumidity();
            int avgT = b.env.avgTemp();

            if (b.rindIntegrity <= 5 || b.moldRisk >= 95) return CheeseType.FAILED_BATCH;

            if (in(avgT, 6, 10) && in(avgH, 92, 98) && in(b.rindIntegrity, 40, 75)) {
                return CheeseType.VACHERIN;
            }

            if (in(avgT, 8, 12) && in(avgH, 88, 96) && b.salt >= 40 && b.rindIntegrity >= 55 && b.moldRisk <= 45) {
                return CheeseType.RACLETTE;
            }

            if (in(avgT, 12, 16) && in(avgH, 80, 90) && b.salt >= 50 && b.moldRisk <= 40 && b.rindIntegrity >= 60) {
                return CheeseType.APPENZELLER;
            }

            if (in(avgT, 10, 14) && in(avgH, 85, 92) && b.moldRisk <= 30 && b.rindIntegrity >= 70) {
                return CheeseType.GRUYERE;
            }

            if (in(avgT, 10, 14) && in(avgH, 80, 88) && b.moldRisk <= 28 && b.rindIntegrity >= 68) {
                return CheeseType.EMMENTAL;
            }

            return CheeseType.UNKNOWN;
        }

        static String explain(Batch b, CheeseType t) {
            int avgH = b.env.avgHumidity();
            int avgT = b.env.avgTemp();

            if (t == CheeseType.VACHERIN) return "Very humid (avgH=" + avgH + "%), cool (avgT=" + avgT + "C), softer rind (rind=" + b.rindIntegrity + ").";
            if (t == CheeseType.RACLETTE) return "Humid/cool band (avgH=" + avgH + "%, avgT=" + avgT + "C) with higher salt (salt=" + b.salt + ").";
            if (t == CheeseType.APPENZELLER) return "Warmer band (avgT=" + avgT + "C) with washed-rind salt level (salt=" + b.salt + ") and controlled mold (moldRisk=" + b.moldRisk + ").";
            if (t == CheeseType.GRUYERE) return "Stable cellar profile (avgH=" + avgH + "%, avgT=" + avgT + "C), strong rind (rind=" + b.rindIntegrity + "), low mold (moldRisk=" + b.moldRisk + ").";
            if (t == CheeseType.EMMENTAL) return "Slightly drier stability (avgH=" + avgH + "%, avgT=" + avgT + "C) with good rind (rind=" + b.rindIntegrity + ") and low mold (moldRisk=" + b.moldRisk + ").";
            if (t == CheeseType.UNKNOWN) return "Metrics out of target ranges: avgH=" + avgH + "% avgT=" + avgT + "C rind=" + b.rindIntegrity + " moldRisk=" + b.moldRisk + " salt=" + b.salt + ".";
            if (t == CheeseType.FAILED_BATCH) return "Batch spoiled: rind=" + b.rindIntegrity + ", moldRisk=" + b.moldRisk + ", avgH=" + avgH + "%, avgT=" + avgT + "C.";

            return "No explanation available.";
        }

        private static boolean in(int v, int lo, int hi) {
            return v >= lo && v <= hi;
        }
    }
}
