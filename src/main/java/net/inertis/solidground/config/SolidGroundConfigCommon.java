package net.inertis.solidground.config;

// net imports
import net.minecraftforge.common.ForgeConfigSpec;

// java imports
import java.util.List;

// com imports

/**
 * Class for config controls
 */
public class SolidGroundConfigCommon {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> WHITELIST;

    // Fatigue Tuning
    public static final ForgeConfigSpec.DoubleValue FATIGUE_ADDED_PER_BLOCK;
    public static final ForgeConfigSpec.DoubleValue FATIGUE_DECAY_PER_TICK;
    public static final ForgeConfigSpec.DoubleValue MAX_FATIGUE_CAP;
    public static final ForgeConfigSpec.DoubleValue PENALTY_SCALING_FACTOR;

    // Depth Scaling Tuning
    public static final ForgeConfigSpec.IntValue Y_LEVEL_SURFACE;
    public static final ForgeConfigSpec.DoubleValue HARDNESS_AT_SURFACE; // e.g. 0.5 = 50% harder

    public static final ForgeConfigSpec.IntValue Y_LEVEL_DEEPSLATE_START;
    public static final ForgeConfigSpec.DoubleValue HARDNESS_AT_DEEPSLATE_START; // e.g. 1.0 = 100% harder

    public static final ForgeConfigSpec.IntValue Y_LEVEL_BEDROCK;
    public static final ForgeConfigSpec.DoubleValue HARDNESS_AT_BEDROCK; // e.g. 2.0 = 200% harder

    static {
        BUILDER.push("SolidGround Configuration");

        WHITELIST = BUILDER
                .comment("List of block IDs affected by the mining fatigue (e.g. minecraft:stone)")
                .defineList("Whitelist", List.of("minecraft:stone", "minecraft:deepslate", "minecraft:tuff", "minecraft:granite", "minecraft:diorite", "minecraft:andesite"), obj -> obj instanceof String);

        BUILDER.push("Fatigue Mechanics");
        FATIGUE_ADDED_PER_BLOCK = BUILDER
                .comment("How much fatigue is added when breaking one block.")
                .defineInRange("FatigueAddedPerBlock", 10.0, 0.0, 1000.0);

        FATIGUE_DECAY_PER_TICK = BUILDER
                .comment("How much fatigue is recovered per tick (20 ticks = 1 second).")
                .defineInRange("FatigueDecayPerTick", 0.05, 0.0, 100.0);

        MAX_FATIGUE_CAP = BUILDER
                .comment("The maximum amount of fatigue a player can accumulate.")
                .defineInRange("MaxFatigueCap", 100.0, 1.0, 10000.0);

        PENALTY_SCALING_FACTOR = BUILDER
                .comment("Determines how harsh the fatigue slowdown is. Higher number = Less penalty.")
                .defineInRange("PenaltyScalingFactor", 10.0, 0.1, 1000.0);
        BUILDER.pop();

        BUILDER.push("Depth Scaling");
        BUILDER.comment("Defines how much harder blocks become based on Y-level.",
                "Hardness adds to the mining time. 0.5 = 50% harder, 1.0 = 100% harder (half speed), etc.");

        Y_LEVEL_SURFACE = BUILDER.defineInRange("Y_Level_Surface", 66, -2048, 2048);
        HARDNESS_AT_SURFACE = BUILDER.defineInRange("Hardness_At_Surface", 0.5, 0.0, 100.0);

        Y_LEVEL_DEEPSLATE_START = BUILDER.defineInRange("Y_Level_Deepslate_Start", 8, -2048, 2048);
        HARDNESS_AT_DEEPSLATE_START = BUILDER.defineInRange("Hardness_At_Deepslate_Start", 1.0, 0.0, 100.0);

        Y_LEVEL_BEDROCK = BUILDER.defineInRange("Y_Level_Bedrock", -64, -2048, 2048);
        HARDNESS_AT_BEDROCK = BUILDER.defineInRange("Hardness_At_Bedrock", 2.0, 0.0, 100.0);
        BUILDER.pop();

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}