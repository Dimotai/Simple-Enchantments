# Simple Enchantments

A comprehensive enchanting system for **Hytale** — craft scrolls, enchant your gear, and extend the system with your own mods.

Simple Enchantments adds an **Enchanting Table**, **31 built-in enchantments**, an **enchantment scroll system**, **usefull commands**, in-game **configuration UI**, **localisation** for 11 languages, and a **public API** that lets other mods register their own enchantments, categories, and scrolls at runtime.

> **Version:** 0.9.1 · **Java:** 25 · **License:** _see [LICENSE.md](LICENSE.md)_ · **Wiki/Documentation:** _coming soon_

#### If you are looking for a Hytale Server, consider using my code and link at BisectHosting. That way you get 25% off and we get a commission which helps with further development:
[![https://www.bisecthosting.com/Herolias](https://www.bisecthosting.com/partners/custom-banners/87d24680-40cb-471d-b1a9-bc3c9eb9ce68.webp)](https://www.bisecthosting.com/Herolias?r=GitHub)
---

## Table of Contents

- [Features](#features)
- [Building from Source](#building-from-source)
- [Folder Structure](#folder-structure)
- [Technical Overview](#technical-overview)
  - [Core Plugin](#core-plugin)
  - [Enchantment Engine](#enchantment-engine)
  - [ECS Systems](#ecs-systems)
  - [Enchantment API (Public)](#enchantment-api-public)
  - [UI System](#ui-system)
  - [Configuration](#configuration)
  - [Localisation](#localisation)
  - [Commands](#commands)
  - [Custom Interactions](#custom-interactions)
  - [Crafting & Recipes](#crafting--recipes)
  - [Asset Pack](#asset-pack)
  - [Optional Integrations](#optional-integrations)

---

## Features

| Category | Highlights |
|---|---|
| **Enchantments** | 31 built-in enchantments across melee, ranged, armor, shields, staves, and tools |
| **Scrolls** | Craft enchantment scrolls at a tiered Enchanting Table |
| **Metadata Storage** | Enchantments stored as BSON in item metadata — no extra JSON item files needed |
| **Enchantment Glow** | Runtime-injected visual glow on enchanted items via `ItemAppearanceConditions` |
| **Cleansing** | Remove enchantments with a Cleansing Scroll |
| **Custom Scrolls** | Merge Scrolls or use the /enchant command to give custom scrolls enchantments with level up to 100 |
| **Configuration** | Full in-game config UI + JSON config with per-enchantment multipliers, recipes, and toggles |
| **Localisation** | Translations for 11 languages (EN, DE, ES, FR, ID, IT, NL, PT-BR, RU, SV, UK) |
| **API** | Public API for addon mods to register enchantments, categories, scrolls, and conflicts |
| **Tooltips** | Optional tooltips via [DynamicTooltipsLib](https://github.com/Herolias/DynamicTooltipsLib) NOTE: Dynamic Tooltips Lib will become a mandatory dependency in version 1.0.0 |
| **Cross-Mod** | Integration with [Perfect Parries](https://www.curseforge.com/hytale/mods/perfect-parries) for Riposte & Coup de Grâce enchantments, [MMO Skill Tree](https://www.curseforge.com/hytale/mods/mmo-skill-tree) for custom Enchantment XP System |

---

## Building from Source

### Prerequisites

- **Hytale** installed via the official launcher (the build references the server JAR from your install)
- **Java 25** (the Hytale server runs on Java 25)
- **Gradle** (bundled wrapper included — no global install required)

### Steps

1. **Clone the repository**
   ```bash
   git clone https://github.com/Herolias/Simple-Enchantments.git
   cd Simple-Enchantments
   ```

2. **Verify your Hytale installation**
   
   The build script automatically locates `HytaleServer.jar` from your Hytale install directory:
   ```
   ~/AppData/Roaming/Hytale/install/release/package/game/latest/Server/HytaleServer.jar
   ```
   If you use a different patchline, edit `patchline` in `gradle.properties`.

3. **Build the plugin**

   ```bash
   ./gradlew build
   ```
   The compiled JAR will be in `build/libs/`.

4. **(Optional) Run a development server** (IntelliJ IDEA)
   
   Open the project in IntelliJ — the build script auto-generates a `HytaleServer` run configuration that starts the server with your plugin and asset packs loaded.

### Configuration Reference (`gradle.properties`)

| Property | Default | Description |
|---|---|---|
| `version` | `0.9.1` | Plugin version (semantic versioning) |
| `java_version` | `25` | Java toolchain version |
| `includes_pack` | `true` | Load the bundled asset pack alongside the plugin |
| `patchline` | `release` | Hytale release channel (`release` or `pre-release`) |
| `load_user_mods` | `false` | Also load mods from the user's standard Mods folder during dev |

---

## Folder Structure

```
src/
└── main/
    ├── java/
    │   ├── com/al3x/
    │   │   └── HStats.java                    # Mod analytics (hstats.dev)
    │   │
    │   └── org/herolias/plugin/
    │       ├── SimpleEnchanting.java           # Plugin entry point (setup, start, shutdown)
    │       │
    │       ├── api/                            # ── Public API ──
    │       │   ├── EnchantmentApi.java          # API interface for third-party mods
    │       │   ├── EnchantmentApiImpl.java       # API implementation
    │       │   ├── EnchantmentApiProvider.java   # Service locator
    │       │   ├── EnchantmentBuilder.java       # Fluent builder for registering enchantments
    │       │   ├── ScrollBuilder.java            # Builder for scroll definitions
    │       │   ├── ScrollDefinition.java         # Scroll data record
    │       │   ├── CraftingCategoryDefinition.java # Enchanting Table tab registration
    │       │   ├── MultiplierDefinition.java     # Config multiplier metadata
    │       │   ├── ScaleType.java                # Scaling curve types (linear, diminishing, etc.)
    │       │   └── event/                        # Custom API events
    │       │       ├── EnchantmentActivatedEvent.java
    │       │       └── ItemEnchantedEvent.java
    │       │
    │       ├── command/                         # ── Chat Commands ──
    │       │   ├── EnchantCommand.java           # /enchant — apply enchantments
    │       │   ├── EnchantingCommand.java         # /enchanting — open enchanting UI
    │       │   ├── EnchantConfigCommand.java      # /enchantconfig — in-game config editor
    │       │   └── GiveEnchantedCommand.java      # /give override — give pre-enchanted items
    │       │
    │       ├── config/                          # ── Configuration ──
    │       │   ├── ConfigManager.java            # Load/save/migrate JSON config
    │       │   ├── SmartConfigManager.java        # Smart config with snapshots
    │       │   ├── EnchantingConfig.java          # Config data class (multipliers, recipes, toggles)
    │       │   ├── UserSettings.java              # Per-player settings
    │       │   └── UserSettingsManager.java       # Per-player settings persistence
    │       │
    │       ├── crafting/                         # ── Crafting ──
    │       │   └── WorkbenchRefreshSystem.java    # Fix for workbench recipe refresh on upgrade
    │       │
    │       ├── enchantment/                     # ── Core Enchantment Engine ──
    │       │   ├── EnchantmentType.java           # Enchantment definitions (30 built-in)
    │       │   ├── EnchantmentRegistry.java       # Central registry (built-in + addon)
    │       │   ├── EnchantmentManager.java        # Core logic (apply, read, calculate)
    │       │   ├── EnchantmentData.java           # BSON serialisation for item metadata
    │       │   ├── EnchantmentApplicationResult.java # Result type for apply operations
    │       │   ├── ItemCategory.java              # Item categorisation (weapon, armor, tool…)
    │       │   ├── ItemCategoryManager.java       # Runtime item → category mapping with config
    │       │   ├── EnchantmentEventHelper.java    # Common event utilities
    │       │   ├── EnchantmentRecipeManager.java  # Runtime recipe filtering (disabled scrolls)
    │       │   ├── ScrollItemGenerator.java       # Runtime scroll item generation (~70 items)
    │       │   ├── ScrollDescriptionManager.java  # Scroll description localisation packets
    │       │   ├── BuiltinScrolls.java            # Built-in scroll definitions
    │       │   ├── EnchantmentGlowInjector.java   # Runtime glow injection via ItemAppearance
    │       │   ├── EnchantmentVisualsListener.java # Event-driven visual updates
    │       │   ├── EnchantmentSlotTracker.java    # Per-tick slot tracking for glow + banner
    │       │   ├── EnchantmentDynamicEffects.java # Dynamic EntityEffect adjustments
    │       │   ├── EnchantmentStateTransferSystem.java # Preserves enchantments on item state changes
    │       │   ├── TooltipBridge.java             # Isolated bridge to DynamicTooltipsLib
    │       │   │
    │       │   ├── AbstractRecipeRegistry.java    # Base for smelting/cooking recipe caches
    │       │   ├── SmeltingRecipeRegistry.java    # Smelting recipe lookup (for Smelting enchant)
    │       │   ├── CookingRecipeRegistry.java     # Cooking recipe lookup (for Burn Smelting)
    │       │   ├── AbstractRefundSystem.java      # Base for refund/resource-saving systems
    │       │   │
    │       │   │  # ── ECS Systems (registered with Hytale's Entity Component System) ──
    │       │   ├── EnchantmentDamageSystem.java        # Sharpness, Strength, Eagle's Eye
    │       │   ├── EnchantmentBlockDamageSystem.java    # Efficiency (mining speed)
    │       │   ├── EnchantmentDurabilitySystem.java     # Durability, Sturdy
    │       │   ├── EnchantmentFortuneSystem.java        # Fortune (extra drops)
    │       │   ├── EnchantmentSmeltingSystem.java       # Smelting (auto-smelt)
    │       │   ├── EnchantmentBurnSmeltingSystem.java   # Auto-smelt drops from burn kills
    │       │   ├── EnchantmentSilktouchSystem.java      # Pick Perfect (silk touch)
    │       │   ├── EnchantmentLootingSystem.java        # Looting (bonus mob drops)
    │       │   ├── EnchantmentStaminaSystem.java        # Dexterity (stamina reduction)
    │       │   ├── EnchantmentAbilityStaminaSystem.java # Frenzy (ability charge rate)
    │       │   ├── EnchantmentProjectileSpeedSystem.java # Strength (projectile speed)
    │       │   ├── EnchantmentFeatherFallingSystem.java  # Feather Falling
    │       │   ├── EnchantmentWaterbreathingSystem.java  # Waterbreathing
    │       │   ├── EnchantmentNightVisionSystem.java     # Night Vision
    │       │   ├── EnchantmentBurnSystem.java            # Burn (fire DoT)
    │       │   ├── EnchantmentFreezeSystem.java          # Freeze (slow)
    │       │   ├── EnchantmentPoisonSystem.java          # Poison (DoT)
    │       │   ├── EnchantmentKnockbackSystem.java       # Knockback
    │       │   ├── EnchantmentReflectionSystem.java      # Reflection (damage reflect)
    │       │   ├── EnchantmentAbsorptionSystem.java      # Absorption (heal on block)
    │       │   ├── EnchantmentFastSwimSystem.java        # Swift Swim
    │       │   ├── EnchantmentThriftSystem.java          # Thrift (mana restore)
    │       │   ├── EnchantmentElementalHeartSystem.java  # Elemental Heart (save essence)
    │       │   ├── EnchantmentEternalShotSystem.java     # Eternal Shot (infinite arrows)
    │       │   ├── EternalShotProjectileCleanupSystem.java # Cleanup for Eternal Shot
    │       │   ├── SwitchActiveSlotSystem.java           # Slot switch handler
    │       │   ├── EnchantmentSalvageSystem.java         # Salvager bench metadata strip
    │       │   ├── SalvagerInteractionSystem.java        # Salvager interaction ECS
    │       │   ├── DropItemEventSystem.java              # Manual drop tracking
    │       │   └── ProjectileEnchantmentData.java        # Projectile enchantment cache
    │       │
    │       ├── interaction/                     # ── Custom Interactions ──
    │       │   ├── ConsumeAmmoInteraction.java    # Custom ammo consumption
    │       │   └── LaunchDynamicProjectileInteraction.java # Dynamic projectile launch
    │       │
    │       ├── lang/                            # ── Localisation ──
    │       │   └── LanguageManager.java           # Multi-language string resolution
    │       │
    │       ├── listener/                        # ── Event Listeners ──
    │       │   ├── EventLoggerListener.java       # Debug logging for enchantment events
    │       │   └── WelcomeListener.java           # First-join tooltip notification
    │       │
    │       ├── ui/                              # ── UI Pages & Elements ──
    │       │   ├── EnchantScrollPageSupplier.java        # Scroll application UI codec
    │       │   ├── EnchantScrollPage.java                # Scroll UI page logic
    │       │   ├── EnchantScrollElement.java             # Scroll UI element
    │       │   ├── CleansingScrollPageSupplier.java      # Cleansing scroll UI codec
    │       │   ├── CleansingScrollPage.java              # Cleansing UI page logic
    │       │   ├── CleansingScrollElement.java           # Cleansing UI element
    │       │   ├── CleansingEnchantmentPage.java         # Enchantment selection for cleansing
    │       │   ├── CleansingEnchantmentElement.java      # Per-enchantment cleansing element
    │       │   ├── CustomScrollPageSupplier.java         # Multi-enchant transfer UI codec
    │       │   ├── CustomScrollEnchantmentPage.java      # Enchantment selection page
    │       │   ├── CustomScrollEnchantmentElement.java   # Enchantment element
    │       │   ├── CustomScrollItemPage.java             # Item selection page
    │       │   ├── CustomScrollItemElement.java          # Item element
    │       │   ├── CustomScrollApplyInteraction.java     # Apply interaction for transfers
    │       │   ├── EnchantItemInteraction.java           # Main enchant interaction
    │       │   ├── RemoveEnchantmentInteraction.java     # Remove enchantment interaction
    │       │   ├── EnchantingPage.java                   # Settings/walkthrough page
    │       │   ├── EnchantingPageEventData.java          # Settings page event data
    │       │   ├── EnchantConfigPage.java                # Config editor page
    │       │   └── EnchantConfigPageEventData.java       # Config page event data
    │       │
    │       └── util/                            # ── Utilities ──
    │           ├── ProcessingGuard.java           # Reentrant event guard
    │           └── ScrollIdHelper.java            # Scroll ID parsing utilities
    │
    └── resources/
        ├── manifest.json                        # Plugin manifest (version, deps, main class)
        │
        ├── Common/                              # ── Shared Asset Pack ──
        │   ├── Blocks/Benches/                   # Enchanting Table block model + animation
        │   ├── Icons/                            # UI icons (crafting categories, items)
        │   ├── Items/Scrolls/                    # Scroll item models + textures
        │   └── UI/Custom/                        # Custom UI layouts, textures, and buttons
        │
        └── Server/                              # ── Server-Side Assets ──
            ├── Entity/
            │   ├── Effects/Status/               # Burn, Freeze, Poison entity effects
            │   ├── ModelVFX/                      # Enchantment glow VFX definitions
            │   └── Stats/                        # Glow stat definitions per armor slot
            ├── Item/Items/                       # Enchanting Table + special scroll items
            ├── Languages/                        # Translations (11 locales)
            └── Particles/Enchantment/            # Enchantment particle effects
```

---

## Technical Overview

### Core Plugin

**`SimpleEnchanting`** is the main plugin class extending Hytale's `JavaPlugin`. It orchestrates all system initialisation in its `setup()` method:

1. **Config Migration** — migrates config files from the legacy `config/` directory to `mods/Simple_Enchantments_Config/`.
2. **Config & Settings** — loads server config (`ConfigManager`) and per-player settings (`UserSettingsManager`).
3. **Localisation** — initialises the `LanguageManager` with 11 supported languages.
4. **Runtime Item Generation** — `ScrollItemGenerator` dynamically creates ~70 scroll items at asset-load time, replacing static JSON files.
5. **Recipe Filtering** — `EnchantmentRecipeManager` intercepts asset loading to filter out recipes for disabled enchantments.
6. **Glow Injection** — `EnchantmentGlowInjector` injects `ItemAppearanceConditions` at runtime for mod compatibility.
7. **Custom UI Codecs** — registers `EnchantScroll`, `CleansingScroll`, and `CustomScroll` page types.
8. **Custom Interactions** — registers `ConsumeAmmo` and `LaunchDynamicProjectile` interaction types.
9. **ECS Systems** — registers 20+ ECS systems with Hytale's `EntityStoreRegistry` for enchantment effects.
10. **Event Listeners** — registers global listeners for inventory changes, player join, slot switching, etc.
11. **Commands** — registers `/enchant`, `/enchanting`, `/enchantconfig`, and an enhanced `/give`.
12. **Optional Tooltips** — conditionally registers `TooltipBridge` if DynamicTooltipsLib is present.

---

### Enchantment Engine

The enchantment system is **metadata-based** — enchantment data is stored directly in Hytale's `ItemStack` metadata as BSON documents, avoiding the need for separate item JSON files per enchantment combination.

| Component | Role |
|---|---|
| **`EnchantmentType`** | Defines an enchantment (ID, display name, max level, applicable categories, multiplier, conflicts). Converted from enum to class to support dynamic registration by addon mods. |
| **`EnchantmentRegistry`** | Central registry for all enchantments (built-in + addon). Handles lookup by ID/display name and conflict tracking. |
| **`EnchantmentManager`** | Core logic: applying enchantments, reading from items, calculating multipliers, checking applicability and conflicts. Includes reflection-cached field access for performance. |
| **`EnchantmentData`** | Serialisation layer — converts between in-memory `Map<EnchantmentType, Integer>` and BSON documents. Supports immutable `EMPTY` singleton and stable hashing for caching. |
| **`ItemCategory`** | Categorises items (melee, ranged, armor, tool, shield, staff, etc.) for enchantment applicability. Converted from enum to class for dynamic registration. |
| **`ItemCategoryManager`** | Runtime item-to-category mapping using item families, tags, and config overrides. |

**Key design decisions:**
- Enchantments are stored at the BSON key `"Enchantments"` within item metadata, keyed by display name with integer levels.
- Single-field BSON lookups are used for hot paths (e.g. `getEnchantmentLevel`) to avoid full deserialisation.
- A disabled-enchantment cache (`Set<String>`) provides O(1) enabled/disabled checks.

---

### ECS Systems

Each enchantment effect is implemented as a dedicated **ECS system** registered with Hytale's `EntityStoreRegistry`. Systems hook into the game's entity/component pipeline to modify damage, mining speed, drops, etc.

| System | Enchantment(s) | What It Does |
|---|---|---|
| `EnchantmentDamageSystem` | Sharpness, Strength, Eagle's Eye, Life Leech, Protection, Ranged Protection, Env. Protection | Modifies outgoing and incoming melee/ranged/environmental damage |
| `EnchantmentBlockDamageSystem` | Efficiency | Increases mining/block break speed |
| `EnchantmentDurabilitySystem` | Durability, Sturdy | Reduces/prevents durability loss |
| `EnchantmentFortuneSystem` | Fortune | Extra ore/crystal drops |
| `EnchantmentSmeltingSystem` | Smelting | Auto-smelts mined blocks |
| `EnchantmentBurnSmeltingSystem` | Burn + Smelting | Auto-smelts drops from burn kills |
| `EnchantmentSilktouchSystem` | Pick Perfect | Drops the block itself |
| `EnchantmentLootingSystem` | Looting | Bonus mob drops |
| `EnchantmentStaminaSystem` | Dexterity | Reduces stamina costs |
| `EnchantmentAbilityStaminaSystem` | Frenzy | Increases ability charge rate |
| `EnchantmentProjectileSpeedSystem` | Strength | Increases projectile speed (currently not used) |
| `EnchantmentBurnSystem` | Burn | Fire DoT on hit |
| `EnchantmentFreezeSystem` | Freeze | Slows targets |
| `EnchantmentPoisonSystem` | Poison | Poison DoT on hit |
| `EnchantmentKnockbackSystem` | Knockback | Knocks targets back |
| `EnchantmentReflectionSystem` | Reflection | Reflects damage when blocking |
| `EnchantmentAbsorptionSystem` | Absorption | Heals from blocked damage |
| `EnchantmentFastSwimSystem` | Swift Swim | Increases swim speed |
| `EnchantmentThriftSystem` | Thrift | Restores mana when casting staff abilities |
| `EnchantmentNightVisionSystem` | Night Vision | Enhances dark vision |
| `EnchantmentFeatherFallingSystem` | Feather Falling | Reduces fall damage |
| `EnchantmentWaterbreathingSystem` | Waterbreathing | Reduces oxygen drain |
| `EnchantmentEternalShotSystem` | Eternal Shot | Infinite arrows |
| `EnchantmentElementalHeartSystem` | Elemental Heart | Saves essence ammo |

> **Note:** Riposte and Coup de Grâce are defined as enchantment types but their gameplay logic is handled by the external [Perfect Parries](https://www.curseforge.com/hytale/mods/perfect-parries) mod. They are automatically disabled if that mod is not installed.

Additional support systems:
- **`EnchantmentStateTransferSystem`** — preserves enchantments when items change state (e.g. filling a watering can).
- **`EnchantmentSalvageSystem`** / **`SalvagerInteractionSystem`** — strips enchantment metadata at salvage benches.
- **`EternalShotProjectileCleanupSystem`** — cleans up projectile entities spawned by the Eternal Shot system.
- **`SwitchActiveSlotSystem`** — clears stale Eternal Shot records when switching away from unloaded crossbows.
- **`DropItemEventSystem`** — tracks manual drops to prevent duplication exploits with Eternal Shot / Elemental Heart.
- **`WorkbenchRefreshSystem`** — fixes a vanilla bug where workbench recipes don't rescan after upgrade.

---

### Enchantment API (Public)

The `EnchantmentApi` interface allows **other mods** to interact with the enchantment system without depending on internal classes.

**Capabilities:**
- Add / remove / query enchantments on items
- Register custom enchantments with the fluent `EnchantmentBuilder`
- Register custom item categories (by family or item IDs)
- Register crafting categories (new Enchanting Table tabs)
- Declare enchantment conflicts
- Query all enchantments on a player's equipment
More capabilities soon!
View [Simple Enchantments API](https://github.com/Herolias/Enchantment-API-Example) for a full API reference and usage examples.

---

### UI System

The UI system implements three custom page types registered via Hytale's `OpenCustomUIInteraction.PAGE_CODEC`:

| Page Supplier | Purpose |
|---|---|
| `EnchantScrollPageSupplier` | Enchantment scroll application UI — shows the scroll info and an item slot to apply the enchantment to. |
| `CleansingScrollPageSupplier` | Cleansing scroll UI — lists current enchantments and lets the player pick which to remove. |
| `CustomScrollPageSupplier` | Multi-enchantment transfer scroll — two-step UI for selecting enchantments from a source item and applying them to a target. |

Each page supplier has associated `Page`, `Element`, and interaction classes for the full UI flow. The settings/walkthrough UI (`EnchantingPage`, `EnchantConfigPage`) provides an in-game configuration editor.

---

### Configuration

Configuration is managed by `ConfigManager` and stored in `mods/Simple_Enchantments_Config/simple_enchanting_config.json`.

**Key features:**
- **Unified multiplier map** — all enchantment multipliers stored in a single `enchantmentMultipliers` map keyed by enchantment ID.
- **Legacy migration** — automatic migration from v1.x per-field config to the unified map.
- **Smart snapshots** — `SmartConfigManager` maintains `.snapshot` files to detect external edits.
- **Per-player settings** — `UserSettingsManager` stores per-player language preferences.
- **Configurable recipes** — scroll recipes, Enchanting Table recipe, and table upgrade recipes are all configurable.
- **Per-enchantment toggles** — each enchantment can be enabled/disabled individually.

---

### Localisation

`LanguageManager` loads translation files from `Server/Languages/{locale}/server.lang` and supports 11 locales:

`en-US` · `de-DE` · `es-ES` · `fr-FR` · `id-ID` · `it-IT` · `nl-NL` · `pt-BR` · `ru-RU` · `sv-SE` · `uk-UA`

Translation keys follow the pattern `enchantment.{id}.{name|description|bonus|walkthrough}`. Locale-specific packets are sent to players on join.

---

### Commands

| Command | Description |
|---|---|
| `/enchant <enchantment> [level]` | Apply an enchantment to the held item |
| `/enchanting` | Open the enchanting settings/walkthrough UI |
| `/enchantconfig` | Open the in-game configuration editor |
| `/give <player> <item> [amount] [enchantments...]` | Enhanced give command that supports pre-enchanted items |

---

### Custom Interactions

Two custom interaction types are registered with Hytale's interaction codec:

- **`ConsumeAmmoInteraction`** — controls ammo consumption for ranged weapons, integrating with the Eternal Shot enchantment.
- **`LaunchDynamicProjectileInteraction`** — launches projectiles with dynamically modified speed/range from enchantments.

---

### Crafting & Recipes

- **Scroll crafting** — scrolls are crafted at the Enchanting Table, which has tiered upgrades (4 tiers). Each scroll recipe specifies ingredients and the tier required to unlock it.
- **Runtime generation** — `ScrollItemGenerator` creates all ~70 scroll items dynamically at asset-load time from `EnchantmentType` definitions, eliminating the need for individual JSON files.
- **Recipe filtering** — `EnchantmentRecipeManager` intercepts the asset loading pipeline and removes recipes for enchantments that are disabled in the config.
- **Salvaging** — the `EnchantmentSalvageSystem` integrates with the Salvager bench by stripping enchantment metadata from items before salvaging.

---

### Asset Pack

The mod bundles a complete asset pack containing:

- **Block models** — Enchanting Table model, texture, and crafting animation
- **Item models** — Scroll and Cleansing Scroll 3D models and textures
- **UI assets** — Custom scroll-themed UI layouts, buttons, and backgrounds
- **Entity effects** — Burn, Freeze, and Poison status effect definitions
- **VFX** — Enchantment glow model VFX definitions for each equipment slot
- **Particles** — Enchantment particle effects
- **Icons** — Crafting category icons and generated item icons

---

### Optional Integrations

| Mod | Integration |
|---|---|
| **[DynamicTooltipsLib](https://github.com/Herolias/DynamicTooltipsLib)** | Provides enchantment tooltips on item hover. Loaded via `TooltipBridge` (class isolated to prevent `NoClassDefFoundError`). When detected, the enchantment banner is auto-disabled. Note: This mod will become a full dependency in version 1.0.0. |
| **Perfect Parries** | Enables the Riposte and Coup de Grâce enchantments (counter-attack and stun bonus damage). These enchantments are automatically disabled if the mod is not present. |
| **MMO Skill Tree** | Adds Enchantment XP with unique rewards |
| **[HStats](https://hstats.dev)** | Anonymous mod usage analytics. |

---

## Contributing

### Contribution Guidelines
Please do not add new features or enchantments without discussing it with the team. Generally, this is a passionate hobby project for Sorath and me, and we want to keep it that way.

But we are absolutely open to smaller contributions like bug fixes, performance improvements, and translations.
Please open a pull request for that or write me up on [Discord](https://discord.com/users/herolias).
Pull requests should be made to the `dev` branch. It has the latest changes, please check if your changes are already implemented there before working on a PR. 

### Contributors
Huge thanks to Thanoz, Samu3k, and Ensō for helping improving the translations!

Huge thanks to Dimotai for helping to improve the `EnchantmentDynamicEffects` class!

---

## Authors

- **MineAndCraft (Herolias)** — Developer
- **Sorath** — Artist
