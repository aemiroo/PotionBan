package com.oorimea.potionban;


import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Set;

public class PotionBanPlugin extends JavaPlugin {

    private Set<PotionEffectType> bannedEffects;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();

        getServer().getPluginManager().registerEvents(new PotionListener(this), this);

        getLogger().info("PotionBan enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("PotionBan disabled.");
    }

    public void reloadConfig() {
        super.reloadConfig();
        bannedEffects = new HashSet<>();
        for (String effectName : getConfig().getStringList("banned-effects")) {
            try {
                bannedEffects.add(PotionEffectType.getByName(effectName));
            } catch (IllegalArgumentException e) {
                getLogger().warning("Invalid potion effect: " + effectName);
            }
        }
    }

    public Set<PotionEffectType> getBannedEffects() {
        return bannedEffects;
    }
}