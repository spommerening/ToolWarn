# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build

Gradle runs on Java 21; the `compileJava` toolchain targets Java 25 (required by the Paper API).

```bash
# Full build (produces build/libs/ToolWarn-1.0.0.jar via Shadow)
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew shadowJar

# Compile only (no JAR)
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew compileJava
```

There are no tests. The JAR is self-contained (no runtime dependencies beyond the Paper server).

## Target environment

- Paper Server 26.1.2, API `io.papermc.paper:paper-api:26.1.2.build.66-stable`
- Java 25 class files; Gradle itself runs on Java 21
- Plugin main: `io.github.toolwarn.ToolWarnPlugin`

## Architecture

All logic runs on the main server thread — no async operations.

### Data flow

1. **`EntityEquipmentChangedEvent`** (Paper-native) fires on every equipment change including durability ticks. `EquipmentListener` resets the tracker state for the changed slot, then immediately checks thresholds on the new item.
2. **`PlayerItemDamageEvent`** schedules a 1-tick-delayed redundancy check via `getScheduler().runTaskLater` in case `EntityEquipmentChangedEvent` hasn't fired yet.
3. `EquipmentListener.checkAndWarn()` is the shared threshold evaluation path: it computes remaining durability percent, iterates the sorted threshold list, and fires any threshold whose index is not already in `DurabilityTracker`'s fired-set for that player+slot.
4. When a slot changes to a different item, `tracker.resetSlot()` clears the fired-set so thresholds re-arm. `PlayerQuitEvent` calls `tracker.removePlayer()`.

### Threshold indexing

`PluginConfig` sorts thresholds **descending** by percentage at load time. `DurabilityTracker` stores fired threshold indices (positions in that sorted list) per `UUID → EquipmentSlot`. When the config is reloaded (`/toolwarn reload`), `tracker.clearAll()` is called to invalidate all stale indices.

### Key API notes for Paper 26.1.2

- `EntityEquipmentChangedEvent.EquipmentChange` uses `newItem()` / `oldItem()` (not `newEquipment()`).
- `Sound` is an interface with static fields, not an enum. `Sound.valueOf()` is deprecated-for-removal; it is used with `@SuppressWarnings({"deprecation","removal"})` to keep the config format human-readable (`ENTITY_EXPERIENCE_ORB_PICKUP` style).
- `ChatColor` is fully deprecated. Color translation uses `LegacyComponentSerializer.legacyAmpersand().deserialize()` from the Adventure API instead.
- Durability is read via `org.bukkit.inventory.meta.Damageable`: guard with `hasMaxDamage()` and `hasDamageValue()` before accessing damage values.
