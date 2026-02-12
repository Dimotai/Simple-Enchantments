package org.herolias.plugin.enchantment;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.LivingEntity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.entity.LivingEntityInventoryChangeEvent;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.Transaction;
import com.hypixel.hytale.server.core.inventory.transaction.SlotTransaction;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackSlotTransaction;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import org.herolias.plugin.util.ProcessingGuard;

import com.hypixel.hytale.server.core.event.events.ecs.SwitchActiveSlotEvent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Eternal Shot Enchantment System
 * 
 * Intercepts arrow/ammo consumption when shooting bows/crossbows and restores
 * the ammo if the weapon has the Eternal Shot enchantment.
 * 
 * Also prevents arrow duplication when swapping away from loaded crossbows
 * (both vanilla and modded) by tracking recent refunds and cancelling
 * swap-from ammo returns.
 * 
 * Tracks manual drops to avoid incorrectly refunding dropped arrows.
 */
@SuppressWarnings("removal")
public class EnchantmentEternalShotSystem extends AbstractRefundSystem {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final EnchantmentManager enchantmentManager;
    private final ProcessingGuard guard = new ProcessingGuard();

    /**
     * Tracks currently loaded crossbows to prevent duplication on swap-from.
     * We don't need a time window anymore; we just need to know if we've refunded
     * an arrow for a specific item, effectively marking it as "in a loaded state"
     * from our system's perspective.
     * 
     * Key: Player UUID, Value: Item ID of the arrow that was refunded.
     */
    private final Map<UUID, String> loadedCrossbowAmmo = new ConcurrentHashMap<>();

    public EnchantmentEternalShotSystem(EnchantmentManager enchantmentManager) {
        this.enchantmentManager = enchantmentManager;
        LOGGER.atInfo().log("EnchantmentEternalShotSystem initialized");
    }

    public void onInventoryChange(@Nonnull LivingEntityInventoryChangeEvent event) {
        if (guard.isProcessing()) return;
        
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Player player)) return;
        
        Transaction transaction = event.getTransaction();
        ItemContainer container = event.getItemContainer();
        
        cleanupOldDropRecords(player);
        
        if (transaction instanceof ItemStackTransaction itemStackTransaction) {
            // DEBUG: Log the entire transaction structure
            if (loadedCrossbowAmmo.containsKey(player.getUuid())) {
                LOGGER.atInfo().log("[DEBUG] Transaction for " + player.getUuid() + " (" + itemStackTransaction.getSlotTransactions().size() + " ops)");
                for (ItemStackSlotTransaction slotTx : itemStackTransaction.getSlotTransactions()) {
                    LOGGER.atInfo().log("   - Slot " + slotTx.getSlot() + ": " 
                        + (slotTx.getSlotBefore() != null ? slotTx.getSlotBefore().getItemId() : "null") + " -> "
                        + (slotTx.getSlotAfter() != null ? slotTx.getSlotAfter().getItemId() : "null"));
                }
            }

            for (ItemStackSlotTransaction slotTx : itemStackTransaction.getSlotTransactions()) {
                // Pass the full transaction context to allow checking other slots
                processSlotTransaction(player, container, slotTx, itemStackTransaction);
            }
        } else if (transaction instanceof SlotTransaction slotTransaction) {
            processSlotTransaction(player, container, slotTransaction, null);
        }
    }
    
    /**
     * Listens for slot switches to clear the "Loaded" record if the player
     * switches away from a crossbow that is actually unloaded (Ammo == 0).
     * This handles the "Fire -> Swap" case where the record would otherwise become stale.
     * 
     * Note: This method signature matches ComponentSystem.process() for SwitchActiveSlotEvent.
     */
    public void onSwitchActiveSlot(SwitchActiveSlotEvent event, Ref<EntityStore> ref, Store<EntityStore> store) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return;

        // Optimization: Only check if we are tracking a load for this player
        if (!loadedCrossbowAmmo.containsKey(player.getUuid())) return;

        EntityStatMap stats = store.getComponent(ref, EntityStatMap.getComponentType());
        if (stats != null) {
            EntityStatValue ammoStat = stats.get(DefaultEntityStatTypes.getAmmo());
            float ammoValue = (ammoStat != null) ? ammoStat.get() : 0f;
            
            LOGGER.atInfo().log("[DEBUG] SwitchSlot for " + player.getUuid() + " | Ammo Stat: " + ammoValue);
            
            // If ammo is depleted (<= 0), then the crossbow is unloaded.
            // If we still have a record, it's stale (from a previous shot). Clear it.
            if (ammoValue < 1.0f) {
                 loadedCrossbowAmmo.remove(player.getUuid());
                 LOGGER.atInfo().log("[DEBUG] Cleared status for " + player.getUuid() + " (Ammo depleted)");
            } else {
                 LOGGER.atInfo().log("[DEBUG] Kept status for " + player.getUuid() + " (Ammo present)");
            }
        } else {
             LOGGER.atInfo().log("[DEBUG] SwitchSlot: No stats found needed to check ammo.");
        }
    }
    
    private void processSlotTransaction(Player player, ItemContainer container, SlotTransaction slotTransaction, @Nullable ItemStackTransaction parentTransaction) {
        if (!slotTransaction.succeeded()) return;
        
        ItemStack slotBefore = slotTransaction.getSlotBefore();
        ItemStack slotAfter = slotTransaction.getSlotAfter();
        
        int beforeQty = (slotBefore == null || slotBefore.isEmpty()) ? 0 : slotBefore.getQuantity();
        int afterQty = (slotAfter == null || slotAfter.isEmpty()) ? 0 : slotAfter.getQuantity();
        short slot = slotTransaction.getSlot();

        if (beforeQty > afterQty) {
            // Ammo was CONSUMED (quantity decreased) - handle refund for Eternal Shot
            processAmmoConsumption(player, container, slotTransaction, slotBefore, slotAfter, beforeQty, afterQty, slot);
        } else if (afterQty > beforeQty) {
            // Ammo was ADDED (quantity increased) - check for swap-from duplication
            processAmmoAddition(player, container, slotTransaction, slotBefore, slotAfter, beforeQty, afterQty, slot, parentTransaction);
        }
    }

    /**
     * Handles ammo consumption: if the player holds a ranged weapon with Eternal Shot,
     * refund the consumed ammo and record the refund for duplication prevention.
     */
    private void processAmmoConsumption(Player player, ItemContainer container, SlotTransaction slotTransaction,
                                         ItemStack slotBefore, ItemStack slotAfter,
                                         int beforeQty, int afterQty, short slot) {
        if (slotBefore == null || slotBefore.isEmpty()) return;
        if (!isAmmoItem(slotBefore)) return;
        
        if (wasRecentlyDropped(player, slot)) return;  // Manual drop, don't refund
        
        Inventory inventory = player.getInventory();
        if (inventory == null) return;
        
        ItemStack weapon = inventory.getItemInHand();
        if (weapon == null || weapon.isEmpty()) return;
        
        ItemCategory category = enchantmentManager.categorizeItem(weapon);
        if (category != ItemCategory.RANGED_WEAPON) return;
        
        int level = enchantmentManager.getEnchantmentLevel(weapon, EnchantmentType.ETERNAL_SHOT);
        if (level <= 0) return;

        // Refund the consumed ammo
        guard.runGuarded(() -> container.replaceItemStackInSlot(slot, slotAfter, slotBefore));

        // Track this refund for crossbow swap-from duplication prevention.
        if (enchantmentManager.isCrossbow(weapon)) {
            LOGGER.atInfo().log("[DEBUG] Recording load for " + player.getUuid() + " | Item: " + slotBefore.getItemId());
            loadedCrossbowAmmo.put(player.getUuid(), slotBefore.getItemId());
        }
    }

    /**
     * Handles ammo being added back to inventory: detects swap-from duplication
     * on modded crossbows and cancels the addition by consuming the returned arrows.
     */
    private void processAmmoAddition(Player player, ItemContainer container, SlotTransaction slotTransaction,
                                      ItemStack slotBefore, ItemStack slotAfter,
                                      int beforeQty, int afterQty, short slot,
                                      @Nullable ItemStackTransaction parentTransaction) {
        if (slotAfter == null || slotAfter.isEmpty()) return;
        if (!isAmmoItem(slotAfter)) return;

        UUID playerUuid = player.getUuid();
        String refundedAmmoId = loadedCrossbowAmmo.get(playerUuid);
        
        // If we haven't recorded a refund (load) for this player, ignore.
        if (refundedAmmoId == null) return;

        // Check if the added ammo matches what we refunded/loaded
        if (!refundedAmmoId.equals(slotAfter.getItemId())) return;

        Inventory inventory = player.getInventory();
        if (inventory == null) return;

        ItemStack currentWeapon = inventory.getItemInHand();
        
        boolean stillHoldingEternalCrossbow = currentWeapon != null && !currentWeapon.isEmpty()
            && enchantmentManager.isCrossbow(currentWeapon)
            && enchantmentManager.getEnchantmentLevel(currentWeapon, EnchantmentType.ETERNAL_SHOT) > 0;

        // CRITICAL CHECK:
        // Use the transaction context to distinguish between a "Swap/Unload Refund" and a "Legitimate Pickup".
        // 1. Swap/Unload Refund: The Crossbow slot is modified (Loaded -> Unloaded) IN THE SAME transaction as the Arrow Add.
        // 2. Legitimate Pickup: Only the Arrow slot is modified (Amount increases). The Crossbow slot is untouched.
        
        boolean isCrossbowBeingModified = false;
        
        if (parentTransaction != null) {
            for (ItemStackSlotTransaction tx : parentTransaction.getSlotTransactions()) {
                 ItemStack sBefore = tx.getSlotBefore();
                 ItemStack sAfter = tx.getSlotAfter();
                 
                 // Force log for debugging
                 if (sBefore != null && sAfter != null && sBefore.equals(sAfter)) {
                     LOGGER.atInfo().log("[DEBUG] Identical Stacks detected. Logging diff anyway.");
                 }

                 // Check if this transaction involves the Eternal Shot crossbow (and isn't the arrow itself)
                 if (sBefore != null && !sBefore.isEmpty() && enchantmentManager.isCrossbow(sBefore) 
                     && enchantmentManager.getEnchantmentLevel(sBefore, EnchantmentType.ETERNAL_SHOT) > 0) {
                     // We found the crossbow in the transaction! This means it's changing state.
                     // A pure pickup wouldn't modify the crossbow.
                     isCrossbowBeingModified = true;
                     LOGGER.atInfo().log("[DEBUG] Detected Crossbow modification in slot " + tx.getSlot() + " - Assuming Swap/Unload event.");
                     LOGGER.atInfo().log("[DEBUG] Crossbow Diff: Before=" + sBefore + " After=" + sAfter);
                     break;
                 }
            }
        }

        if (stillHoldingEternalCrossbow && !isCrossbowBeingModified) {
            // Player is holding the crossbow, and the crossbow itself is NOT being modified in this transaction.
            // This is a legitimate pickup.
            LOGGER.atInfo().log("[DEBUG] Allowed legitimate pickup of " + slotAfter.getItemId() + " (Crossbow not modified)");
            return;
        }

        // We are either:
        // A) Not holding the crossbow anymore (swapped to something else)
        // B) Holding it, but it is being modified (Unloading due to swap logic or fire logic logic?)
        //    - Actually, firing logic doesn't add arrows. Only Swap/Unload logic adds arrows.
        //    - So if Crossbow matches AND Arrow matches refund ID -> It is the Swap Refund.
        
        int addedQty = afterQty - beforeQty;
        LOGGER.atInfo().log("[DEBUG] Cancelling swap-from arrow duplication for " + player.getUuid() + 
            " (" + addedQty + "x " + slotAfter.getItemId() + ")");
        
        // Restore the slot to its state before the arrows were added back
        guard.runGuarded(() -> container.replaceItemStackInSlot(slot, slotAfter, slotBefore));

        // Clear the record since we've handled the unload.
        loadedCrossbowAmmo.remove(playerUuid);
    }

    /**
     * Checks if an item is ammunition.
     */
    private boolean isAmmoItem(@Nonnull ItemStack itemStack) {
        String itemId = itemStack.getItemId();
        if (itemId == null) return false;

        String lower = itemId.toLowerCase();

        // Direct name matching
        if (lower.contains("arrow") || lower.contains("bolt") || lower.contains("ammo") || lower.contains("ammunition")) {
            return true;
        }

        // Check item tags
        try {
            Item item = itemStack.getItem();
            if (item != null && item.getData() != null) {
                String[] typeValues = item.getData().getRawTags().get("Type");
                if (typeValues != null) {
                    for (String tag : typeValues) {
                        String tagLower = tag.toLowerCase();
                        if (tagLower.contains("arrow") || tagLower.contains("bolt") || tagLower.contains("ammo") || tagLower.contains("ammunition") || tagLower.contains("projectile")) {
                            return true;
                        }
                    }
                }
                String[] familyValues = item.getData().getRawTags().get("Family");
                if (familyValues != null) {
                    for (String tag : familyValues) {
                        String tagLower = tag.toLowerCase();
                        if (tagLower.contains("arrow") || tagLower.contains("ammo") || tagLower.contains("ammunition") || tagLower.contains("projectile")) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Fallback
        }

        return false;
    }
}
