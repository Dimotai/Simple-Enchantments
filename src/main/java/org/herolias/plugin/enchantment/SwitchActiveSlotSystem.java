package org.herolias.plugin.enchantment;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.event.events.ecs.SwitchActiveSlotEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * System to handle SwitchActiveSlotEvent and delegate to EnchantmentEternalShotSystem.
 * Required because EventRegistry doesn't support passing EntityStore context to listeners,
 * but the logic needs to access Player and EntityStatMap components.
 */
public class SwitchActiveSlotSystem extends EntityEventSystem<EntityStore, SwitchActiveSlotEvent> {

    private final EnchantmentEternalShotSystem eternalShotSystem;
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public SwitchActiveSlotSystem(EnchantmentEternalShotSystem eternalShotSystem) {
        super(SwitchActiveSlotEvent.class);
        this.eternalShotSystem = eternalShotSystem;
    }

    @Override
    @Nonnull
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }

    @Override
    public void handle(int index,
                       @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull SwitchActiveSlotEvent event) {
        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        LOGGER.atInfo().log("[DEBUG] SwitchActiveSlotEvent fired!");
        eternalShotSystem.onSwitchActiveSlot(event, ref, store);
    }
}
