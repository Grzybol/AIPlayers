package pl.nop.aiplayers.ai.controller;

import com.google.gson.annotations.SerializedName;

public class PlannerSettings {
    @SerializedName("max_actions")
    private final int maxActions;
    @SerializedName("min_delay_ms")
    private final int minDelayMs;
    @SerializedName("max_delay_ms")
    private final int maxDelayMs;
    @SerializedName("global_silence_chance")
    private final double globalSilenceChance;
    @SerializedName("reply_chance")
    private final double replyChance;

    public PlannerSettings(int maxActions, int minDelayMs, int maxDelayMs, double globalSilenceChance, double replyChance) {
        this.maxActions = maxActions;
        this.minDelayMs = minDelayMs;
        this.maxDelayMs = maxDelayMs;
        this.globalSilenceChance = globalSilenceChance;
        this.replyChance = replyChance;
    }
}
