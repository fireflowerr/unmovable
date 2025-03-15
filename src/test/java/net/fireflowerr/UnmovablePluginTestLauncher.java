package net.fireflowerr;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;
import org.junit.jupiter.api.Disabled;

@Disabled
public class UnmovablePluginTestLauncher
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(UnmovablePlugin.class);
		RuneLite.main(args);
	}
}