package net.fireflowerr;

import net.runelite.api.Client;
import net.runelite.api.KeyCode;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UnmovablePluginTest
{
    private LinkedList<MenuEntry> entries;

    private Client client;

    private UnmovablePlugin plugin;

    private boolean preserveMenu;

    @BeforeEach
    public void setup()
    {
        client = mock(Client.class);
        Menu menu = mock(Menu.class);
        entries = new LinkedList<>();
        lenient().when(client.getMenu()).thenReturn(menu);
        lenient().when(menu.getMenuEntries()).thenAnswer((invocation) -> entries.toArray(MenuEntry[]::new));

        // sync entries array with entries used in PostMenuSort callback
        lenient().doAnswer((invocation) ->
        {
            entries.clear();
            Collections.addAll(entries, invocation.getArgument(0, MenuEntry[].class));
            return null;
        }).when(menu).setMenuEntries(any(MenuEntry[].class));

        UnmovableConfig config = mock(UnmovableConfig.class);
        when(config.preserveMenu()).thenAnswer((invocation) -> preserveMenu);
        ConfigManager configManager = mock(ConfigManager.class);
        when(configManager.getConfig(any())).thenReturn(config);

        plugin = new UnmovablePlugin();
        plugin.setClient(client);
        plugin.providesConfig(configManager);
    }

    private void updateStrategy(boolean preserveMenu) {
        this.preserveMenu = preserveMenu;
        ConfigChanged changed = mock(ConfigChanged.class);
        when(changed.getGroup()).thenReturn(UnmovableConfig.CONFIG_GROUP);
        plugin.onConfigChanged(changed);
    }


    @DisplayName("Assert no-op when WALK not present")
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void onPostMenuSort(boolean preserveMenu)
    {
        updateStrategy(preserveMenu);
        int entryCount = 5;
        for (int i = 0; i < entryCount; i++)
        {
            entries.add(createMenuEntry(MenuAction.PLAYER_FIRST_OPTION));
        }

        plugin.onPostMenuSort(null);

        assertEquals(entryCount, entries.size());
    }

    @DisplayName("Assert no-op when shift pressed")
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void onPostMenuSort2(boolean preserveMenu)
    {
        updateStrategy(preserveMenu);
        int entryCount = 5;
        for (int i = 0; i < entryCount - 1;  i++)
        {
            entries.add(createMenuEntry(MenuAction.PLAYER_FIRST_OPTION));
        }
        var walkHere = createMenuEntry(MenuAction.WALK);
        entries.add(walkHere);
        when(client.isKeyPressed(eq(KeyCode.KC_SHIFT))).thenReturn(true);

        plugin.onPostMenuSort(null);

        assertEquals(entryCount, entries.size());
        assertEquals(walkHere, entries.getLast());
    }

    @Nested
    class FilteredStrategyTest {
        @DisplayName("Assert WALK filtered from menu entries if it is only entry")
        @Test
        public void onPostMenuSort()
        {
            entries.add(createMenuEntry(MenuAction.WALK));
            plugin.onPostMenuSort(null);
            assertTrue(entries.isEmpty());
        }

        @DisplayName("Assert WALK removed if it is tail of menu entries")
        @Test
        public void onPostMenuSort2()
        {
            int entryCount = 5;
            for (int i = 0; i < entryCount; i++)
            {
                entries.add(createMenuEntry(MenuAction.PLAYER_FIRST_OPTION));
            }

            var oneBehind = entries.get(entryCount - 2);
            entries.removeLast();
            entries.addLast(createMenuEntry(MenuAction.WALK));
            plugin.onPostMenuSort(null);

            // assert something was removed
            assertEquals(entryCount - 1, entries.size());

            // assert filtering shifted down
            assertEquals(oneBehind, entries.getLast());
        }

        @DisplayName("Assert WALK is not removed if not at tail")
        @Test
        public void onPostMenuSort3()
        {
            int entryCount = 5;
            int walkHereIndex = 3;
            for (int i = 0; i < walkHereIndex; i++)
            {
                entries.add(createMenuEntry(MenuAction.PLAYER_FIRST_OPTION));
            }

            var walkHere = createMenuEntry(MenuAction.WALK);
            entries.add(walkHere);

            for (int i = walkHereIndex + 1; i < entryCount; i++)
            {
                entries.add(createMenuEntry(MenuAction.PLAYER_FIRST_OPTION));
            }

            plugin.onPostMenuSort(null);

            // assert nothing was removed
            assertEquals(entryCount, entries.size());

            // assert walk here is still at index 'walkHereIndex'
            assertEquals(walkHere, entries.get(walkHereIndex));
        }
    }

    @Nested
    class SwappingStrategyTest
    {
        @BeforeEach
        public void setStrategy()
        {
            updateStrategy(true);
        }

        @DisplayName("Assert no-op if WALK is the only entry")
        @Test
        public void onPostMenuSort()
        {
            entries.add(createMenuEntry(MenuAction.WALK));
            plugin.onPostMenuSort(null);
            assertEquals(1, entries.size());
        }

        @DisplayName("Assert WALK swapped with CANCEL")
        @ParameterizedTest
        @ValueSource(ints = {0, 1, 2, 3})
        public void onPostMenuSort2(int cancelIndex)
        {
            entries.add(createMenuEntry(MenuAction.PLAYER_FIRST_OPTION));
            entries.add(createMenuEntry(MenuAction.PLAYER_SECOND_OPTION));
            entries.add(createMenuEntry(MenuAction.PLAYER_THIRD_OPTION));
            entries.add(createMenuEntry(MenuAction.PLAYER_FOURTH_OPTION));

            var walkHere = createMenuEntry(MenuAction.WALK);
            var cancel = createMenuEntry(MenuAction.CANCEL);
            entries.add(walkHere);
            entries.set(cancelIndex, cancel);

            plugin.onPostMenuSort(null);

            // assert swap
            assertEquals(walkHere, entries.get(cancelIndex));
            assertEquals(cancel, entries.getLast());
        }

        @DisplayName("Assert no-op if cancel not present")
        @Test
        public void onPostMenuSort4()
        {
            var option1 = createMenuEntry(MenuAction.PLAYER_FIRST_OPTION);
            var option2 = createMenuEntry(MenuAction.PLAYER_SECOND_OPTION);
            var walkHere = createMenuEntry(MenuAction.WALK);
            entries.add(option1);
            entries.add(option2);
            entries.add(walkHere);

            plugin.onPostMenuSort(null);

            Iterator<MenuEntry> iterator = entries.iterator();
            assertEquals(option1, iterator.next());
            assertEquals(option2, iterator.next());
            assertEquals(walkHere, iterator.next());
        }

        @DisplayName("Assert WALK is not moved if not at tail")
        @Test
        public void onPostMenuSort3()
        {
            var expectedHead = createMenuEntry(MenuAction.CANCEL);
            var expectedTail = createMenuEntry(MenuAction.PLAYER_FIRST_OPTION);
            var expectedBody = createMenuEntry(MenuAction.WALK);
            entries.add(expectedHead);
            entries.add(expectedBody);
            entries.add(expectedTail);

            plugin.onPostMenuSort(null);

            Iterator<MenuEntry> iterator = entries.iterator();
            assertEquals(expectedHead, iterator.next());
            assertEquals(expectedBody, iterator.next());
            assertEquals(expectedTail, iterator.next());
        }
    }

    private static MenuEntry createMenuEntry(MenuAction type)
    {
        MenuEntry entry = mock(MenuEntry.class);
        lenient().when(entry.getType()).thenReturn(type);
        return entry;
    }
}
