package net.fireflowerr;

import net.fireflowerr.UnmovablePlugin;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class UnmovablePluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(UnmovablePlugin.class);
		RuneLite.main(args);
	}
}