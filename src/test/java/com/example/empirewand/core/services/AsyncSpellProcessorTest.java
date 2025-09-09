package com.example.empirewand.core.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import net.kyori.adventure.text.Component;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.empirewand.api.EffectService;
import com.example.empirewand.core.text.TextService;
import com.example.empirewand.spell.CastResult;
import com.example.empirewand.spell.Spell;
import com.example.empirewand.spell.SpellContext;

@ExtendWith(MockitoExtension.class)
class AsyncSpellProcessorTest {

    @Mock
    private Plugin plugin;

    @Mock
    private Spell<?> spell;

    @Mock
    private SpellContext context;

    @Mock
    private Player player;

    @Mock
    private EffectService effectService;

    @Mock
    private TextService textService;

    private AsyncSpellProcessor processor;

    @BeforeEach
    void setUp() {
        when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getLogger("TestLogger"));
        processor = new AsyncSpellProcessor(plugin);
    }

    @Test
    void testSuccessfulAsyncSpellCast() throws Exception {
        // Arrange
        CastResult expectedResult = CastResult.success(Component.text("Spell cast successfully"));
        when(spell.cast(context)).thenReturn(expectedResult);
        when(spell.key()).thenReturn("test-spell");

        // Act
        CompletableFuture<CastResult> future = processor.castSpellAsync(spell, context);
        CastResult result = future.get(5, TimeUnit.SECONDS);

        // Assert
        assertNotNull(result);
        assertEquals(expectedResult, result);
        verify(spell).cast(context);
    }

    @Test
    void testFailedAsyncSpellCast() throws Exception {
        // Arrange
        RuntimeException exception = new RuntimeException("Spell failed");
        when(spell.cast(context)).thenThrow(exception);
        when(spell.key()).thenReturn("test-spell");

        // Act
        CompletableFuture<CastResult> future = processor.castSpellAsync(spell, context);
        CastResult result = future.get(5, TimeUnit.SECONDS);

        // Assert
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertTrue(result.message().toString().contains("Spell casting failed"));
    }

    @Test
    void testAsyncSpellCastWithCallback() throws Exception {
        // Arrange
        CastResult successResult = CastResult.success(Component.text("Success"));
        when(spell.cast(context)).thenReturn(successResult);
        when(spell.key()).thenReturn("test-spell");
        when(context.caster()).thenReturn(player);
        when(player.isOnline()).thenReturn(true);

        // Act
        processor.castSpellAsyncWithCallback(spell, context, effectService, textService);

        // Give async processing time to complete
        Thread.sleep(100);

        // Assert
        verify(effectService).showSuccess(player, "spell-cast");
    }

    @Test
    void testAsyncSpellCastWithCallbackFailure() throws Exception {
        // Arrange
        CastResult failureResult = CastResult.fail(Component.text("Failed"));
        when(spell.cast(context)).thenReturn(failureResult);
        when(spell.key()).thenReturn("test-spell");
        when(context.caster()).thenReturn(player);
        when(player.isOnline()).thenReturn(true);

        // Act
        processor.castSpellAsyncWithCallback(spell, context, effectService, textService);

        // Give async processing time to complete
        Thread.sleep(100);

        // Assert
        verify(effectService).showError(player, "spell-failed");
    }

    @Test
    void testAsyncSpellCastWithCallbackException() throws Exception {
        // Arrange
        RuntimeException exception = new RuntimeException("Processing error");
        when(spell.cast(context)).thenThrow(exception);
        when(spell.key()).thenReturn("test-spell");
        when(context.caster()).thenReturn(player);
        when(player.isOnline()).thenReturn(true);

        // Act
        processor.castSpellAsyncWithCallback(spell, context, effectService, textService);

        // Give async processing time to complete
        Thread.sleep(100);

        // Assert
        verify(effectService).actionBar(eq(player), any(net.kyori.adventure.text.Component.class));
    }

    @Test
    void testMetricsCollection() {
        // Arrange
        CastResult successResult = CastResult.success(Component.text("Success"));
        when(spell.cast(context)).thenReturn(successResult);
        when(spell.key()).thenReturn("test-spell");

        // Act
        processor.castSpellAsync(spell, context);

        // Give async processing time to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Assert
        AsyncSpellProcessor.SpellProcessorMetrics metrics = processor.getMetrics();
        assertNotNull(metrics);
        assertTrue(metrics.totalSpellsProcessed() >= 1);
    }

    @Test
    void testShutdown() {
        // Act
        processor.shutdown();

        // Assert - Should not throw any exceptions
        assertDoesNotThrow(() -> processor.getMetrics());
    }

    @Test
    void testNullSpellHandling() {
        // Act & Assert
        assertThrows(NullPointerException.class,
                () -> processor.castSpellAsync(null, context));
    }

    @Test
    void testNullContextHandling() {
        // Act & Assert
        assertThrows(NullPointerException.class,
                () -> processor.castSpellAsync(spell, null));
    }
}