# Book of the Dead Reminder

A RuneLite plugin that reminds you when you're missing requirements to cast thralls.

This is [Endrit-alt's fork](https://github.com/Endrit-alt/book-of-the-dead-reminder) of
[Jake's plugin](https://github.com/jakevollkommer/book-of-the-dead-reminder), based on
Plugin Hub revision `90eee3bdd863159b9f30473eaca35c68304898ac`. The original BSD license and author credit are retained.

### Changes in this fork

- **Confirm button** next to every reminder. Left-click it, or use the existing hide hotkey, to acknowledge the current warning.
- **Persistent acknowledgment**: inventory updates, rune-count changes, and visual setting changes do not bring back the same warning or repeat its notification. A warning returns after its condition clears and recurs, when a different requirement becomes the warning, when the spellbook being confirmed changes, after logout/world hopping, or after restarting the plugin. Confirming a low-rune warning also acknowledges its later depletion until one of these resets.
- **Check Carried Rune Pouch**, enabled by default: carrying a pouch on a non-Arceuus spellbook prompts **Confirm spellbook : Ancients**, **Confirm spellbook : Standard**, or **Confirm spellbook : Lunar**, even without thrall runes or the book. Confirm acknowledges your intentional spellbook choice. It does not change the game's spellbook.
- Spellbook confirmation takes priority. Switching to Arceuus reveals any missing-rune warning, even if the book is also missing. Fixing the runes can then reveal a missing-book warning. Each notification condition can still be disabled separately.
- Pouch types and quantities are still checked against your selected tier. Inventory runes, combination runes, and equipped infinite sources count toward the total. An ancient-rune pouch with sufficient thrall supplies elsewhere does not produce a rune warning.
- Inventory and pouch updates are combined once per game tick, including updates reported through backing varps, to avoid warnings from intermediate loadouts.

The Confirm button consumes its click and does not click the game underneath. Alt-drag still moves the reminder. The reminder renders above widgets so the button remains visible while banking.

## Features

The original rule warns when you have **exactly 2 out of 3 requirements** for casting thralls. This fork also checks a carried pouch: first confirm a non-Arceuus spellbook, then check thrall runes on Arceuus. Turn off **Check Carried Rune Pouch** to use only the original rule:

### The 3 Requirements:
1. **Arceuus Spellbook** - You must be on the Arceuus spellbook
2. **Thrall Runes** - You must have enough runes for the thrall you cast
3. **Book of the Dead** - You must have the Book of the Dead equipped or in your inventory

### All Three Thrall Tiers

Ghost, skeleton and zombie thralls of the same tier cost the same runes, so the tier is all the plugin needs to know:

| Tier | Magic | Runes per cast |
|----------|-------|-----------------------------|
| Lesser | 38 | 10 air, 5 mind, 1 cosmic |
| Superior | 57 | 10 earth, 5 death, 1 cosmic |
| Greater | 76 | 10 fire, 5 blood, 1 cosmic |

By default the plugin checks the highest tier your Magic level can cast, and you can pin it to a specific tier if you deliberately cast a cheaper one.

### Casts Remaining

Instead of raw rune counts, the plugin works out how many thralls you can actually cast and warns when you drop below your **Minimum Casts** setting. When you still have some left, the reminder names the shortfall — "Low on thrall runes (3 casts)".

### Reminder Messages

The plugin displays a reminder above your chatbox when you're missing one of these:

- Missing **Book of the Dead**: "Missing Book of the Dead"
- Other **Spellbook**: "Confirm spellbook : Ancients", "Confirm spellbook : Standard", or "Confirm spellbook : Lunar"
- Out of **Runes**: "Missing thrall runes"
- Low on **Runes**: "Low on thrall runes (3 casts)"

### Configuration Options

- **Reminder Text Style**: Choose between long text, short text, or custom text
- **Notification on Reminder**: Send system notification when reminder appears
- **Thrall Tier**: The thrall you cast, or Auto to follow your Magic level
- **Minimum Casts**: Warn when you can cast fewer thralls than this (default: 1)
- **Display Options**: Customize colors and enable flashing
- **Hide Reminder Hotkey**: Acknowledge the current warning, just like Confirm
- **Check Carried Rune Pouch**: Confirm a non-Arceuus spellbook and check insufficient thrall runes even when other requirements are also missing

Long and short text styles both name the spellbook. Custom text still uses the message you configure.

### Smart Rune Detection

The plugin intelligently detects:
- Runes in both inventory and rune pouch, including divine rune pouches
- Combo runes (Dust, Mist and Smoke count as air; Dust, Mud and Lava as earth; Lava, Smoke, Steam and Sunfire as fire)
- Aether runes (count as cosmic runes)
- Elemental staves and tomes as an infinite source of their element (air, earth and fire staves and battlestaves, the combo battlestaves, Tome of Fire and Tome of Earth)

## Build and run this fork

Use JDK 11 or newer. On Windows:

```powershell
.\gradlew.bat test jar
.\gradlew.bat runPlugin
```

On macOS/Linux, use `./gradlew` instead. `runPlugin` starts a developer RuneLite client with this fork loaded; disable the original Plugin Hub copy in that client to avoid duplicate reminders. The built JAR is under `build/libs/`. It is not a standalone application or automatically installed into your regular RuneLite client. This fork has not been submitted to the Plugin Hub.

## Review and possible next features

See [CODE_REVIEW.md](CODE_REVIEW.md) for the full source review, remaining limitations, and prioritized suggestions. Tests cover pouch contents, warning transitions, actual overlay rendering and Confirm mouse handling. Rendered test previews are generated in `build/overlay-previews/`.
