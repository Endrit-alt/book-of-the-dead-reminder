package com.bookofthedeadnotifier;

import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.MouseManager;
import net.runelite.client.ui.overlay.OverlayManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ReminderBehaviorTest
{
    @Mock private Client client;
    @Mock private PlayerLoadout loadout;
    @Mock private Notifier notifier;
    @Mock private ClientThread clientThread;
    @Mock private OverlayManager overlayManager;
    @Mock private BookOfTheDeadNotifierOverlay overlay;
    @Mock private KeyManager keyManager;
    @Mock private MouseManager mouseManager;
    @Mock private ConfirmMouseListener confirmMouseListener;
    @Spy private BookOfTheDeadNotifierConfig config = new BookOfTheDeadNotifierConfig() {};
    @InjectMocks private BookOfTheDeadNotifierPlugin plugin;
    private boolean bookPresent = true;
    private boolean pouchPresent;
    private int suppliedCasts = 10;

    @Before
    public void setUp() throws Exception
    {
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(client.getRealSkillLevel(Skill.MAGIC)).thenReturn(99);
        when(client.getVarbitValue(VarbitID.SPELLBOOK)).thenReturn(3);
        bookPresent = true;
        suppliedCasts = 10;
        when(loadout.carriedItems()).thenAnswer(call -> new PlayerLoadout.CarriedItems(bookPresent, pouchPresent));
        when(loadout.snapshot()).thenAnswer(call ->
        {
            Map<ThrallRune, Integer> runes = new EnumMap<>(ThrallRune.class);
            runes.put(ThrallRune.FIRE, suppliedCasts * 10);
            runes.put(ThrallRune.BLOOD, suppliedCasts * 5);
            runes.put(ThrallRune.COSMIC, suppliedCasts);
            return new PlayerLoadout.Snapshot(new PlayerLoadout.CarriedItems(bookPresent, pouchPresent), runes);
        });
        doAnswer(call -> { call.<Runnable>getArgument(0).run(); return null; })
            .when(clientThread).invokeLater(any(Runnable.class));
        plugin.startUp();
    }

    @Test
    public void confirmsAncientsEvenWithoutBookOrThrallRunes()
    {
        when(client.getVarbitValue(VarbitID.SPELLBOOK)).thenReturn(1);
        bookPresent = false;
        pouchPresent = true;
        suppliedCasts = 0;
        refresh();
        assertTrue(plugin.shouldShowWarning());
        assertEquals(MissingCondition.ARCEUUS_SPELLBOOK, plugin.getCurrentMissingCondition());
        assertEquals("Confirm spellbook : Ancients", plugin.getReminderLongText());
        plugin.confirmWarning(plugin.getWarningVersion());
        refresh();
        assertFalse(plugin.shouldShowWarning());
    }

    @Test
    public void confirmsStandardAndLunarRegardlessOfRuneSupply()
    {
        pouchPresent = true;
        bookPresent = false;
        when(client.getVarbitValue(VarbitID.SPELLBOOK)).thenReturn(0);
        refresh();
        assertEquals("Confirm spellbook : Standard", plugin.getReminderLongText());
        assertTrue(plugin.shouldShowWarning());
        plugin.confirmWarning(plugin.getWarningVersion());
        when(client.getVarbitValue(VarbitID.SPELLBOOK)).thenReturn(2);
        refresh();
        assertEquals("Confirm spellbook : Lunar", plugin.getReminderShortText());
        assertTrue(plugin.shouldShowWarning());
    }

    @Test
    public void changingFromConfirmedAncientsToStandardRequiresNewConfirmation()
    {
        pouchPresent = true;
        when(client.getVarbitValue(VarbitID.SPELLBOOK)).thenReturn(1);
        refresh();
        long oldVersion = plugin.getWarningVersion();
        plugin.confirmWarning(oldVersion);
        when(client.getVarbitValue(VarbitID.SPELLBOOK)).thenReturn(0);
        refresh();
        plugin.confirmWarning(oldVersion);
        assertTrue(plugin.shouldShowWarning());
        assertEquals("Confirm spellbook : Standard", plugin.getReminderLongText());
        verify(notifier, times(2)).notify(eq(config.notification()), anyString());
    }

    @Test
    public void switchingToArceuusRevealsMissingRunesEvenWithoutBook()
    {
        pouchPresent = true;
        bookPresent = false;
        suppliedCasts = 0;
        when(client.getVarbitValue(VarbitID.SPELLBOOK)).thenReturn(1);
        refresh();
        plugin.confirmWarning(plugin.getWarningVersion());
        when(client.getVarbitValue(VarbitID.SPELLBOOK)).thenReturn(3);
        refresh();
        assertTrue(plugin.shouldShowWarning());
        assertEquals("Missing thrall runes", plugin.getReminderLongText());
        suppliedCasts = 10;
        refresh();
        assertEquals(MissingCondition.BOOK_OF_THE_DEAD, plugin.getCurrentMissingCondition());
    }

    @Test
    public void disablingSpellbookPromptStillAllowsRuneWarning()
    {
        pouchPresent = true;
        when(client.getVarbitValue(VarbitID.SPELLBOOK)).thenReturn(1);
        suppliedCasts = 0;
        when(config.notifyOnWrongSpellbook()).thenReturn(false);
        refresh();
        assertTrue(plugin.shouldShowWarning());
        assertEquals(MissingCondition.THRALL_RUNES, plugin.getCurrentMissingCondition());
    }

    @Test
    public void extendedPouchRuleCanBeDisabled()
    {
        when(client.getVarbitValue(VarbitID.SPELLBOOK)).thenReturn(1);
        pouchPresent = true;
        suppliedCasts = 0;
        when(config.checkCarriedRunePouch()).thenReturn(false);
        refresh();
        assertFalse(plugin.shouldShowWarning());
    }

    @Test
    public void runeNotificationToggleStillApplies()
    {
        pouchPresent = true;
        suppliedCasts = 0;
        when(config.notifyOnMissingRunes()).thenReturn(false);
        refresh();
        assertFalse(plugin.shouldShowWarning());
    }

    @Test
    public void noPouchKeepsOriginalTwoOfThreeRule()
    {
        bookPresent = false;
        suppliedCasts = 0;
        refresh();
        assertFalse(plugin.shouldShowWarning());
        suppliedCasts = 10;
        refresh();
        assertEquals(MissingCondition.BOOK_OF_THE_DEAD, plugin.getCurrentMissingCondition());
        assertTrue(plugin.shouldShowWarning());
    }

    @Test
    public void confirmationSurvivesInventoryUpdatesAndDoesNotNotifyAgain()
    {
        suppliedCasts = 0;
        refresh();
        plugin.confirmWarning(plugin.getWarningVersion());
        refresh();
        refresh();
        assertFalse(plugin.shouldShowWarning());
        verify(notifier, times(1)).notify(eq(config.notification()), anyString());
    }

    @Test
    public void warningRearmsAfterRequirementsAreFixed()
    {
        suppliedCasts = 0;
        refresh();
        plugin.confirmWarning(plugin.getWarningVersion());
        suppliedCasts = 10;
        refresh();
        suppliedCasts = 0;
        refresh();
        assertTrue(plugin.shouldShowWarning());
        verify(notifier, times(2)).notify(eq(config.notification()), anyString());
    }

    @Test
    public void changedWarningNotifiesAndCannotBeDismissedByAnOldClick()
    {
        suppliedCasts = 0;
        refresh();
        long oldWarning = plugin.getWarningVersion();
        suppliedCasts = 10;
        bookPresent = false;
        refresh();
        plugin.confirmWarning(oldWarning);
        assertTrue(plugin.shouldShowWarning());
        assertEquals(MissingCondition.BOOK_OF_THE_DEAD, plugin.getCurrentMissingCondition());
        verify(notifier, times(2)).notify(eq(config.notification()), anyString());
    }

    @Test
    public void intermediateContainerUpdatesAreCoalesced()
    {
        suppliedCasts = 0;
        plugin.onItemContainerChanged(new ItemContainerChanged(InventoryID.INVENTORY.getId(), null));
        suppliedCasts = 10;
        plugin.onGameTick(new GameTick());
        assertFalse(plugin.shouldShowWarning());
        verifyNoInteractions(notifier);
    }

    @Test
    public void backingVarpUpdatesRefreshPouchRunes()
    {
        suppliedCasts = 0;
        VarbitChanged event = new VarbitChanged();
        event.setVarbitId(-1);
        event.setVarpId(123);
        plugin.onVarbitChanged(event);
        plugin.onGameTick(new GameTick());
        assertTrue(plugin.shouldShowWarning());
    }

    @Test
    public void lowRuneTextTracksCountsWithoutNotificationSpam()
    {
        when(config.minCasts()).thenReturn(5);
        suppliedCasts = 3;
        refresh();
        assertEquals("Low on thrall runes (3 casts)", plugin.getReminderLongText());
        suppliedCasts = 1;
        refresh();
        assertEquals("1 cast", plugin.getReminderShortText());
        verify(notifier, times(1)).notify(eq(config.notification()), anyString());
    }

    @Test
    public void logoutAndRestartClearConfirmation() throws Exception
    {
        suppliedCasts = 0;
        refresh();
        plugin.confirmWarning(plugin.getWarningVersion());
        when(client.getGameState()).thenReturn(GameState.LOGIN_SCREEN);
        GameStateChanged event = new GameStateChanged();
        event.setGameState(GameState.LOGIN_SCREEN);
        plugin.onGameStateChanged(event);
        assertFalse(plugin.shouldShowWarning());
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        event.setGameState(GameState.LOGGED_IN);
        plugin.onGameStateChanged(event);
        plugin.onGameTick(new GameTick());
        assertTrue(plugin.shouldShowWarning());
        plugin.confirmWarning(plugin.getWarningVersion());
        plugin.shutDown();
        verify(mouseManager).unregisterMouseListener(confirmMouseListener);
        plugin.startUp();
        assertTrue(plugin.shouldShowWarning());
    }

    @Test
    public void bankingOnlyBookThenWithdrawingRearmsAncientsPrompt()
    {
        pouchPresent = true;
        suppliedCasts = 0;
        when(client.getVarbitValue(VarbitID.SPELLBOOK)).thenReturn(1);
        refresh();
        plugin.confirmWarning(plugin.getWarningVersion());
        bookPresent = false;
        refresh();
        assertFalse(plugin.shouldShowWarning());
        bookPresent = true;
        refresh();
        assertTrue(plugin.shouldShowWarning());
        assertEquals("Confirm spellbook : Ancients", plugin.getReminderLongText());
        verify(notifier, times(2)).notify(eq(config.notification()), anyString());
        plugin.confirmWarning(plugin.getWarningVersion());
        refresh();
        assertFalse(plugin.shouldShowWarning());
    }

    @Test
    public void bankingOnlyPouchThenWithdrawingRearmsUnchangedRuneWarning()
    {
        pouchPresent = true;
        suppliedCasts = 0;
        refresh();
        plugin.confirmWarning(plugin.getWarningVersion());
        pouchPresent = false;
        refresh();
        assertFalse(plugin.shouldShowWarning());
        pouchPresent = true;
        refresh();
        assertTrue(plugin.shouldShowWarning());
        assertEquals(MissingCondition.THRALL_RUNES, plugin.getCurrentMissingCondition());
        verify(notifier, times(2)).notify(eq(config.notification()), anyString());
    }

    @Test
    public void fastBookDepositAndWithdrawalBeforeTickStillRearms()
    {
        assertFastBankingRearms(true);
    }

    @Test
    public void fastPouchDepositAndWithdrawalBeforeTickStillRearms()
    {
        assertFastBankingRearms(false);
    }

    private void assertFastBankingRearms(boolean bankBook)
    {
        pouchPresent = true;
        suppliedCasts = 0;
        when(client.getVarbitValue(VarbitID.SPELLBOOK)).thenReturn(1);
        refresh();
        long oldVersion = plugin.getWarningVersion();
        plugin.confirmWarning(oldVersion);
        clearInvocations(loadout);
        if (bankBook) bookPresent = false;
        else pouchPresent = false;
        inventoryChanged();
        bookPresent = true;
        pouchPresent = true;
        inventoryChanged();
        verify(loadout, never()).snapshot();
        plugin.onGameTick(new GameTick());
        verify(loadout, times(1)).snapshot();
        plugin.confirmWarning(oldVersion);
        assertTrue(plugin.shouldShowWarning());
        assertEquals("Confirm spellbook : Ancients", plugin.getReminderLongText());
        verify(notifier, times(2)).notify(eq(config.notification()), anyString());
    }

    @Test
    public void withdrawingPouchWithFixedRunesDoesNotShowFalseWarning()
    {
        pouchPresent = true;
        suppliedCasts = 0;
        refresh();
        plugin.confirmWarning(plugin.getWarningVersion());
        pouchPresent = false;
        inventoryChanged();
        pouchPresent = true;
        suppliedCasts = 10;
        inventoryChanged();
        plugin.onGameTick(new GameTick());
        assertFalse(plugin.shouldShowWarning());
        assertEquals(MissingCondition.NONE, plugin.getCurrentMissingCondition());
        verify(notifier, times(1)).notify(eq(config.notification()), anyString());
    }

    @Test
    public void alreadyVisibleWarningDoesNotNotifyAgainForBankingBook()
    {
        pouchPresent = true;
        when(client.getVarbitValue(VarbitID.SPELLBOOK)).thenReturn(1);
        refresh();
        bookPresent = false;
        refresh();
        bookPresent = true;
        refresh();
        assertTrue(plugin.shouldShowWarning());
        verify(notifier, times(1)).notify(eq(config.notification()), anyString());
    }

    @Test
    public void confirmingCurrentLoadoutBeforeNextTickDoesNotImmediatelyRearm()
    {
        pouchPresent = true;
        when(client.getVarbitValue(VarbitID.SPELLBOOK)).thenReturn(1);
        refresh();
        bookPresent = false;
        inventoryChanged();
        bookPresent = true;
        inventoryChanged();
        plugin.confirmWarning(plugin.getWarningVersion());
        plugin.onGameTick(new GameTick());
        assertFalse(plugin.shouldShowWarning());
        verify(notifier, times(1)).notify(eq(config.notification()), anyString());
    }

    private void inventoryChanged()
    {
        plugin.onItemContainerChanged(new ItemContainerChanged(InventoryID.INVENTORY.getId(), null));
    }

    private void refresh()
    {
        inventoryChanged();
        plugin.onGameTick(new GameTick());
    }
}
