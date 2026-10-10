package dev.tuff;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Hex color editor for each Storage ESP block type, with a live preview swatch. */
public class ColorScreen extends Screen {
    private final Screen parent;
    private final List<String> keys = new ArrayList<>(Config.DEFAULTS.keySet());
    private final List<EditBox> fields = new ArrayList<>();

    public ColorScreen(Screen parent) {
        super(Component.literal("Storage ESP Colors"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        fields.clear();
        int y = 50;
        int fx = width / 2 + 5;
        for (String key : keys) {
            EditBox f = new EditBox(font, fx, y, 70, 20, Component.literal(key));
            f.setMaxLength(7);
            f.setValue(Config.hex(key));
            f.setResponder(s -> {
                int rgb = Config.parseHex(s);
                if (rgb >= 0) Config.set(key, rgb);
            });
            addRenderableWidget(f);
            fields.add(f);
            y += 26;
        }
        addRenderableWidget(Button.builder(Component.literal("Reset to defaults"), b -> {
            Config.resetColors();
            for (int i = 0; i < keys.size(); i++) fields.get(i).setValue(Config.hex(keys.get(i)));
        }).bounds(width / 2 - 100, y + 8, 98, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(width / 2 + 2, y + 8, 98, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        String title = "Storage ESP Colors";
        String sub = "Hex format: #RRGGBB";
        g.text(font, title, width / 2 - font.width(title) / 2, 20, 0xFFFFAA00, true);
        g.text(font, sub, width / 2 - font.width(sub) / 2, 33, 0xFFAAAAAA, false);
        int y = 50;
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            g.text(font, Config.LABELS.get(key), width / 2 - 110, y + 6, 0xFFFFFFFF, true);
            int sx = width / 2 + 80;
            g.fill(sx - 1, y - 1, sx + 21, y + 21, 0xFFFFFFFF);
            g.fill(sx, y, sx + 20, y + 20, Config.get(key));
            boolean bad = Config.parseHex(fields.get(i).getValue()) < 0;
            fields.get(i).setTextColor(bad ? 0xFFFF5555 : 0xFFE0E0E0);
            y += 26;
        }
    }

    @Override
    public void onClose() { minecraft.gui.setScreen(parent); }
}
