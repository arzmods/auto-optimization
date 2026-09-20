package com.arzmods.potatopvp.client;

import com.arzmods.potatopvp.PotatoConfig;
import com.arzmods.potatopvp.QualityLevel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * The screen you get by pressing Z.
 *
 * <p>Three buttons, one per setting, each cycling None / Minimum / Medium.
 * Nothing is applied until the screen is closed, so you can flick through the
 * options without triggering a resource reload on every click.
 */
public class PotatoOptionsScreen extends Screen {

    private static final int ROW_WIDTH = 210;
    private static final int ROW_HEIGHT = 20;
    private static final int ROW_SPACING = 24;

    private final Screen parent;

    /** Remembered so we only pay for a resource reload when the textures setting really moved. */
    private QualityLevel texturesOnOpen = PotatoConfig.textures();

    public PotatoOptionsScreen(Screen parent) {
        super(Component.translatable("potatopvp.screen.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.texturesOnOpen = PotatoConfig.textures();

        int left = this.width / 2 - ROW_WIDTH / 2;
        int y = this.height / 4 + 12;

        this.addRenderableWidget(levelButton("potatopvp.option.particles", left, y,
                PotatoConfig.particles(), PotatoConfig::setParticles));
        y += ROW_SPACING;

        this.addRenderableWidget(levelButton("potatopvp.option.textures", left, y,
                PotatoConfig.textures(), PotatoConfig::setTextures));
        y += ROW_SPACING;

        this.addRenderableWidget(levelButton("potatopvp.option.animations", left, y,
                PotatoConfig.animations(), PotatoConfig::setAnimations));
        y += ROW_SPACING + 10;

        this.addRenderableWidget(Button.builder(
                        Component.translatable("potatopvp.button.preset"),
                        button -> {
                            PotatoConfig.resetToPotatoPreset();
                            this.rebuildWidgets();
                        })
                .bounds(left, y, ROW_WIDTH, ROW_HEIGHT)
                .build());
        y += ROW_SPACING;

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .bounds(left, y, ROW_WIDTH, ROW_HEIGHT)
                .build());
    }

    /** One row: a label plus a value that cycles through the three levels. */
    private CycleButton<QualityLevel> levelButton(String translationKey, int x, int y,
                                                  QualityLevel initial, Consumer<QualityLevel> setter) {
        return CycleButton.<QualityLevel>builder(level -> Component.translatable(level.getTranslationKey()))
                .withValues(QualityLevel.values())
                .withInitialValue(initial)
                .create(x, y, ROW_WIDTH, ROW_HEIGHT,
                        Component.translatable(translationKey),
                        (button, value) -> setter.accept(value));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 18, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font,
                Component.translatable("potatopvp.screen.subtitle"), this.width / 2, 32, 0xA0A0A0);
    }

    @Override
    public void onClose() {
        PotatoConfig.save();
        PotatoOptions.applyAll();

        if (this.minecraft != null) {
            // Flattening happens while textures are decoded, so a changed
            // Textures setting only shows up after the resources are re-read.
            if (PotatoConfig.textures() != this.texturesOnOpen) {
                this.minecraft.reloadResourcePacks();
            }
            this.minecraft.setScreenAndShow(this.parent);
        }
    }

    /** Never freeze the game behind the menu - you might be in a fight. */
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
