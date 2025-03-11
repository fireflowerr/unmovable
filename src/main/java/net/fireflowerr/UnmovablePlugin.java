package net.fireflowerr;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.KeyCode;
import net.runelite.api.Menu;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.PostMenuSort;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.Text;

import javax.inject.Inject;


@Slf4j
@PluginDescriptor(
	name = "Unmovable",
	description = "Disables left-click on ground to walk. Hold shift+click to walk. Useful for kiting.",
	tags = {"swapper", "kiting", "walk", "misclick"}
)
public class UnmovablePlugin extends Plugin
{
	@Inject
	private Client client;

	/**
	 * Removes walking from left-click menu if shift is not pressed.
	 *
	 * @param postMenuSort event
	 */
	@Subscribe(priority = -10) // run after MenuEntrySwapper to avoid interfering with it
	public void onPostMenuSort(PostMenuSort postMenuSort)
	{
		if (shiftModifier() || client.isMenuOpen())
		{
			return;
		}

		Menu root = client.getMenu();
		MenuEntry[] menuEntries = root.getMenuEntries();
		MenuEntry[] newMenuEntries = removePrimaryWalk(menuEntries);
		if (newMenuEntries == null)
		{
			return;
		}

		client.setMenuEntries(newMenuEntries);
	}

	/**
	 * If walk is the first non-cancel {@link MenuEntry}, remove it, returning a new array.
	 *
	 * @param entries entries to filter
	 * @return new array if filtered else null
	 */
	private MenuEntry[] removePrimaryWalk(MenuEntry[] entries)
	{
		if (entries.length > 0)
		{
			String entryOption = Text.removeTags(entries[0].getOption()).toLowerCase();
			if (entryOption.toLowerCase().contains("walk here"))
			{
				MenuEntry[] newEntries = new MenuEntry[entries.length - 1];
				System.arraycopy(entries, 1, newEntries, 0, newEntries.length);
				return newEntries;
			}
		}

		return null;
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
