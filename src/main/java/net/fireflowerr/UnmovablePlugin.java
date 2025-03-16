package net.fireflowerr;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.KeyCode;
import net.runelite.api.Menu;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.PostMenuSort;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.Text;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;


@Slf4j
@PluginDescriptor(
	name = "Unmovable",
	description = "Disables left-click on ground to walk. Hold shift+click to walk. Useful for kiting.",
	tags = {"swapper", "kiting", "walk", "misclick"}
)
public class UnmovablePlugin extends Plugin {
	private static final String TARGET_OPTION = "walk here";

	private Client client;
	private UnmovableConfig config;
	private Strategy strategy;

	private enum Strategy {FILTER, DEPRIORITIZE}

	@Inject
	public void setClient(Client client) {
		this.client = client;
	}

	/**
	 * Provides plugin config and sets initial strategy.
	 *
	 * @param configManager config manager
	 * @return config
	 */
	@Provides
	@SuppressWarnings("UnusedReturnValue")
	public UnmovableConfig providesConfig(ConfigManager configManager) {
		config = configManager.getConfig(UnmovableConfig.class);
		updateStrategy();
		return config;
	}

	/**
	 * Update the strategy if the plugin config has changed.
	 *
	 * @param configChanged config changed event
	 */
	@Subscribe
	public void onConfigChanged(ConfigChanged configChanged) {
		if (UnmovableConfig.CONFIG_GROUP.equals(configChanged.getGroup())) {
			updateStrategy();
		}
	}

	/**
	 * Removes walking from left-click menu if shift is not pressed.
	 *
	 * @param postMenuSort event
	 */
	@Subscribe(priority = -10) // run after MenuEntrySwapper to avoid interfering with it
	public void onPostMenuSort(PostMenuSort postMenuSort) {
		if (shiftModifier() || client.isMenuOpen()) {
			return;
		}

		Menu root = client.getMenu();
		MenuEntry[] menuEntries = root.getMenuEntries();
		MenuEntry[] newMenuEntries = updateEntries(menuEntries);
		if (newMenuEntries == null)
		{
			return;
		}

		root.setMenuEntries(newMenuEntries);
	}

	/**
	 * Updates the unmovable strategy.
	 */
	private void updateStrategy()
	{
		this.strategy = config.preserveMenu() ? Strategy.DEPRIORITIZE : Strategy.FILTER;
	}

	/**
	 * If 'walk here' is the highest priority menu entry, apply the current strategy and return a new array.
	 *
	 * @param menuEntries entries
	 * @return new array or else null
	 */
	@Nullable
	private MenuEntry[] updateEntries(MenuEntry[] menuEntries)
	{
		if (!isTailWalkHere(menuEntries))
		{
			return null;
		}

		return Strategy.FILTER == strategy ? removeWalk(menuEntries) : deprioritizeWalk(menuEntries);
	}

	/**
	 * Deprioritizes the 'walk here' option, returning a new array.
	 *
	 * @param entries entries
	 * @return new entries
	 */
	@Nullable
	private MenuEntry[] deprioritizeWalk(MenuEntry[] entries)
	{
		// if it is the only item, deprioritizing does nothing
		if (entries.length == 1)
		{
			return null;
		}

		MenuEntry[] newEntries = new MenuEntry[entries.length];
		// if there are two items, swap them
		if (entries.length == 2)
		{
			newEntries[0] = entries[1];
			newEntries[1] = entries[0];
			return newEntries;
		}

		// if there are three or more items, give second-lowest priority (normally directly below cancel)
		newEntries[0] = entries[0];
		newEntries[1] = entries[entries.length - 1];
		System.arraycopy(entries, 0, newEntries, 2, entries.length - 2);
		newEntries[entries.length - 1] = entries[1];
		return newEntries;
	}

	/**
	 * Removes the last item from the input array, returning a new array.
	 *
	 * @param entries entries to filter
	 * @return filtered array
	 */
	@Nonnull
	private MenuEntry[] removeWalk(MenuEntry[] entries)
	{
		MenuEntry[] newEntries = new MenuEntry[entries.length - 1];
		if (newEntries.length > 0)
		{
			System.arraycopy(entries, 0, newEntries, 0, newEntries.length);
		}
		return newEntries;
	}

	/**
	 * Returns true if 'walk here' is the tail of the provided entries.
	 *
	 * @param entries entries
	 * @return true if 'walk here' is tail
	 */
	private boolean isTailWalkHere(MenuEntry[] entries)
	{
		if (entries.length == 0)
		{
			return false;
		}

		int lastIndex = entries.length - 1;
		String entryOption = Text.removeTags(entries[lastIndex].getOption()).toLowerCase();
		return entryOption.toLowerCase().contains(TARGET_OPTION);
	}

	/**
	 * Returns true if shift is currently pressed.
	 *
	 * @return true if shift pressed
	 */
	private boolean shiftModifier()
	{
		return client.isKeyPressed(KeyCode.KC_SHIFT);
	}
}
