package dev.tuff;

import dev.tuff.module.Module;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class TuffScreen extends Screen {
    public TuffScreen() { super(Component.literal("TuffClient")); }

    private static Component label(Module m) {
        boolean on = m.isEnabled();
        return Component.literal(m.name + ": ")
                .append(Component.literal(on ? "ON" : "OFF").withColor(on ? 0x55FF55 : 0xFF5555));
    }

    @Override
    protected void init() {
        int y = 45;
        for (Module m : TuffClient.MODULES) {
            boolean hasSettings = m.createSettingsScreen(this) != null;
            int w = hasSettings ? 154 : 180;
            addRenderableWidget(Button.builder(label(m), b -> {
                m.toggle();
                b.setMessage(label(m));
            }).bounds(width / 2 - 90, y, w, 20).build());
            if (hasSettings) {
                addRenderableWidget(Button.builder(Component.literal("⚙"),
                        b -> minecraft.gui.setScreen(m.createSettingsScreen(this)))
                        .bounds(width / 2 + 68, y, 22, 20).build());
            }
            y += 24;
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        String title = "TuffClient";
        graphics.text(font, title, width / 2 - font.width(title) / 2, 20, 0xFFFFAA00, true);
    }
}
