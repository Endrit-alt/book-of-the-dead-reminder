package com.bookofthedeadnotifier;

import net.runelite.client.config.*;

import java.awt.*;

@ConfigGroup(BookOfTheDeadNotifierConfig.GROUP)
public interface BookOfTheDeadNotifierConfig extends Config
{
	String GROUP = "bookofthedeadreminder";

    @ConfigItem(
        keyName = "reminderStyle",
        name = "Reminder Text",
        description = "The style of reminder text to display",
        position = 0
    )
    default BookOfTheDeadNotifierStyle reminderStyle()
    {
        return BookOfTheDeadNotifierStyle.LONG_TEXT;
    }

    @ConfigItem(
        keyName = "notification",
        name = "Notification on Reminder",
        description = "Sends a notification when warning appears",
        position = 1
    )
    default Notification notification()
    {
        return Notification.ON;
    }

    @ConfigItem(
        keyName = "hideReminderHotkey",
        name = "Hide Reminder Hotkey",
        description = "Confirm the current warning until it clears or the missing requirement changes",
        position = 2
    )
    default Keybind hideReminderHotkey()
    {
        return Keybind.NOT_SET;
    }

    @ConfigSection(
        name = "Notification Conditions",
        description = "Choose which conditions trigger a reminder",
        position = 3,
        closedByDefault = true
    )
    String notificationConditionsSection = "notificationConditions";

    @ConfigItem(
        keyName = "notifyOnMissingBook",
        name = "Notify on Missing Book",
        description = "Show reminder when Book of the Dead is missing",
        position = 0,
        section = notificationConditionsSection
    )
    default boolean notifyOnMissingBook()
    {
        return true;
    }

    @ConfigItem(
        keyName = "notifyOnMissingRunes",
        name = "Notify on Missing Runes",
        description = "Show reminder when thrall runes are missing",
        position = 1,
        section = notificationConditionsSection
    )
    default boolean notifyOnMissingRunes()
    {
        return true;
    }

    @ConfigItem(
        keyName = "notifyOnWrongSpellbook",
        name = "Notify on Wrong Spellbook",
        description = "Show reminder when not on Arceuus spellbook",
        position = 2,
        section = notificationConditionsSection
    )
    default boolean notifyOnWrongSpellbook()
    {
        return true;
    }

    @ConfigItem(
        keyName = "checkCarriedRunePouch",
        name = "Check Carried Book or Pouch",
        description = "Confirm your current non-Arceuus spellbook when carrying the Book of the Dead or a rune pouch; on Arceuus, a carried pouch warns about insufficient thrall runes even without the book",
        position = 3,
        section = notificationConditionsSection
    )
    default boolean checkCarriedRunePouch()
    {
        return true;
    }

    @ConfigSection(
        name = "Thrall Spell",
        description = "Which thrall you cast and when to warn about runes",
        position = 4,
        closedByDefault = true
    )
    String thrallSpellSection = "thrallSpell";

    @ConfigItem(
        keyName = "thrallTier",
        name = "Thrall Tier",
        description = "The thrall you cast; Auto uses the highest your Magic level allows",
        position = 0,
        section = thrallSpellSection
    )
    default ThrallTierSetting thrallTier()
    {
        return ThrallTierSetting.AUTO;
    }

    @Range(min = 1, max = 1000)
    @ConfigItem(
        keyName = "minCasts",
        name = "Minimum Casts",
        description = "Warn when you can cast fewer thralls than this",
        position = 1,
        section = thrallSpellSection
    )
    default int minCasts()
    {
        return 1;
    }

    @ConfigSection(
        name = "Display Options",
        description = "Customize the appearance of warnings",
        position = 5,
        closedByDefault = true
    )
    String displaySection = "displayOptions";

    @ConfigItem(
        keyName = "customText",
        name = "Custom Text",
        description = "Custom text to display when using CUSTOM_TEXT style",
        position = 0,
        section = displaySection
    )
    default String customText()
    {
        return "Cannot cast thralls!";
    }

    @ConfigItem(
        keyName = "flashReminderBox",
        name = "Flash the Reminder Box",
        description = "Makes the reminder box flash between two colors",
        position = 1,
        section = displaySection
    )
    default boolean flashReminderBox()
    {
        return false;
    }

    @Alpha
    @ConfigItem(
        keyName = "reminderColor",
        name = "Color",
        description = "Main color for the reminder box",
        position = 2,
        section = displaySection
    )
    default Color reminderColor()
    {
        return new Color(255, 0, 0, 150);
    }

    @Alpha
    @ConfigItem(
        keyName = "flashColor",
        name = "Flash Color",
        description = "Secondary color to flash between (if flashing enabled)",
        position = 3,
        section = displaySection
    )
    default Color flashColor()
    {
        return new Color(70, 70, 70, 150);
    }

	@ConfigSection(
		name = "Feedback",
		description = "Suggestions, bug reports, and support",
		position = 99,
		closedByDefault = true
	)
	String feedbackSection = "feedbackSection";

	@ConfigItem(
		keyName = "suggestButton",
		name = "Suggest a feature",
		description = "Have an idea or found a bug? Click the box to open the GitHub issues page",
		section = feedbackSection,
		position = 0
	)
	default boolean suggestButton()
	{
		return false;
	}

	@ConfigItem(
		keyName = "supportButton",
		name = "Support the original author ❤",
		description = "Click the box to open Jake's Ko-fi page",
		section = feedbackSection,
		position = 1
	)
	default boolean supportButton()
	{
		return false;
	}
}
