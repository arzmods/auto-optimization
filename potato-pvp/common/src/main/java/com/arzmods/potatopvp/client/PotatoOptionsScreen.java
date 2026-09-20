package com.arzmods.potatopvp.client;

import com.arzmods.potatopvp.PotatoConfig;
import com.arzmods.potatopvp.QualityLevel;
import net.minecraft.client.Minecraft;
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
 *
 * <p>There is no drawn title. Minecraft 26.3 replaced the old
 * render(GuiGraphics, ...) method with a render-state extraction model, and a
 * decorative heading is not worth reaching into that for - every button is
 * labelled with the setting it controls, so the screen reads fine without one.
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
                            // Reopen so every button picks up its new value.
                            Minecraft.getInstance().setScreenAndShow(new PotatoOptionsScreen(this.parent));
                        })
                .bounds(left, y, ROW_WIDTH, ROW_HEIGHT)
                .build());
        y += ROW_SPACING;

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .bounds(left, y, ROW_WIDTH, ROW_HEIGHT)
                .build());
    }

    /**
     * One row: a label plus a value that cycles through the three levels.
     *
     * <p>The starting value is the second argument to builder() now; the old
     * withInitialValue() step is gone.
     */
    private CycleButton<QualityLevel> levelButton(String translationKey, int x, int y,
                                                  QualityLevel initial, Consumer<QualityLevel> setter) {
        return CycleButton.<QualityLevel>builder(
                        level -> Component.translatable(level.getTranslationKey()), initial)
                .withValues(QualityLevel.values())
                .create(x, y, ROW_WIDTH, ROW_HEIGHT,
                        Component.translatable(translationKey),
                        (button, value) -> setter.accept(value));
    }

    @Override
    public void onClose() {
        PotatoConfig.save();
        PotatoOptions.applyAll();

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null) {
            // Flattening happens while textures are decoded, so a changed
            // Textures setting only shows up after the resources are re-read.
            if (PotatoConfig.textures() != this.texturesOnOpen) {
                minecraft.reloadResourcePacks();
            }
            minecraft.setScreenAndShow(this.parent);
        }
    }

    /** Never freeze the game behind the menu - you might be in a fight. */
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
