package org.herolias.plugin.ui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.entities.Player;
import org.herolias.plugin.config.UserSettingsManager;

import javax.annotation.Nonnull;

/**
 * Interactive UI page corresponding to the /enchanting command.
 * Provides a Walkthrough and User Settings (Glow, Banner toggles).
 */
public class EnchantingPage extends InteractiveCustomUIPage<EnchantingPageEventData> {

    private static final Value<String> BUTTON_STYLE = Value.ref("Pages/BasicTextButton.ui", "LabelStyle");
    private static final Value<String> BUTTON_STYLE_SELECTED = Value.ref("Pages/BasicTextButton.ui", "SelectedLabelStyle");

    private static final String TAB_WALKTHROUGH = "walkthrough";
    private static final String TAB_SETTINGS = "settings";

    private final UserSettingsManager userSettingsManager;
    private final PlayerRef playerRef;
    private String currentTab = TAB_WALKTHROUGH;

    public EnchantingPage(@Nonnull PlayerRef playerRef, @Nonnull UserSettingsManager userSettingsManager) {
        super(playerRef, CustomPageLifetime.CanDismiss, EnchantingPageEventData.CODEC);
        this.playerRef = playerRef;
        this.userSettingsManager = userSettingsManager;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commandBuilder,
                      @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store) {
        
        commandBuilder.append("Pages/EnchantingPage.ui");

        // Tab switches
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#TabWalkthrough",
            EventData.of("TabSwitch", TAB_WALKTHROUGH));
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#TabSettings",
            EventData.of("TabSwitch", TAB_SETTINGS));

        // Action button
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton",
            EventData.of("Close", "true"));

        buildTabContent(commandBuilder, eventBuilder);
        updateTabStyles(commandBuilder);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                                @Nonnull EnchantingPageEventData data) {
        UICommandBuilder commandBuilder = new UICommandBuilder();
        UIEventBuilder eventBuilder = new UIEventBuilder();

        if (data.tabSwitch != null) {
            this.currentTab = data.tabSwitch;
            buildTabContent(commandBuilder, eventBuilder);
            updateTabStyles(commandBuilder);
            this.sendUpdate(commandBuilder, eventBuilder, false);
        } else if (data.toggleSetting != null) {
            if ("glow".equals(data.toggleSetting)) {
                boolean current = userSettingsManager.getEnableEnchantmentGlow(this.playerRef.getUuid());
                userSettingsManager.setEnableEnchantmentGlow(this.playerRef.getUuid(), !current);
            } else if ("banner".equals(data.toggleSetting)) {
                boolean current = userSettingsManager.getShowEnchantmentBanner(this.playerRef.getUuid());
                userSettingsManager.setShowEnchantmentBanner(this.playerRef.getUuid(), !current);
            }
            
            buildTabContent(commandBuilder, eventBuilder);
            this.sendUpdate(commandBuilder, eventBuilder, false);
        } else if ("true".equals(data.close)) {
            closeWithoutSaving(ref, store);
        }
    }

    private void buildTabContent(@Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder) {
        commandBuilder.clear("#ContentArea");

        switch (currentTab) {
            case TAB_WALKTHROUGH -> buildWalkthroughTab(commandBuilder, eventBuilder);
            case TAB_SETTINGS -> buildSettingsTab(commandBuilder, eventBuilder);
        }
    }

    private void updateTabStyles(@Nonnull UICommandBuilder commandBuilder) {
        commandBuilder.set("#TabWalkthrough.Style", TAB_WALKTHROUGH.equals(currentTab) ? BUTTON_STYLE_SELECTED : BUTTON_STYLE);
        commandBuilder.set("#TabSettings.Style", TAB_SETTINGS.equals(currentTab) ? BUTTON_STYLE_SELECTED : BUTTON_STYLE);
    }

    private void buildWalkthroughTab(@Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder) {
        commandBuilder.append("#ContentArea", "Pages/EnchantingWalkthrough.ui");
    }

    private void buildSettingsTab(@Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder) {
        boolean glowEnabled = userSettingsManager.getEnableEnchantmentGlow(this.playerRef.getUuid());
        boolean bannerEnabled = userSettingsManager.getShowEnchantmentBanner(this.playerRef.getUuid());

        int index = 0;

        // Setup Glow Toggle
        commandBuilder.append("#ContentArea", "Pages/EnchantingSettings.ui");
        commandBuilder.set("#ContentArea[" + index + "] #SettingName.TextSpans", Message.translation("server.config.general.enable_glow"));
        commandBuilder.set("#ContentArea[" + index + "] #ToggleButton.TextSpans",
            Message.translation(glowEnabled ? "server.config.common.enabled" : "server.config.common.disabled"));
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#ContentArea[" + index + "] #ToggleButton",
            EventData.of("ToggleSetting", "glow"));
        index++;

        // Setup Banner Toggle
        commandBuilder.append("#ContentArea", "Pages/EnchantingSettings.ui");
        commandBuilder.set("#ContentArea[" + index + "] #SettingName.TextSpans", Message.translation("server.config.general.show_banner"));
        commandBuilder.set("#ContentArea[" + index + "] #ToggleButton.TextSpans",
            Message.translation(bannerEnabled ? "server.config.common.enabled" : "server.config.common.disabled"));
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#ContentArea[" + index + "] #ToggleButton",
            EventData.of("ToggleSetting", "banner"));
    }

    private void closeWithoutSaving(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        Player playerComponent = store.getComponent(ref, Player.getComponentType());
        if (playerComponent != null) {
            playerComponent.getPageManager().setPage(ref, store, Page.None);
        }
    }
}
