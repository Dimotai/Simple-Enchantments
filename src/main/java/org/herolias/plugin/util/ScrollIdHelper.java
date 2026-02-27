package org.herolias.plugin.util;

import org.herolias.plugin.enchantment.EnchantmentType;

import java.util.Map;

/**
 * Utility for constructing scroll item IDs from enchantment type and level.
 * Shared by EnchantmentSalvageSystem and RemoveEnchantmentInteraction.
 */
public final class ScrollIdHelper {

    private ScrollIdHelper() {
        // Utility class — no instantiation
    }

    /**
     * Builds the scroll item ID for a given enchantment type and level.
     * Delegates to {@link EnchantmentType#getScrollBaseName()} to correctly
     * resolve custom scroll names and namespace stripping for addons.
     *
     * @param type  the enchantment type
     * @param level the enchantment level (1-based)
     * @return the scroll item ID string
     */
    public static String getScrollItemId(EnchantmentType type, int level) {
        return type.getScrollBaseName() + "_" + EnchantmentType.toRoman(level);
    }
}
