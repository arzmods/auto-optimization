package com.arzmods.potatopvp;

/**
 * The three steps every Potato PvP setting can be on.
 *
 * <p>NONE is the cheapest (the feature is switched off), MINIMUM keeps only the
 * bits that actually matter in a fight, and MEDIUM is "close to vanilla, but
 * still trimmed". There is deliberately no HIGH: this mod only ever makes the
 * game lighter, never heavier than vanilla.
 */
public enum QualityLevel {
    NONE("none"),
    MINIMUM("minimum"),
    MEDIUM("medium");

    private final String id;

    QualityLevel(String id) {
        this.id = id;
    }

    /** Stable name used in the config file, so renaming the enum never breaks saves. */
    public String getId() {
        return this.id;
    }

    /** Translation key for the label shown on the button, see assets/potatopvp/lang. */
    public String getTranslationKey() {
        return "potatopvp.level." + this.id;
    }

    /** Reads a level back from the config file, falling back when the text is junk. */
    public static QualityLevel byId(String id, QualityLevel fallback) {
        if (id != null) {
            for (QualityLevel level : values()) {
                if (level.id.equalsIgnoreCase(id)) {
                    return level;
                }
            }
        }
        return fallback;
    }
}
