package dev.tuff.module;

import dev.tuff.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;

import java.util.*;

/**
 * Flags chunks containing a suspicious amount of player-made blocks underground (planks, glass,
 * torches, hoppers, furnaces, beds, doors, wool, concrete, ...). Scans each loaded chunk once and
 * remembers results until you leave the world.
 *
 * Heuristic only: mineshafts, dungeons, strongholds and villages will false-positive.
 */
public class BaseFinder extends Module {
    public static int threshold = 15;     // min score to highlight
    public static int maxScanY = 50;      // only look below this Y ("underground")
    public static int chunksPerTick = 1;  // scan budget
    public static int drawRadiusChunks = 48;

    private record Result(int score, int minY, int maxY) {}

    private final Map<Long, Result> results = new HashMap<>();
    private final Set<Long> queued = new HashSet<>();
    private final ArrayDeque<long[]> queue = new ArrayDeque<>();
    private Object lastLevel;
    private int rescan;

    private static final Map<Block, Integer> WEIGHTS = new IdentityHashMap<>();
    static {
        for (Block b : BuiltInRegistries.BLOCK) {
            String n = BuiltInRegistries.BLOCK.getKey(b).getPath();
            int w = 0;
            if (n.endsWith("_planks")) w = 1;
            else if (n.contains("torch") || n.contains("lantern") || n.contains("campfire")) w = 2;
            else if (n.contains("glass")) w = 2;
            else if (n.endsWith("_wool") || n.endsWith("_concrete")) w = 2;
            else if (n.endsWith("_bed") || n.endsWith("_door") || n.endsWith("_trapdoor") || n.endsWith("_sign")) w = 3;
            else if (n.equals("bricks") || n.equals("ladder") || n.equals("bookshelf")) w = 2;
            else if (n.equals("crafting_table") || n.contains("furnace") || n.equals("smoker")
                    || n.equals("barrel") || n.equals("chest") || n.equals("trapped_chest") || n.equals("ender_chest")
                    || n.endsWith("shulker_box") || n.equals("anvil") || n.equals("enchanting_table")
                    || n.equals("brewing_stand") || n.equals("jukebox") || n.equals("lectern")
                    || n.equals("stonecutter") || n.equals("grindstone") || n.equals("smithing_table")
                    || n.equals("loom") || n.equals("composter") || n.equals("cauldron")) w = 3;
            else if (n.equals("hopper") || n.equals("dropper") || n.equals("dispenser") || n.equals("piston")
                    || n.equals("sticky_piston") || n.equals("observer") || n.equals("repeater")
                    || n.equals("comparator") || n.equals("redstone_lamp") || n.equals("lever")
                    || n.equals("redstone_wire")) w = 2;
            if (w > 0) WEIGHTS.put(b, w);
        }
    }

    private static int weight(BlockState s) { return WEIGHTS.getOrDefault(s.getBlock(), 0); }
    private static long key(int x, int z) { return ((long) x << 32) | (z & 0xFFFFFFFFL); }

    public BaseFinder() { super("Base Finder"); }

    public void reset() { results.clear(); queue.clear(); queued.clear(); lastLevel = null; }

    @Override protected void onEnable() { rescan = 0; }

    @Override
    public void onTick(Minecraft mc) {
        if (lastLevel != mc.level) { reset(); lastLevel = mc.level; }

        if (rescan-- <= 0) { // look for new, unscanned chunks every second
            rescan = 20;
            int pcx = mc.player.blockPosition().getX() >> 4;
            int pcz = mc.player.blockPosition().getZ() >> 4;
            int r = mc.options.renderDistance().get();
            for (int dx = -r; dx <= r; dx++)
                for (int dz = -r; dz <= r; dz++) {
                    int cx = pcx + dx, cz = pcz + dz;
                    long k = key(cx, cz);
                    if (results.containsKey(k) || queued.contains(k)) continue;
                    if (mc.level.getChunkSource().getChunkNow(cx, cz) != null) {
                        queue.add(new long[]{cx, cz});
                        queued.add(k);
                    }
                }
        }

        for (int i = 0; i < chunksPerTick && !queue.isEmpty(); i++) {
            long[] p = queue.poll();
            int cx = (int) p[0], cz = (int) p[1];
            queued.remove(key(cx, cz));
            LevelChunk ch = mc.level.getChunkSource().getChunkNow(cx, cz);
            if (ch != null) results.put(key(cx, cz), scan(ch));
        }
    }

    private Result scan(LevelChunk chunk) {
        int score = 0, minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        LevelChunkSection[] sections = chunk.getSections();
        int minSection = chunk.getMinSectionY();
        for (int i = 0; i < sections.length; i++) {
            int baseY = (minSection + i) << 4;
            if (baseY > maxScanY) break;
            LevelChunkSection s = sections[i];
            if (s == null || s.hasOnlyAir() || !s.maybeHas(st -> weight(st) > 0)) continue; // fast palette skip
            for (int y = 0; y < 16; y++) {
                if (baseY + y > maxScanY) break;
                for (int x = 0; x < 16; x++)
                    for (int z = 0; z < 16; z++) {
                        int w = weight(s.getBlockState(x, y, z));
                        if (w > 0) {
                            score += w;
                            minY = Math.min(minY, baseY + y);
                            maxY = Math.max(maxY, baseY + y);
                        }
                    }
            }
        }
        return new Result(score, minY, maxY);
    }

    @Override
    public void onExtract() {
        if (mc.player == null) return;
        double px = mc.player.getX(), pz = mc.player.getZ();
        for (Map.Entry<Long, Result> e : results.entrySet()) {
            Result r = e.getValue();
            if (r.score < threshold) continue;
            int cx = (int) (e.getKey() >> 32), cz = (int) (long) e.getKey();
            double x0 = cx * 16.0, z0 = cz * 16.0;
            if (Math.abs(x0 + 8 - px) > drawRadiusChunks * 16 || Math.abs(z0 + 8 - pz) > drawRadiusChunks * 16) continue;
            float t = Math.min(1f, (r.score - threshold) / 60f); // yellow -> red as score climbs
            int g = (int) (255 * (1f - t));
            int color = 0xFF000000 | (255 << 16) | (g << 8);
            RenderUtil.box(new AABB(x0, r.minY, z0, x0 + 16, r.maxY + 1, z0 + 16), color);
        }
    }
}
