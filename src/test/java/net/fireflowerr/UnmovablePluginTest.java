package net.fireflowerr;

import net.runelite.api.Client;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.LinkedList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UnmovablePluginTest
{
    private LinkedList<MenuEntry> entries;

    private UnmovablePlugin plugin;

    @BeforeEach
    public void setup()
    {
        Client client = mock(Client.class);
        Menu menu = mock(Menu.class);
        entries = new LinkedList<>();
        when(client.getMenu()).thenReturn(menu);
        when(menu.getMenuEntries()).thenAnswer((invocation) -> entries.toArray(MenuEntry[]::new));

        // sync entries array with entries used in PostMenuSort callback
        lenient().doAnswer((invocation) ->
        {
            entries.clear();
            Collections.addAll(entries, invocation.getArgument(0, MenuEntry[].class));
            return null;
        }).when(menu).setMenuEntries(any(MenuEntry[].class));

        plugin = new UnmovablePlugin();
        plugin.setClient(client);
    }

    @DisplayName("Assert no-op when 'walk here' not present")
    @Test
    public void onPostMenuSort()
    {
        int entryCount = 5;
        for (int i = 0; i < entryCount; i++)
        {
            entries.add(createMenuEntry("foo", null));
        }

        plugin.onPostMenuSort(null);
    }

    @DisplayName("Assert 'walk here' filtered from menu entries if it is only entry")
    @Test
    public void onPostMenuSort2()
    {
        entries.add(createMenuEntry("walk here", MenuAction.WALK));
        plugin.onPostMenuSort(null);
        assertTrue(entries.isEmpty());
    }

    @DisplayName("Assert 'walk here' removed if it is tail of menu entries")
    @ParameterizedTest
    @ValueSource(ints = {2,3,4})
    public void onPostMenuSort3(int entryCount)
    {
        for (int i = 0; i < entryCount; i++)
        {
            entries.add(createMenuEntry("foo", null));
        }

        var oneBehind = entries.get(entryCount - 2);
        entries.removeLast();
        entries.addLast(createMenuEntry("walk here", null));
        plugin.onPostMenuSort(null);

        // assert something was removed
        assertEquals(entryCount - 1, entries.size());

        // assert filtering shifted down
        assertEquals(oneBehind, entries.getLast());
    }

    @DisplayName("Assert 'walk here' is not removed if not at tail")
    @ParameterizedTest
    @ValueSource(ints = {0,1,2,3})
    public void onPostMenuSort4(int walkHereIndex)
    {
        int menuOptionCount = 5;
        for (int i = 0; i < walkHereIndex; i++)
        {
            entries.add(createMenuEntry("foo", null));
        }

        var walkHere = createMenuEntry("walk here", MenuAction.WALK);
        entries.add(walkHere);

        for (int i = walkHereIndex + 1; i < menuOptionCount; i++)
        {
            entries.add(createMenuEntry("foo", null));
        }

        plugin.onPostMenuSort(null);

        // assert nothing was removed
        assertEquals(menuOptionCount, entries.size());

        // assert walk here is still at index 'walkHereIndex'
        assertEquals(walkHere, entries.get(walkHereIndex));
    }

    private static MenuEntry createMenuEntry(String option, MenuAction type)
    {
        MenuEntry entry = mock(MenuEntry.class);
        lenient().when(entry.getOption()).thenReturn(option);
        lenient().when(entry.getType()).thenReturn(type);
        return entry;
    }
}
