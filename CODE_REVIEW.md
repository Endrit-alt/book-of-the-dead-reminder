# Source review and suggested features

Reviewed the complete upstream source at `90eee3bdd863159b9f30473eaca35c68304898ac`, the revision listed in RuneLite's Plugin Hub marker on 2026-09-25, and the changes in this fork. This includes every production class, existing tests, build/launch configuration, both plugin metadata files, README, license, funding file, and Gradle wrapper configuration. Game behavior was assessed from source and automated tests; no live account session was used.

## Findings addressed in this fork

| Finding | Cause in upstream | Change |
| --- | --- | --- |
| Pouch with ancient runes can produce no warning | `BookOfTheDeadNotifierPlugin.evaluateWarningState()` requires exactly two of the three prerequisites. The pouch reader does inspect rune types; multiple missing prerequisites suppress the warning. | A carried pouch prompts confirmation of the named non-Arceuus spellbook, regardless of the rune/book state. On Arceuus it warns about insufficient runes even if the book is also absent. |
| No visible acknowledgment control | `BookOfTheDeadNotifierOverlay` only renders text. | A visible Confirm button sits beside the message in every text style. Its full click is consumed; Alt-drag and clicks outside it retain their normal behavior. |
| Hide hotkey does not reliably stay hidden | `hideWarning()` resets the missing condition to `NONE`, so the next inventory event treats the same issue as new. | Acknowledgment retains the warning identity. It resets on resolution, a different warning/spellbook, logout/hopping, or restart. A queued click cannot dismiss a newer warning. |
| Pouch changes can be missed | `onVarbitChanged()` checks only individual varbit IDs. RuneLite also reports changes to their backing varps. | Both update paths mark state dirty; evaluate once at the next game tick, after related inventory and rune-slot updates. |
| Different warning can silently replace the old one | `showWarning()` sends a notification only if no warning was already visible. | A new warning identity sends one notification; repeated updates of the same warning do not. |
| State survives stopping and restarting the plugin | Shutdown unregisters the overlay/hotkey but does not reset warning state. | Clear state and button targets on shutdown, startup, logout and hopping; unregister the mouse listener too. |
| Inconsistent sizing across text styles | The old overlay uses negative padding for long/custom text and different render paths depending on style. | Measure the rendered text and button with explicit positive spacing and one render path. |
| Packaged metadata differs from root metadata | `src/main/resources/runelite-plugin.properties` has older tags and lacks the build field. | Synchronize metadata, name the fork contributor, and retain Jake's authorship and license. |

## Remaining limitations and code improvements

1. **Readiness is about supplies, not every casting restriction.** `ThrallTier.highestCastableAt()` falls back to Lesser even below level 38. Auto uses the real Magic level, so boosts and drains do not change the selected tier, and an explicitly selected tier can exceed the player's level. Prayer, quest completion, cooldowns and location restrictions are not checked. Describe the count as rune-supported casts; consider a separate optional casting-readiness check. Avoid noisy warnings on every temporary stat change.
2. **Only one issue is displayed at a time.** `evaluateWarningState()` deliberately gives a carried pouch's spellbook prompt priority, followed by runes. Confirming that spellbook keeps the intentional non-thrall setup quiet. A checklist would be better for users who want to see all missing requirements together.
3. **Rune quantity addition uses `int`.** `PlayerLoadout.countInInventory()` and `availableRunes()` can overflow when combining near-maximum stacks or multiple combination-rune stacks. This is an extreme-quantity edge case. Sum using `long` and clamp to `Integer.MAX_VALUE` before computing casts.
4. **The equipment/rune registry needs upkeep.** `ThrallRune` is a clear, small mapping of pure runes, combination runes and infinite sources, but future rune/staff/tome variants must be added. Compare it to RuneLite's current game data when new items release. Keep charged and empty tome variants distinct.
5. **Acknowledgment currently covers later depletion too.** A dismissed low-rune warning remains dismissed at zero casts because both have condition `THRALL_RUNES`. This is documented, but a configurable emergency alert at zero would be useful. Changing tier/minimum-casts while the same issue remains also does not itself clear acknowledgment.
6. **Feedback controls are checkbox links.** `openFeedbackLink()` opens a browser when a config value changes in either direction, including changes restored programmatically. A conventional plugin panel with explicit links would be clearer. The fork's feedback URL points to the fork; the donation link is explicitly labeled as supporting the original author.
7. **Build resolution is moving.** `build.gradle` uses RuneLite `latest.release`, which is appropriate for keeping up with the client but means later builds can resolve a different API. Add CI against the current release, and optionally allow an explicit RuneLite-version override for reproducing bugs. The bundled Gradle wrapper is 8.12.1; consider setting its distribution checksum when maintaining the wrapper.
8. **Very long custom text remains a single row.** The overlay autosizes to its text. A maximum width with word wrapping would prevent unusually long custom messages from extending beyond a small game window. Standard messages fit comfortably in the rendered previews.

## Performance assessment

The plugin is small and event-driven, so no noticeable FPS improvement is claimed without profiling. The useful improvements are reducing redundant work and making state transitions predictable.

| Area | Current behavior | Recommendation/status |
| --- | --- | --- |
| Event bursts | Upstream immediately recalculates after each relevant inventory, equipment or varbit event. One loadout change can produce several events. | Implemented: mark state dirty and read the settled loadout once at the next game tick. Config/startup refreshes remain explicit. |
| Inventory and equipment scans | `castsAvailable()` searches for the pouch, then scans inventory separately for each required rune. Warning evaluation searches for the pouch again, and the book check scans the containers again. | Next improvement: build one local loadout snapshot per refresh and reuse its book/pouch flags and rune totals. Avoid persistent caches that can become stale. |
| Pouch reads | Each of the three required rune types rereads all six type/quantity pairs and obtains the rune enum for each populated slot. | Next improvement: read each slot once, resolve the enum once, and accumulate rune totals in an `EnumMap<ThrallRune, Integer>` or a small array. |
| Frame rendering | The overlay measures text and creates its drawing components each visible frame. | Low priority: cache layout until text/font changes and reuse drawing components if a profiler shows allocation pressure. The overlay is tiny and only renders while a warning is visible. |
| Broad backing-varp fallback | Any backing-varp change can mark the loadout dirty, even when unrelated to thralls. This protects against missed pouch updates. | If profiling justifies it, map the spellbook/pouch varbits to their backing varps and filter those IDs. Preserve game-version compatibility and the new regression test. |

The highest-value correctness improvements were reliable acknowledgment, handling backing-varp updates, and clearing stale state. Those are implemented. A one-snapshot loadout reader and overflow-safe totals would be the next focused refactor; they are suggestions, not changes included in this fork yet.

## Suggested features, in priority order

| Priority | Feature | Player benefit |
| --- | --- | --- |
| 1 | Activity presets: Thralls / Ancients / Standard / Lunar | Choose the spellbook and rune loadout expected for a raid, boss or skilling activity. Confirm is then checking the intended setup, rather than treating every pouch as a possible thrall setup. |
| 2 | Missing-rune details | Show exactly what is short, such as "Missing cosmic runes" or "Need 20 more fire runes for 5 casts", combining inventory and pouch supplies. |
| 3 | Bank departure check | Recheck the complete setup on bank close, with optional rearming for the next trip. Reduce interruptions while assembling a loadout. |
| 4 | All-requirements checklist | Show spellbook, book and runes together, with a single acknowledgment for the current setup. Optional prayer/level/quest checks can be added separately. |
| 5 | Configurable acknowledgment duration | Offer until setup changes, next bank visit, or a short snooze. Optionally warn again when casts reach zero. |

Tier selection, minimum-cast thresholds, low-cast text, combination runes, Aether runes, equipped infinite sources, flashing, colors and a hide hotkey already existed upstream. Those do not need to be rebuilt as new features.

## Coverage and validation

Validated on 2026-09-25 with Java 11 and RuneLite 1.12.39 (resolved from `latest.release`): **38 tests passed**, the plugin JAR built successfully, and the rendered Ancients, Standard, normal, custom-text and flashing previews were visually inspected. No live game session was tested.

- `PlayerLoadout`, `ThrallRune`, `ThrallTier`, `ThrallTierSetting`: reviewed rune counting, all pouch slots and variants, combinations, infinite sources, tier selection and thresholds. Added loadout tests for ancient runes, split inventory/pouch supplies, banked pouches, later slots, missing enum/container data, and equipped versus carried staves.
- `BookOfTheDeadNotifierPlugin`, `MissingCondition`: reviewed each event handler, warning transition, notification, lifecycle hook and config link. Added behavioral tests for named spellbooks, priority, rearming, notifications, backing-varp updates and event coalescing.
- `BookOfTheDeadNotifierOverlay`, `BookOfTheDeadNotifierStyle`, `ConfirmMouseListener`: reviewed layout, text styles, rendering layers, mouse coordinates, click consumption and thread handoff. Tests use actual Guice construction and render the real overlay into images, then exercise the button at its canvas coordinates.
- `BookOfTheDeadNotifierConfig`: reviewed defaults, condition toggles, tier settings, text, colors, notification and hotkey settings. Preserved the existing configuration group and added one default-on pouch check.
- Existing `ThrallDataTest`, `EventBusRegistrationTest`, `PluginLauncher`, test logging, Gradle files and metadata: reviewed and retained; added focused regression tests and updated test dependencies. Existing event subscriber registration and tier-data tests remain in the suite.

Run `gradlew.bat test jar` on Windows or `./gradlew test jar` elsewhere. Visual previews are generated under `build/overlay-previews/`. A live RuneLite check is still useful for fixed/resizable/stretched layouts, real banking and world hopping; the fork is not automatically installed by building the JAR.
