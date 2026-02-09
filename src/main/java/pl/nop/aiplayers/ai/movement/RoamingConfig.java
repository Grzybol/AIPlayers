package pl.nop.aiplayers.ai.movement;

import org.bukkit.configuration.file.FileConfiguration;

public class RoamingConfig {
    private final long minDelayMs;
    private final long maxDelayMs;
    private final double idleChance;
    private final long idleMinMs;
    private final long idleMaxMs;
    private final double stepMin;
    private final double stepMax;
    private final double reachThreshold;

    public RoamingConfig(FileConfiguration config) {
        long minDelay = config.getLong("ai.roam.min-delay-ms", 700L);
        long maxDelay = config.getLong("ai.roam.max-delay-ms", 3500L);
        long idleMin = config.getLong("ai.roam.idle-min-ms", 400L);
        long idleMax = config.getLong("ai.roam.idle-max-ms", 2500L);
        double stepMinValue = config.getDouble("ai.roam.step-min", 0.35);
        double stepMaxValue = config.getDouble("ai.roam.step-max", 0.80);
        this.minDelayMs = Math.min(minDelay, maxDelay);
        this.maxDelayMs = Math.max(minDelay, maxDelay);
        this.idleChance = clamp(config.getDouble("ai.roam.idle-chance", 0.30), 0.0, 1.0);
        this.idleMinMs = Math.min(idleMin, idleMax);
        this.idleMaxMs = Math.max(idleMin, idleMax);
        this.stepMin = Math.min(stepMinValue, stepMaxValue);
        this.stepMax = Math.max(stepMinValue, stepMaxValue);
        this.reachThreshold = config.getDouble("ai.roam.reach-threshold", 0.6);
    }

    public long getMinDelayMs() {
        return minDelayMs;
    }

    public long getMaxDelayMs() {
        return maxDelayMs;
    }

    public double getIdleChance() {
        return idleChance;
    }

    public long getIdleMinMs() {
        return idleMinMs;
    }

    public long getIdleMaxMs() {
        return idleMaxMs;
    }

    public double getStepMin() {
        return stepMin;
    }

    public double getStepMax() {
        return stepMax;
    }

    public double getReachThreshold() {
        return reachThreshold;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
