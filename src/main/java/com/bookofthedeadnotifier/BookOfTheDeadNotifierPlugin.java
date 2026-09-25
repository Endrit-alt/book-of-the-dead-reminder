package com.bookofthedeadnotifier;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;
import net.runelite.client.util.LinkBrowser;

@Slf4j
@PluginDescriptor(
    name = "Book of the Dead Reminder",
    description = "Reminds you when you seem to be missing a thrall requirement (Book of the Dead, Arceuus spellbook, Thrall runes)",
    tags = {"jake", "arceuus", "thrall", "thralls", "book of the dead", "spell", "spellbook", "reminder", "necromancy", "resurrect", "ghost", "skeleton", "zombie", "rune", "runes", "lesser", "superior", "greater", "casts", "air", "earth", "fire", "mind", "death", "blood", "cosmic", "staff", "tome", "rune pouch"}
)
public class BookOfTheDeadNotifierPlugin extends Plugin
{
    private static final int ARCEUUS_SPELLBOOK = 3;
    private static final String SUGGEST_BUTTON_KEY = "suggestButton";
    private static final String SUPPORT_BUTTON_KEY = "supportButton";
    private static final String ISSUES_URL = "https://github.com/Endrit-alt/book-of-the-dead-reminder/issues";
    private static final String KO_FI_URL = "https://ko-fi.com/jakevollkommer";

    @Inject
    private Client client;

    @Inject
    private BookOfTheDeadNotifierConfig config;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private BookOfTheDeadNotifierOverlay overlay;

    @Inject
    private Notifier notifier;

    @Inject
    private KeyManager keyManager;

    @Inject
    private MouseManager mouseManager;

    @Inject
    private ConfirmMouseListener confirmMouseListener;

    @Inject
    private ClientThread clientThread;

    @Inject
    private PlayerLoadout loadout;

    private boolean hasArceuusSpellbook = false;
    private boolean hasSufficientThrallRunes = false;
    private boolean hasBookOfTheDead = false;
    private boolean hasRunePouch = false;
    private boolean carriedItemsKnown = false;
    private boolean acknowledgmentNeedsReset = false;
    private int spellbook = -1;
    private int warningSpellbook = -1;
    private volatile boolean warningShown = false;
    private volatile MissingCondition currentMissingCondition = MissingCondition.NONE;
    private volatile long warningVersion = 0;
    private boolean stateDirty = true;
    private volatile boolean active;
    private int castsAvailable = 0;
    private int magicLevel = 1;

    private final HotkeyListener hotkeyListener = new HotkeyListener(() -> config.hideReminderHotkey())
    {
        @Override
        public void hotkeyPressed()
        {
            confirmWarning(warningVersion);
        }
    };

    @Override
    protected void startUp() throws Exception
    {
        active = true;
        clearWarning();
        overlayManager.add(overlay);
        keyManager.registerKeyListener(hotkeyListener);
        confirmMouseListener.reset();
        mouseManager.registerMouseListener(confirmMouseListener);
        clientThread.invokeLater(this::refreshPlayerState);
        log.info("Book of the Dead Reminder started!");
    }

    @Override
    protected void shutDown() throws Exception
    {
        active = false;
        clearWarning();
        overlayManager.remove(overlay);
        keyManager.unregisterKeyListener(hotkeyListener);
        mouseManager.unregisterMouseListener(confirmMouseListener);
        confirmMouseListener.reset();
        log.info("Book of the Dead Reminder stopped!");
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged event)
    {
        // The server can update the backing varp without emitting an individual varbit id.
        if (event.getVarpId() >= 0 || event.getVarbitId() == VarbitID.SPELLBOOK
            || PlayerLoadout.isRunePouchVarbit(event.getVarbitId()))
        {
            stateDirty = true;
        }
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (stateDirty)
        {
            refreshPlayerState();
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGGED_IN)
        {
            stateDirty = true;
        }
        else if (event.getGameState() == GameState.LOGIN_SCREEN
            || event.getGameState() == GameState.HOPPING)
        {
            clearWarning();
        }
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event)
    {
        if (isInventoryOrEquipment(event.getContainerId()))
        {
            if (active && client.getGameState() == GameState.LOGGED_IN)
            {
                // Preserve item transitions even when deposit and withdrawal occur before the next tick.
                checkCarriedItems(loadout.carriedItems());
            }
            stateDirty = true;
        }
    }

    // Magic experience arrives constantly in combat, so only a level change can move the auto tier.
    @Subscribe
    public void onStatChanged(StatChanged event)
    {
        boolean magicLevelChanged = event.getSkill() == Skill.MAGIC && event.getLevel() != magicLevel;
        if (magicLevelChanged)
        {
            stateDirty = true;
        }
    }

    // The config panel cannot host real buttons, so the Feedback "buttons" are checkboxes
    // that act as buttons: any click of the box, tick or untick, opens the link.
    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (!BookOfTheDeadNotifierConfig.GROUP.equals(event.getGroup()))
        {
            return;
        }

        if (openFeedbackLink(event))
        {
            return;
        }

        clientThread.invokeLater(this::refreshPlayerState);
    }

    private boolean openFeedbackLink(ConfigChanged event)
    {
        if (event.getNewValue() == null)
        {
            return false;
        }

        if (SUGGEST_BUTTON_KEY.equals(event.getKey()))
        {
            LinkBrowser.browse(ISSUES_URL);
            return true;
        }

        if (SUPPORT_BUTTON_KEY.equals(event.getKey()))
        {
            LinkBrowser.browse(KO_FI_URL);
            return true;
        }

        return false;
    }

    private void refreshPlayerState()
    {
        if (!active || client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }

        stateDirty = false;
        magicLevel = client.getRealSkillLevel(Skill.MAGIC);
        checkSpellbook();
        PlayerLoadout.Snapshot snapshot = loadout.snapshot();
        checkThrallRunes(snapshot);
        checkCarriedItems(snapshot.getCarriedItems());
        evaluateWarningState();
    }

    private boolean isInventoryOrEquipment(int containerId)
    {
        return containerId == InventoryID.INVENTORY.getId()
            || containerId == InventoryID.EQUIPMENT.getId();
    }

    private void checkSpellbook()
    {
        spellbook = client.getVarbitValue(VarbitID.SPELLBOOK);
        hasArceuusSpellbook = spellbook == ARCEUUS_SPELLBOOK;
    }

    private void checkThrallRunes(PlayerLoadout.Snapshot snapshot)
    {
        ThrallTier tier = config.thrallTier().resolve(magicLevel);
        castsAvailable = snapshot.castsAvailable(tier);
        hasSufficientThrallRunes = castsAvailable >= config.minCasts();
    }

    private void checkCarriedItems(PlayerLoadout.CarriedItems carriedItems)
    {
        boolean bookPresent = carriedItems.hasBookOfTheDead();
        boolean pouchPresent = carriedItems.hasRunePouch();
        if (carriedItemsKnown && ((!hasBookOfTheDead && bookPresent) || (!hasRunePouch && pouchPresent)))
        {
            acknowledgmentNeedsReset = true;
            // A click queued for the previous loadout must not acknowledge the new one.
            warningVersion++;
            overlay.clearConfirmTarget();
        }
        hasBookOfTheDead = bookPresent;
        hasRunePouch = pouchPresent;
        carriedItemsKnown = true;
    }

    private void evaluateWarningState()
    {
        // Confirm an intentional non-Arceuus loadout before asking for thrall supplies.
        boolean checkPouch = config.checkCarriedRunePouch() && hasRunePouch;
        MissingCondition missingCondition;
        if (checkPouch && !hasArceuusSpellbook && config.notifyOnWrongSpellbook())
        {
            missingCondition = MissingCondition.ARCEUUS_SPELLBOOK;
        }
        else if (checkPouch && !hasSufficientThrallRunes && config.notifyOnMissingRunes())
        {
            missingCondition = MissingCondition.THRALL_RUNES;
        }
        else
        {
            missingCondition = countConditionsMet() == 2 ? determineMissingCondition() : MissingCondition.NONE;
        }

        if (!isConditionNotificationEnabled(missingCondition))
        {
            missingCondition = MissingCondition.NONE;
        }

        // Keep the condition after Confirm, so unrelated inventory updates cannot re-show it.
        boolean rearmAcknowledgedWarning = acknowledgmentNeedsReset && !warningShown;
        acknowledgmentNeedsReset = false;
        int nextWarningSpellbook = missingCondition == MissingCondition.ARCEUUS_SPELLBOOK ? spellbook : -1;
        if (missingCondition == currentMissingCondition && nextWarningSpellbook == warningSpellbook
            && !rearmAcknowledgedWarning)
        {
            return;
        }

        currentMissingCondition = missingCondition;
        warningSpellbook = nextWarningSpellbook;
        warningVersion++;
        warningShown = missingCondition != MissingCondition.NONE;
        overlay.clearConfirmTarget();
        if (warningShown)
        {
            sendNotification();
        }
    }

    private int countConditionsMet()
    {
        int count = 0;
        if (hasArceuusSpellbook) count++;
        if (hasSufficientThrallRunes) count++;
        if (hasBookOfTheDead) count++;
        return count;
    }

    private boolean isConditionNotificationEnabled(MissingCondition condition)
    {
        switch (condition)
        {
            case BOOK_OF_THE_DEAD:
                return config.notifyOnMissingBook();
            case THRALL_RUNES:
                return config.notifyOnMissingRunes();
            case ARCEUUS_SPELLBOOK:
                return config.notifyOnWrongSpellbook();
            default:
                return false;
        }
    }

    private MissingCondition determineMissingCondition()
    {
        if (!hasBookOfTheDead)
        {
            return MissingCondition.BOOK_OF_THE_DEAD;
        }

        if (!hasArceuusSpellbook)
        {
            return MissingCondition.ARCEUUS_SPELLBOOK;
        }

        if (!hasSufficientThrallRunes)
        {
            return MissingCondition.THRALL_RUNES;
        }

        return MissingCondition.NONE;
    }

    public MissingCondition getCurrentMissingCondition()
    {
        return currentMissingCondition;
    }

    public String getReminderLongText()
    {
        if (currentMissingCondition == MissingCondition.ARCEUUS_SPELLBOOK)
        {
            return "Confirm spellbook : " + getSpellbookName();
        }
        return isRunningLowOnRunes()
            ? "Low on thrall runes (" + describeCastsRemaining() + ")"
            : currentMissingCondition.getLongText();
    }

    public String getReminderShortText()
    {
        if (currentMissingCondition == MissingCondition.ARCEUUS_SPELLBOOK)
        {
            return getSpellbookName() + "!";
        }
        return isRunningLowOnRunes()
            ? describeCastsRemaining()
            : currentMissingCondition.getShortText();
    }

    // Below the configured minimum but not empty, so naming the shortfall beats "missing runes".
    private boolean isRunningLowOnRunes()
    {
        return currentMissingCondition == MissingCondition.THRALL_RUNES && castsAvailable > 0;
    }

    private String describeCastsRemaining()
    {
        return castsAvailable == 1 ? "1 cast" : castsAvailable + " casts";
    }

    private void sendNotification()
    {
        if (!config.notification().isEnabled())
        {
            return;
        }

        notifier.notify(config.notification(), "Thrall Reminder: " + getReminderLongText());
    }

    public long getWarningVersion()
    {
        return warningVersion;
    }

    private String getSpellbookName()
    {
        switch (warningSpellbook)
        {
            case 0:
                return "Standard";
            case 1:
                return "Ancients";
            case 2:
                return "Lunar";
            case ARCEUUS_SPELLBOOK:
                return "Arceuus";
            default:
                return "Unknown (" + warningSpellbook + ")";
        }
    }

    // Both mouse events and hotkeys arrive off the client thread.
    public void confirmWarning(long expectedVersion)
    {
        clientThread.invokeLater(() ->
        {
            if (active && warningShown && warningVersion == expectedVersion)
            {
                warningShown = false;
                acknowledgmentNeedsReset = false;
                overlay.clearConfirmTarget();
            }
        });
    }

    private void clearWarning()
    {
        hasBookOfTheDead = false;
        hasRunePouch = false;
        carriedItemsKnown = false;
        acknowledgmentNeedsReset = false;
        warningShown = false;
        currentMissingCondition = MissingCondition.NONE;
        warningSpellbook = -1;
        warningVersion++;
        stateDirty = true;
        overlay.clearConfirmTarget();
    }

    public boolean shouldShowWarning()
    {
        return warningShown;
    }

    @Provides
    BookOfTheDeadNotifierConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(BookOfTheDeadNotifierConfig.class);
    }
}
