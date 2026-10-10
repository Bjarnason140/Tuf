package dev.tuff;

import com.mojang.blaze3d.platform.InputConstants;
import dev.tuff.module.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public class TuffClient implements ClientModInitializer {
    public static final List<Module> MODULES = new ArrayList<>();
    public static final StorageEsp STORAGE_ESP = new StorageEsp();
    public static final Tracers TRACERS = new Tracers();
    public static final Freecam FREECAM = new Freecam();
    public static final BaseFinder BASE_FINDER = new BaseFinder();

    private static KeyMapping menuKey, freecamKey;

    @Override
    public void onInitializeClient() {
        RenderUtil.init(); // registers the through-walls pipeline up front

        MODULES.add(STORAGE_ESP);
        MODULES.add(TRACERS);
        MODULES.add(FREECAM);
        MODULES.add(BASE_FINDER);

        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("tuffclient", "main"));
        menuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.tuffclient.menu", InputConstants.Type.KEYSYM, InputConstants.KEY_RSHIFT, category));
        freecamKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.tuffclient.freecam", InputConstants.Type.KEYSYM, InputConstants.KEY_F6, category));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (menuKey.consumeClick()) mc.gui.setScreen(new TuffScreen());
            while (freecamKey.consumeClick()) FREECAM.toggle();
            if (mc.level == null || mc.player == null) return;
            for (Module m : MODULES) if (m.isEnabled()) m.onTick(mc);
        });

        // 26.x splits rendering: modules add lines during EXTRACTION, then we draw them.
        LevelExtractionEvents.END_EXTRACTION.register(ctx -> {
            RenderUtil.beginExtract();
            if (net.minecraft.client.Minecraft.getInstance().level != null) {
                for (Module m : MODULES) if (m.isEnabled()) m.onExtract();
            }
            RenderUtil.endExtract();
        });
        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(RenderUtil::draw);

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            if (FREECAM.isEnabled()) FREECAM.toggle();
            BASE_FINDER.reset();
        });
    }
}
