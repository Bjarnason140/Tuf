package dev.tuff.module;

import dev.tuff.ColorScreen;
import dev.tuff.Config;
import dev.tuff.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/** Outlines chests, hoppers, droppers, dispensers and spawners through walls. Colors are configurable. */
public class StorageEsp extends Module {
    private record Entry(AABB box, String type) {}

    private final List<Entry> cache = new ArrayList<>();
    private int timer;

    public StorageEsp() { super("Storage ESP"); }

    @Override protected void onEnable() { timer = 0; }

    @Override
    public Screen createSettingsScreen(Screen parent) { return new ColorScreen(parent); }

    private static String typeFor(BlockEntity be) {
        if (be instanceof TrappedChestBlockEntity) return "trapped_chest";
        if (be instanceof ChestBlockEntity)        return "chest";
        if (be instanceof HopperBlockEntity)       return "hopper";
        if (be instanceof DropperBlockEntity)      return "dropper"; // before dispenser: Dropper extends Dispenser
        if (be instanceof DispenserBlockEntity)    return "dispenser";
        if (be instanceof SpawnerBlockEntity)      return "spawner";
        return null;
    }

    @Override
    public void onTick(Minecraft mc) {
        if (timer-- > 0) return;
        timer = 10;
        cache.clear();
        int pcx = mc.player.blockPosition().getX() >> 4;
        int pcz = mc.player.blockPosition().getZ() >> 4;
        int r = Math.min(mc.options.renderDistance().get(), 12);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                LevelChunk ch = mc.level.getChunkSource().getChunkNow(pcx + dx, pcz + dz);
                if (ch == null) continue;
                for (BlockEntity be : ch.getBlockEntities().values()) {
                    String t = typeFor(be);
                    if (t != null) cache.add(new Entry(new AABB(be.getBlockPos()), t));
                }
            }
        }
    }

    @Override
    public void onExtract() {
        for (Entry e : cache) RenderUtil.box(e.box, Config.get(e.type));
    }
}
