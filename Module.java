package dev.tuff.module;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public abstract class Module {
    public final String name;
    private boolean enabled;
    protected final Minecraft mc = Minecraft.getInstance();

    protected Module(String name) { this.name = name; }

    public boolean isEnabled() { return enabled; }

    public void toggle() {
        enabled = !enabled;
        if (enabled) onEnable(); else onDisable();
    }

    /** For a module that fails to start and needs to turn itself back off. */
    protected final void forceDisable() { enabled = false; }

    /** Return a settings screen, or null if this module has none. */
    public Screen createSettingsScreen(Screen parent) { return null; }

    protected void onEnable() {}
    protected void onDisable() {}
    /** Client tick. */
    public void onTick(Minecraft mc) {}
    /** Render EXTRACTION phase: add lines via RenderUtil. World coordinates. */
    public void onExtract() {}
}
