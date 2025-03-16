package net.fireflowerr;

import net.runelite.api.Client;
import net.runelite.api.KeyCode;
import net.runelite.api.Menu;
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
    private static final String TARGET_OPTION = "walk here";

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


    @DisplayName("Assert no-op when 'walk here' not present")
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void onPostMenuSort(boolean preserveMenu)
    {
        updateStrategy(preserveMenu);
        int entryCount = 5;
        for (int i = 0; i < entryCount; i++)
        {
            entries.add(createMenuEntry("foo"));
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
            entries.add(createMenuEntry("foo"));
        }
        var walkHere = createMenuEntry(TARGET_OPTION);
        entries.add(walkHere);
        when(client.isKeyPressed(eq(KeyCode.KC_SHIFT))).thenReturn(true);

        plugin.onPostMenuSort(null);

        assertEquals(entryCount, entries.size());
        assertEquals(walkHere, entries.getLast());
    }

    @Nested
    class FilteredStrategyTest {
        @DisplayName("Assert 'walk here' filtered from menu entries if it is only entry")
        @Test
        public void onPostMenuSort()
        {
            entries.add(createMenuEntry(TARGET_OPTION));
            plugin.onPostMenuSort(null);
            assertTrue(entries.isEmpty());
        }

        @DisplayName("Assert 'walk here' removed if it is tail of menu entries")
        @Test
        public void onPostMenuSort2()
        {
            int entryCount = 5;
            for (int i = 0; i < entryCount; i++)
            {
                entries.add(createMenuEntry("foo"));
            }

            var oneBehind = entries.get(entryCount - 2);
            entries.removeLast();
            entries.addLast(createMenuEntry(TARGET_OPTION));
            plugin.onPostMenuSort(null);

            // assert something was removed
            assertEquals(entryCount - 1, entries.size());

            // assert filtering shifted down
            assertEquals(oneBehind, entries.getLast());
        }

        @DisplayName("Assert 'walk here' is not removed if not at tail")
        @Test
        public void onPostMenuSort3()
        {
            int entryCount = 5;
            int walkHereIndex = 3;
            for (int i = 0; i < walkHereIndex; i++)
            {
                entries.add(createMenuEntry("foo"));
            }

            var walkHere = createMenuEntry(TARGET_OPTION);
            entries.add(walkHere);

            for (int i = walkHereIndex + 1; i < entryCount; i++)
            {
                entries.add(createMenuEntry("foo"));
            }

            plugin.onPostMenuSort(null);

            // assert nothing was removed
            assertEquals(entryCount, entries.size());

            // assert walk here is still at index 'walkHereIndex'
            assertEquals(walkHere, entries.get(walkHereIndex));
        }
    }

    @Nested
    class DeprioritizingStrategyTest
    {
        @BeforeEach
        public void setStrategy()
        {
            updateStrategy(true);
        }

        @DisplayName("Assert no-op if 'walk here' is the only entry")
        @Test
        public void onPostMenuSort()
        {
            entries.add(createMenuEntry(TARGET_OPTION));
            plugin.onPostMenuSort(null);
            assertEquals(1, entries.size());
        }

        @DisplayName("Assert 'walk here' swapped if there are two entries")
        @Test
        public void onPostMenuSort2()
        {
            var head = createMenuEntry("cancel");
            var tail = createMenuEntry(TARGET_OPTION);
            entries.add(head);
            entries.add(tail);
            plugin.onPostMenuSort(null);

            // assert swap
            assertEquals(head, entries.getLast());
            assertEquals(tail, entries.getFirst());
        }

        @DisplayName("Assert 'walk here' is moved to index one if > 2 entries")
        @Test
        public void onPostMenuSort4()
        {
            var expectedHead = createMenuEntry("cancel");
            var expectedTail = createMenuEntry("foo");
            var expectedBody = createMenuEntry(TARGET_OPTION);
            entries.add(expectedHead);
            entries.add(expectedTail);
            entries.add(expectedBody);

            plugin.onPostMenuSort(null);

            Iterator<MenuEntry> iterator = entries.iterator();
            assertEquals(expectedHead, iterator.next());
            assertEquals(expectedBody, iterator.next());
            assertEquals(expectedTail, iterator.next());
        }

        @DisplayName("Assert 'walk here' is not moved if not at tail")
        @Test
        public void onPostMenuSort3()
        {
            var expectedHead = createMenuEntry("cancel");
            var expectedTail = createMenuEntry("foo");
            var expectedBody = createMenuEntry(TARGET_OPTION);
            entries.add(expectedHead);
            entries.add(expectedBody );
            entries.add(expectedTail);

            plugin.onPostMenuSort(null);

            Iterator<MenuEntry> iterator = entries.iterator();
            assertEquals(expectedHead, iterator.next());
            assertEquals(expectedBody, iterator.next());
            assertEquals(expectedTail, iterator.next());
        }
    }

    private static MenuEntry createMenuEntry(String option)
    {
        MenuEntry entry = mock(MenuEntry.class);
        lenient().when(entry.getOption()).thenReturn(option);
        return entry;
    }
}
