package net.fireflowerr;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.KeyCode;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.PostMenuSort;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

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
	private Client client;
	private UnmovableConfig config;
	private Strategy strategy;

	private enum Strategy {FILTER, SWAP}

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
		this.strategy = config.preserveMenu() ? Strategy.SWAP : Strategy.FILTER;
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

		return Strategy.FILTER == strategy ? removeWalk(menuEntries) : swapWithCancel(menuEntries);
	}

	/**
	 * Swaps 'walk here' option with 'cancel', returning a new array.
	 *
	 * @param entries entries
	 * @return new entries or null if cancel not present
	 */
	@Nullable
	private MenuEntry[] swapWithCancel(MenuEntry[] entries)
	{
		// if it is the only item, swapping does nothing
		if (entries.length == 1)
		{
			return null;
		}

		int cancelIndex = -1;
		for (int i = 0; i < entries.length; i++)
		{
			if (MenuAction.CANCEL == entries[i].getType())
			{
				cancelIndex = i;
				break;
			}
		}

		if (cancelIndex == -1)
		{
			return null;
		}

		MenuEntry[] newEntries = new MenuEntry[entries.length];
		System.arraycopy(entries, 0, newEntries, 0, entries.length - 1);
		newEntries[entries.length - 1] = entries[cancelIndex];
		newEntries[cancelIndex] = entries[entries.length - 1];
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

		return MenuAction.WALK == entries[entries.length -1].getType();
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
