package nl.wantedchef.empirewand.spell.enhanced;

import nl.wantedchef.empirewand.EmpireWandPlugin;
import nl.wantedchef.empirewand.api.EmpireWandAPI;
import nl.wantedchef.empirewand.core.config.ReadableConfig;
import nl.wantedchef.empirewand.core.task.TaskManager;
import nl.wantedchef.empirewand.spell.Spell;
import nl.wantedchef.empirewand.spell.SpellContext;
import nl.wantedchef.empirewand.spell.SpellType;
import nl.wantedchef.empirewand.spell.enhanced.defensive.EnergyShield;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EnergyShieldTest {

    private EnergyShield energyShield;

    @Mock
    private EmpireWandAPI api;
    @Mock
    private SpellContext context;
    @Mock
    private Player player;
    @Mock
    private EmpireWandPlugin plugin;
    @Mock
    private TaskManager taskManager;
    @Mock
    private PluginManager pluginManager;
    @Mock
    private World world;
    @Mock
    private Location location;
    @Mock
    private ReadableConfig readableConfig;
    @Mock
    private Logger logger;

    private MockedStatic<Bukkit> bukkitMock;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        bukkitMock = mockStatic(Bukkit.class);
        bukkitMock.when(Bukkit::getPluginManager).thenReturn(pluginManager);
        bukkitMock.when(Bukkit::isPrimaryThread).thenReturn(true);

        when(context.caster()).thenReturn(player);
        when(context.plugin()).thenReturn(plugin);
        when(plugin.getTaskManager()).thenReturn(taskManager);
        when(plugin.getLogger()).thenReturn(logger);
        when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenReturn(location);
        when(location.getWorld()).thenReturn(world);
        energyShield = new EnergyShield.Builder(api).build();
        energyShield.loadConfig(readableConfig);
    }

    @AfterEach
    void tearDown() {
        bukkitMock.close();
    }

    @Test
    void testSpellKey() {
        assertEquals("energyshield", energyShield.key());
    }

    @Test
    void testSpellType() {
        assertEquals(SpellType.AURA, energyShield.type());
    }
}
