# ToolWarn

> **Never lose a tool to unexpected breakage again.**

ToolWarn is a lightweight [Paper](https://papermc.io/) server plugin that watches every item your players have equipped — armor, mainhand, offhand — and sends them a private warning the moment durability drops below a threshold you define. No polling, no lag, no spam.

---

## Features

- **Monitors all 6 equipment slots** — head, chest, legs, feet, main hand, off hand
- **Unlimited configurable thresholds** — set as many warning levels as you want (20%, 10%, 5%, …)
- **Each threshold fires exactly once per item** — no repeated messages for the same item in the same slot; resets automatically when the player swaps items
- **Immediate warnings on equip** — if a player picks up an already-damaged item, all applicable thresholds fire right away
- **Chat + ActionBar** — warnings appear both in chat and as an ActionBar overlay (individually toggleable)
- **Configurable sound** — plays an optional sound effect on each warning
- **Hot-reload** — `/toolwarn reload` applies config changes without a server restart
- **Zero external dependencies** — only the Paper API; the final JAR is under 20 KB

---

## Requirements

| Component | Version |
|---|---|
| Paper Server | 26.1.2 |
| Java (runtime) | 25+ |

> ToolWarn targets the Paper-native `EntityEquipmentChangedEvent` introduced in modern Paper builds. It will **not** run on Spigot or older Paper versions.

---

## Installation

1. Download `ToolWarn-1.0.0.jar` from the [Releases](../../releases) page.
2. Drop the JAR into your server's `plugins/` folder.
3. Start or restart the server. A `config.yml` is generated automatically under `plugins/ToolWarn/`.
4. Edit `config.yml` to your liking and run `/toolwarn reload` — no restart needed.

---

## Configuration

**`plugins/ToolWarn/config.yml`**

```yaml
# Thresholds are in percentage of remaining durability (0.0 – 1.0).
# You can add as many entries as you like.
# They are evaluated from highest to lowest automatically — order in this file does not matter.

thresholds:
  - percentage: 0.20
    message: "&e⚠ &f{item} &eis getting worn down! &7({durability}/{max}, {percent}% left)"
  - percentage: 0.10
    message: "&c✖ &f{item} &cis nearly broken! &7({durability}/{max}, {percent}% left)"

# Sound played on each warning. Set to "" to disable.
# Use the UPPER_SNAKE_CASE Bukkit sound name, e.g. ENTITY_EXPERIENCE_ORB_PICKUP.
warning-sound: "ENTITY_EXPERIENCE_ORB_PICKUP"

# Also show the warning in the ActionBar (above the hotbar) in addition to chat.
send-actionbar: true
```

### Message placeholders

| Placeholder | Description | Example |
|---|---|---|
| `{item}` | Item name (custom name preferred, otherwise type) | `Diamond Sword` |
| `{slot}` | Equipment slot | `HAND`, `CHEST` |
| `{durability}` | Remaining durability points | `156` |
| `{max}` | Maximum durability points | `1561` |
| `{percent}` | Remaining durability as whole number | `10` |

Standard `&`-style color codes and formatting codes (`&a`, `&c`, `&l`, …) are supported in all messages.

### Adding more thresholds

```yaml
thresholds:
  - percentage: 0.50
    message: "&6⚡ &f{item} &6is at half durability. &7({durability}/{max})"
  - percentage: 0.20
    message: "&e⚠ &f{item} &eis getting worn down! &7({durability}/{max}, {percent}% left)"
  - percentage: 0.10
    message: "&c✖ &f{item} &cis nearly broken! &7({durability}/{max}, {percent}% left)"
  - percentage: 0.05
    message: "&4&l!!! &f{item} &4&lis about to break! &7({durability}/{max})"
```

---

## Commands & Permissions

| Command | Description | Permission |
|---|---|---|
| `/toolwarn reload` | Hot-reloads `config.yml` without restarting | `toolwarn.admin` |
| `/toolwarn status` | Shows loaded threshold count and tracked player count | `toolwarn.admin` |

`toolwarn.admin` defaults to **op**.

---

## How it works

ToolWarn listens to Paper's `EntityEquipmentChangedEvent`, which fires on every equipment change including durability decrements. When the event fires for a player:

1. The tracker state for the changed slot is **reset** (the old item's fired thresholds are cleared).
2. The new item is **immediately evaluated** against all configured thresholds.
3. Any threshold whose percentage is ≥ the item's remaining durability fires exactly **once** and is recorded.

A secondary `PlayerItemDamageEvent` handler schedules a 1-tick-delayed redundancy check so no damage event is missed between equipment change ticks. When a player disconnects, all their tracker data is removed.

---

## Building from source

### Prerequisites

| Tool | Version |
|---|---|
| JDK | 21 (to run Gradle) + 25 (to compile against Paper API) |
| Git | any |

The Gradle wrapper is included — no system-wide Gradle installation required.

**Install OpenJDK 21 and 25 on Ubuntu/Debian:**

```bash
sudo apt-get install openjdk-21-jdk openjdk-25-jdk-headless
```

### Build steps

```bash
# Clone the repository
git clone https://github.com/yourname/toolwarn.git
cd toolwarn

# Build (Gradle runs on Java 21; the toolchain compiles against Java 25)
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew shadowJar
```

The compiled plugin JAR is written to:

```
build/libs/ToolWarn-1.0.0.jar
```

### Why two Java versions?

The Paper 26.1.2 API is compiled as Java 25 class files, so the **compiler** must target Java 25. However, Gradle's Kotlin DSL build scripts have a known compatibility issue when Gradle itself is launched with Java 25. The workaround is to **run Gradle with Java 21** and let Gradle's [toolchain support](https://docs.gradle.org/current/userguide/toolchains.html) invoke the Java 25 `javac` for the actual compilation step. Gradle will locate the Java 25 JDK automatically via the system's JVM installations.

### Other useful Gradle tasks

```bash
# Compile only (no JAR)
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew compileJava

# Clean build output
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew clean

# Full build + shadowJar (equivalent to the build command above)
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew build
```

---

## Comparison with similar plugins

| Feature | ToolWarn | DurabilityWarnings | DurabilityAlert |
|---|---|---|---|
| Paper-native event (no polling) | ✅ | ❌ | ❌ |
| Unlimited thresholds | ✅ | ❌ (fixed 3) | ❌ |
| Per-slot warnings with slot context | ✅ | ❌ | ❌ |
| Hot-reload without restart | ✅ | ❌ | ❌ |
| Zero external dependencies | ✅ | ✅ | ✅ |

---

## License

MIT — see [LICENSE](LICENSE) for details.
