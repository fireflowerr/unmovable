package net.fireflowerr;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(UnmovableConfig.CONFIG_GROUP)
public interface UnmovableConfig extends Config {
    String CONFIG_GROUP = "fireflowerr";

    @ConfigItem(
            keyName = "preserveMenu",
            name = "Preserve menu",
            description = "When enabled, swaps 'walk here' with 'cancel' instead of removing it")
    default boolean preserveMenu() {
        return false;
    }
}
